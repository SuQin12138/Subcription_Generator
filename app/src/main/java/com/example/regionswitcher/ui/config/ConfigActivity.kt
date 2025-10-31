package com.example.regionswitcher.ui.config

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.regionswitcher.R
import com.example.regionswitcher.databinding.ActivityConfigBinding
import com.example.regionswitcher.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class ConfigActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityConfigBinding
    private val viewModel: ConfigViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        observeViewModel()
        setupClickListeners()
        setupInputWatchers()
        
        // 加载配置
        viewModel.loadConfigs()
    }
    
    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.config_title)
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.systemConfig.collect { config ->
                // 更新UI
                setTextIfChanged(binding.etWorkerDomain, config.workerDomain)
                binding.etWorkerRegion.setText(config.manualRegion)
                binding.etCustomProxyIP.setText(config.customProxyIP)
                setTextIfChanged(binding.etPreferredIps, config.preferredIPs)
                setTextIfChanged(binding.etPreferredIpsUrl, config.preferredIPsURL)
                setTextIfChanged(binding.etSocksConfig, config.socks5Config)
                setTextIfChanged(binding.etSubConverterUrl, config.subConverterUrl)
                binding.swRegionMatching.isChecked = config.enableRegionMatching
                binding.swTlsOnly.isChecked = config.disableNonTLS
                binding.swDisablePreferred.isChecked = config.disablePreferred
                binding.swPreferredDomains.isChecked = config.enablePreferredDomains
                binding.swPreferredIps.isChecked = config.enablePreferredIPs
                binding.swGithubIps.isChecked = config.enableGithubIPs
                binding.swApiManagement.isChecked = config.apiManagementEnabled
                binding.swDowngradeMode.isChecked = config.downgradeMode
                updateWorkerRegionState()
            }
        }
        
        lifecycleScope.launch {
            viewModel.protocolConfig.collect { config ->
                binding.swEnableVless.isChecked = config.enableVless
                binding.swEnableTrojan.isChecked = config.enableTrojan
                binding.swEnableXhttp.isChecked = config.enableXhttp
                binding.etTrojanPassword.setText(config.trojanPassword)
                setTextIfChanged(binding.etAuthToken, config.authToken)
                setTextIfChanged(binding.etCustomPath, config.customPath)
            }
        }
        
        lifecycleScope.launch {
            viewModel.message.collect { message ->
                message?.let {
                    Toast.makeText(this@ConfigActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // 显示/隐藏加载指示器
                binding.btnSaveConfig.isEnabled = !isLoading
                binding.btnResetConfig.isEnabled = !isLoading
            }
        }
    }
    
    private fun setupClickListeners() {
        // 保存配置
        binding.btnSaveConfig.setOnClickListener {
            saveConfigs()
        }
        
        // 重置配置
        binding.btnResetConfig.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("重置配置")
                .setMessage("确定要重置所有配置为默认值吗？此操作不可撤销。")
                .setPositiveButton("确定") { _, _ ->
                    viewModel.resetConfigs()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun setupInputWatchers() {
        binding.etCustomProxyIP.doAfterTextChanged {
            updateWorkerRegionState()
        }
    }

    private fun updateWorkerRegionState() {
        val hasCustomIP = binding.etCustomProxyIP.text?.toString()?.trim()?.isNotEmpty() == true
        binding.etWorkerRegion.isEnabled = !hasCustomIP
        binding.etWorkerRegion.alpha = if (hasCustomIP) 0.5f else 1f
    }

    private fun setTextIfChanged(editText: com.google.android.material.textfield.TextInputEditText, value: String) {
        val current = editText.text?.toString() ?: ""
        if (current != value) {
            editText.setText(value)
        }
    }
    
    private fun saveConfigs() {
        val currentSystem = viewModel.systemConfig.value
        val currentProtocol = viewModel.protocolConfig.value
        val workerDomain = binding.etWorkerDomain.text?.toString()?.trim().orEmpty()
        if (workerDomain.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_missing_worker_domain), Toast.LENGTH_SHORT).show()
            return
        }
        
        val rawAuthToken = binding.etAuthToken.text?.toString()?.trim().orEmpty()
        val sanitizedAuthToken = if (rawAuthToken.isNotEmpty()) rawAuthToken.lowercase(Locale.ROOT) else ""
    val rawCustomPath = binding.etCustomPath.text?.toString()?.trim().orEmpty()
    val sanitizedCustomPath = rawCustomPath.trim('/').trim()
        
        if (sanitizedAuthToken.isEmpty() && sanitizedCustomPath.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_missing_auth_or_path), Toast.LENGTH_SHORT).show()
            return
        }
        
        if (sanitizedCustomPath.isEmpty() && sanitizedAuthToken.isNotEmpty() && !NetworkUtils.isValidUUID(sanitizedAuthToken)) {
            Toast.makeText(this, getString(R.string.error_invalid_uuid), Toast.LENGTH_SHORT).show()
            return
        }
        // 收集系统配置
        val systemConfig = currentSystem.copy(
            workerDomain = workerDomain,
            manualRegion = binding.etWorkerRegion.text?.toString()?.trim()?.uppercase(Locale.ROOT).orEmpty(),
            customProxyIP = binding.etCustomProxyIP.text?.toString()?.trim().orEmpty(),
            preferredIPs = binding.etPreferredIps.text?.toString()?.trim().orEmpty(),
            preferredIPsURL = binding.etPreferredIpsUrl.text?.toString()?.trim().orEmpty(),
            socks5Config = binding.etSocksConfig.text?.toString()?.trim().orEmpty(),
            subConverterUrl = binding.etSubConverterUrl.text?.toString()?.trim().orEmpty(),
            enableRegionMatching = binding.swRegionMatching.isChecked,
            disableNonTLS = binding.swTlsOnly.isChecked,
            disablePreferred = binding.swDisablePreferred.isChecked,
            enablePreferredDomains = binding.swPreferredDomains.isChecked,
            enablePreferredIPs = binding.swPreferredIps.isChecked,
            enableGithubIPs = binding.swGithubIps.isChecked,
            apiManagementEnabled = binding.swApiManagement.isChecked,
            downgradeMode = binding.swDowngradeMode.isChecked
        )
        
        // 收集协议配置
        val protocolConfig = currentProtocol.copy(
            enableVless = binding.swEnableVless.isChecked,
            enableTrojan = binding.swEnableTrojan.isChecked,
            enableXhttp = binding.swEnableXhttp.isChecked,
            trojanPassword = binding.etTrojanPassword.text?.toString()?.trim().orEmpty(),
            customPath = sanitizedCustomPath,
            authToken = sanitizedAuthToken
        )
        
        // 验证配置
        if (!binding.swEnableVless.isChecked && 
            !binding.swEnableTrojan.isChecked && 
            !binding.swEnableXhttp.isChecked) {
            Toast.makeText(this, "至少需要启用一个协议", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 保存配置
        viewModel.saveConfigs(systemConfig, protocolConfig)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

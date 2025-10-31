package com.example.regionswitcher.ui.generator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.regionswitcher.R
import com.example.regionswitcher.data.model.Region
import com.example.regionswitcher.databinding.ActivitySubscriptionGeneratorBinding
import com.example.regionswitcher.ui.adapter.SubscriptionLinksAdapter
import com.example.regionswitcher.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class SubscriptionGeneratorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySubscriptionGeneratorBinding
    private val viewModel: SubscriptionGeneratorViewModel by viewModels()
    private lateinit var adapter: SubscriptionLinksAdapter
    private lateinit var regionAdapter: ArrayAdapter<String>
    private var regionOptions: List<RegionOption> = emptyList()
    private var selectedRegionCode: String? = null
    private var pendingRegionCode: String? = null
    private var generatorLoading = false
    private var configLoading = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRegionDropdown()
        setupObservers()
        setupInputWatchers()
        setupClickListeners()

        viewModel.generateDefaultSubscription()
    }
    
    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.generator_title)

        adapter = SubscriptionLinksAdapter { link ->
            copyToClipboard(link, "订阅链接已复制")
        }

        binding.recyclerViewLinks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLinks.adapter = adapter

        updateTrojanPasswordVisibility(binding.swEnableTrojan.isChecked)
    }

    private fun setupRegionDropdown() {
        regionAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.etWorkerRegion.setAdapter(regionAdapter)
        binding.etWorkerRegion.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !binding.etWorkerRegion.isPopupShowing) {
                binding.etWorkerRegion.showDropDown()
            }
        }
        binding.etWorkerRegion.setOnClickListener {
            if (!binding.etWorkerRegion.isPopupShowing) {
                binding.etWorkerRegion.showDropDown()
            }
        }
        binding.etWorkerRegion.setOnItemClickListener { _, _, position, _ ->
            regionOptions.getOrNull(position)?.let { option ->
                selectedRegionCode = option.code
                pendingRegionCode = option.code
            }
        }

        lifecycleScope.launch {
            viewModel.availableRegions.collect { regions ->
                updateRegionOptions(regions)
            }
        }
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.subscriptionLinks.collect { links ->
                adapter.updateLinks(links)
                binding.tvLinkCount.text = "共 ${links.size} 个节点"
            }
        }
        
        lifecycleScope.launch {
            viewModel.base64Subscription.collect { base64 ->
                binding.tvBase64Content.text = base64
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                generatorLoading = isLoading
                binding.btnGenerate.isEnabled = !isLoading
                updateLoadingState()
            }
        }
        
        lifecycleScope.launch {
            viewModel.systemConfig.collect { config ->
                setTextIfDifferent(binding.etWorkerDomain, config.workerDomain)
                setTextIfDifferent(binding.etCustomProxyIP, config.customProxyIP)
                setTextIfDifferent(binding.etPreferredIps, config.preferredIPs)
                setTextIfDifferent(binding.etPreferredIpsUrl, config.preferredIPsURL)
                setTextIfDifferent(binding.etSocksConfig, config.socks5Config)
                setTextIfDifferent(binding.etSubConverterUrl, config.subConverterUrl)
                binding.swRegionMatching.isChecked = config.enableRegionMatching
                binding.swTlsOnly.isChecked = config.disableNonTLS
                binding.swDisablePreferred.isChecked = config.disablePreferred
                binding.swPreferredDomains.isChecked = config.enablePreferredDomains
                binding.swPreferredIps.isChecked = config.enablePreferredIPs
                binding.swGithubIps.isChecked = config.enableGithubIPs
                binding.swApiManagement.isChecked = config.apiManagementEnabled
                binding.swDowngradeMode.isChecked = config.downgradeMode
                binding.tvLastWorkerDomain.text = formatLastValue(config.workerDomain)
                updateRegionSelection(config.manualRegion)
                updateWorkerRegionState()
            }
        }

        lifecycleScope.launch {
            viewModel.protocolConfig.collect { config ->
                if (binding.swEnableVless.isChecked != config.enableVless) {
                    binding.swEnableVless.isChecked = config.enableVless
                }
                if (binding.swEnableTrojan.isChecked != config.enableTrojan) {
                    binding.swEnableTrojan.isChecked = config.enableTrojan
                }
                binding.swEnableXhttp.isChecked = config.enableXhttp
                setTextIfDifferent(binding.etTrojanPassword, config.trojanPassword)
                setTextIfDifferent(binding.etAuthToken, config.authToken)
                setTextIfDifferent(binding.etCustomPath, config.customPath)
                binding.tvLastUuid.text = formatLastValue(config.authToken)
                updateTrojanPasswordVisibility(config.enableTrojan)
            }
        }

        lifecycleScope.launch {
            viewModel.configLoading.collect { loading ->
                configLoading = loading
                binding.btnRefreshConfig.isEnabled = !loading
                binding.btnSaveConfig.isEnabled = !loading
                binding.btnResetConfig.isEnabled = !loading
                updateLoadingState()
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(this@SubscriptionGeneratorActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.message.collect { message ->
                message?.let {
                    Toast.makeText(this@SubscriptionGeneratorActivity, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearMessage()
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        // 生成订阅按钮
        binding.btnGenerate.setOnClickListener {
            buildGeneratorConfig()?.let { config ->
                viewModel.generateSubscription(config)
            }
        }
        
        // 复制Base64按钮
        binding.btnCopyBase64.setOnClickListener {
            val base64 = binding.tvBase64Content.text.toString()
            if (base64.isNotEmpty()) {
                copyToClipboard(base64, "Base64订阅内容已复制")
            }
        }
        
        // 客户端按钮们
        binding.btnClash.setOnClickListener {
            generateClientSubscription("clash")
        }
        
        binding.btnSurge.setOnClickListener {
            generateClientSubscription("surge")
        }
        
        binding.btnSingBox.setOnClickListener {
            generateClientSubscription("singbox")
        }
        
        binding.btnQuantumultX.setOnClickListener {
            generateClientSubscription("quanx")
        }
        
        binding.btnV2ray.setOnClickListener {
            generateClientSubscription("v2ray")
        }
        
        binding.btnLoon.setOnClickListener {
            generateClientSubscription("loon")
        }
        
        // 协议开关监听
        binding.swEnableVless.setOnCheckedChangeListener { _, _ ->
            checkProtocolSelection()
        }
        
        binding.swEnableTrojan.setOnCheckedChangeListener { _, isChecked ->
            checkProtocolSelection()
            updateTrojanPasswordVisibility(isChecked)
        }

        // 配置按钮
        binding.btnRefreshConfig.setOnClickListener {
            viewModel.refreshConfigFromRemote()
        }

        binding.btnSaveConfig.setOnClickListener {
            saveConfigs()
        }

        binding.btnResetConfig.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("重置配置")
                .setMessage("确定要重置所有配置为默认值吗？此操作不可撤销。")
                .setPositiveButton("确定") { _, _ ->
                    viewModel.resetConfigs()
                }
                .setNegativeButton("取消", null)
                .show()
        }
        
        // 添加自定义IP按钮
        binding.btnAddCustomIP.setOnClickListener {
            val ip = binding.etCustomIP.text.toString().trim()
            val port = binding.etCustomPort.text.toString().trim()
            val name = binding.etCustomName.text.toString().trim()
            
            if (ip.isNotEmpty() && port.isNotEmpty() && name.isNotEmpty()) {
                viewModel.addCustomIP(ip, port.toIntOrNull() ?: 443, name)
                
                // 清空输入框
                binding.etCustomIP.text?.clear()
                binding.etCustomPort.text?.clear()
                binding.etCustomName.text?.clear()
                
                Toast.makeText(this, "已添加自定义IP: $name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "请填写完整的IP信息", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 清除自定义IP按钮
        binding.btnClearCustomIPs.setOnClickListener {
            viewModel.clearCustomIPs()
            Toast.makeText(this, "已清除所有自定义IP", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupInputWatchers() {
        binding.etCustomProxyIP.doAfterTextChanged {
            updateWorkerRegionState()
        }
    }
    
    private fun checkProtocolSelection() {
        if (!binding.swEnableVless.isChecked && !binding.swEnableTrojan.isChecked) {
            binding.swEnableVless.isChecked = true
            Toast.makeText(this, "至少需要启用一个协议", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun generateClientSubscription(clientType: String) {
        val workerDomain = binding.etWorkerDomain.text.toString().trim()
        val customPath = binding.etCustomPath.text.toString().trim()
        val authToken = binding.etAuthToken.text.toString().trim()
        
        if (workerDomain.isEmpty()) {
            Toast.makeText(this, "请先填写Worker域名", Toast.LENGTH_SHORT).show()
            return
        }

        val identifier = when {
            customPath.isNotEmpty() -> customPath.trim('/').trim()
            authToken.isNotEmpty() -> authToken.lowercase(Locale.ROOT)
            else -> {
                Toast.makeText(this, "请填写订阅路径或令牌", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val baseUrl = if (workerDomain.startsWith("http", ignoreCase = true)) {
            workerDomain
        } else {
            "https://$workerDomain"
        }
        val normalizedBase = baseUrl.trimEnd('/')
        val subPath = "/$identifier"
        val subscriptionUrl = "$normalizedBase$subPath/sub"
        
        val clientUrl = when (clientType) {
            "v2ray" -> subscriptionUrl
            else -> {
                val converterUrl = "https://url.v1.mk/sub"
                val encodedUrl = java.net.URLEncoder.encode(subscriptionUrl, "UTF-8")
                "$converterUrl?target=$clientType&url=$encodedUrl&insert=false"
            }
        }
        
        copyToClipboard(clientUrl, "${clientType.uppercase()} 订阅链接已复制")
    }

    private fun buildGeneratorConfig(): SubscriptionGeneratorViewModel.GeneratorConfig? {
        val uuid = binding.etAuthToken.text.toString().trim().lowercase(Locale.ROOT)
        val workerDomain = binding.etWorkerDomain.text.toString().trim()
        val customPath = binding.etCustomPath.text.toString().trim().trim('/')

        if (workerDomain.isEmpty()) {
            Toast.makeText(this, "请先填写Worker域名", Toast.LENGTH_SHORT).show()
            return null
        }

        if (uuid.isEmpty()) {
            Toast.makeText(this, "请填写认证令牌(UUID)", Toast.LENGTH_SHORT).show()
            return null
        }

        return SubscriptionGeneratorViewModel.GeneratorConfig(
            uuid = uuid,
            workerDomain = workerDomain,
            enableVless = binding.swEnableVless.isChecked,
            enableTrojan = binding.swEnableTrojan.isChecked,
            trojanPassword = binding.etTrojanPassword.text.toString().trim().ifEmpty { null },
            disableNonTLS = binding.swTlsOnly.isChecked,
            customPath = customPath.ifEmpty { null }
        )
    }

    private fun setTextIfDifferent(
        editText: com.google.android.material.textfield.TextInputEditText,
        value: String
    ) {
        val current = editText.text?.toString() ?: ""
        if (current != value) {
            editText.setText(value)
        }
    }
    
    private fun copyToClipboard(text: String, message: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("订阅链接", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateRegionOptions(regions: List<Region>) {
        regionOptions = regions
            .sortedWith(compareBy<Region> { it.isCustom }.thenBy { it.priority }.thenBy { it.code })
            .map { region ->
                RegionOption(
                    code = region.code.uppercase(Locale.ROOT),
                    displayText = resolveRegionLabel(region)
                )
            }

        regionAdapter.clear()
        regionAdapter.addAll(regionOptions.map { it.displayText })
        regionAdapter.notifyDataSetChanged()

        updateRegionSelection(pendingRegionCode)
    }

    private fun resolveRegionLabel(region: Region): String {
        val code = region.code.uppercase(Locale.ROOT)
        val fallback = region.nameZh.takeIf { it.isNotBlank() }
            ?: region.nameEn.takeIf { it.isNotBlank() }
            ?: code
        return resolveRegionLabel(code, fallback)
    }

    private fun resolveRegionLabel(code: String, fallback: String? = null): String {
        val normalized = code.trim().uppercase(Locale.ROOT)
        val resName = "region_${normalized.lowercase(Locale.ROOT)}"
        val resId = resources.getIdentifier(resName, "string", packageName)
        return if (resId != 0) getString(resId) else fallback ?: normalized
    }

    private fun updateRegionSelection(regionCode: String?) {
        val normalized = regionCode?.trim()?.uppercase(Locale.ROOT)
        pendingRegionCode = normalized
        selectedRegionCode = normalized

        val option = regionOptions.firstOrNull { it.code == normalized }
        when {
            option != null -> {
                val display = option.displayText
                if (binding.etWorkerRegion.text?.toString() != display) {
                    binding.etWorkerRegion.setText(display, false)
                }
            }
            normalized.isNullOrEmpty() -> {
                if (!binding.etWorkerRegion.text.isNullOrEmpty()) {
                    binding.etWorkerRegion.setText("", false)
                }
            }
            else -> {
                val label = resolveRegionLabel(normalized, normalized)
                if (binding.etWorkerRegion.text?.toString() != label) {
                    binding.etWorkerRegion.setText(label, false)
                }
            }
        }
    }

    private fun updateWorkerRegionState() {
        val hasCustomIP = binding.etCustomProxyIP.text?.toString()?.trim()?.isNotEmpty() == true
        binding.etWorkerRegion.isEnabled = !hasCustomIP
        binding.etWorkerRegion.alpha = if (hasCustomIP) 0.5f else 1f
    }

    private fun formatLastValue(value: String?): String {
        return if (value.isNullOrBlank()) {
            getString(R.string.config_last_value_none)
        } else {
            getString(R.string.config_last_value, value)
        }
    }

    private fun updateLoadingState() {
        binding.progressBar.visibility = if (generatorLoading || configLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateTrojanPasswordVisibility(enabled: Boolean) {
        binding.layoutTrojanPassword.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun saveConfigs() {
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

        val manualRegionText = binding.etWorkerRegion.text?.toString()?.trim().orEmpty()
        val manualRegionCode = resolveManualRegionCode(manualRegionText)

        val currentSystem = viewModel.systemConfig.value
        val currentProtocol = viewModel.protocolConfig.value

        val systemConfig = currentSystem.copy(
            workerDomain = workerDomain,
            manualRegion = manualRegionCode,
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

        val protocolConfig = currentProtocol.copy(
            enableVless = binding.swEnableVless.isChecked,
            enableTrojan = binding.swEnableTrojan.isChecked,
            enableXhttp = binding.swEnableXhttp.isChecked,
            trojanPassword = binding.etTrojanPassword.text?.toString()?.trim().orEmpty(),
            customPath = sanitizedCustomPath,
            authToken = sanitizedAuthToken
        )

        if (!binding.swEnableVless.isChecked && !binding.swEnableTrojan.isChecked && !binding.swEnableXhttp.isChecked) {
            Toast.makeText(this, "至少需要启用一个协议", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveConfigs(systemConfig, protocolConfig)
    }

    private fun resolveManualRegionCode(input: String): String {
        selectedRegionCode?.let { return it }
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        regionOptions.firstOrNull { it.displayText == trimmed }?.let { return it.code }
        val uppercase = trimmed.uppercase(Locale.ROOT)
        return if (uppercase.length == 2 && uppercase.all { it in 'A'..'Z' }) uppercase else ""
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

private data class RegionOption(
    val code: String,
    val displayText: String
)

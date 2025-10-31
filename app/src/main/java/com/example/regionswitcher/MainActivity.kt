package com.example.regionswitcher

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.regionswitcher.data.model.ConnectionStatus
import com.example.regionswitcher.databinding.ActivityMainBinding
import com.example.regionswitcher.ui.config.ConfigActivity
import com.example.regionswitcher.ui.subscription.SubscriptionActivity
import com.example.regionswitcher.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        observeViewModel()
        setupClickListeners()
        
        // 初始化数据
        viewModel.refreshSystemStatus()
    }
    
    private fun setupUI() {
        // 设置Matrix风格的动画效果
        binding.tvAppTitle.alpha = 0f
        binding.tvAppTitle.animate()
            .alpha(1f)
            .setDuration(1000)
            .start()
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            // 观察当前地区
            viewModel.currentRegion.collect { region ->
                region?.let {
                    binding.tvCurrentRegion.text = it.nameZh
                }
            }
        }
        
        lifecycleScope.launch {
            // 观察系统状态
            viewModel.systemStatus.collect { status ->
                updateSystemStatus(status)
            }
        }
        
        lifecycleScope.launch {
            // 观察加载状态
            viewModel.isLoading.collect { isLoading ->
                // 可以添加loading indicator
            }
        }
        
        lifecycleScope.launch {
            // 观察错误信息
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        // 地区选择按钮
        binding.btnRegionUS.setOnClickListener { selectRegion("US") }
        binding.btnRegionSG.setOnClickListener { selectRegion("SG") }
        binding.btnRegionJP.setOnClickListener { selectRegion("JP") }
        binding.btnRegionHK.setOnClickListener { selectRegion("HK") }
        binding.btnRegionKR.setOnClickListener { selectRegion("KR") }
        binding.btnRegionDE.setOnClickListener { selectRegion("DE") }
        binding.btnRegionSE.setOnClickListener { selectRegion("SE") }
        binding.btnRegionNL.setOnClickListener { selectRegion("NL") }
        binding.btnRegionFI.setOnClickListener { selectRegion("FI") }
        binding.btnRegionGB.setOnClickListener { selectRegion("GB") }
        
        // 自动检测按钮
        binding.btnAutoDetect.setOnClickListener {
            viewModel.enableAutoDetection()
            Toast.makeText(this, "已启用自动地区检测", Toast.LENGTH_SHORT).show()
        }
        
        // 刷新按钮
        binding.btnRefreshRegion.setOnClickListener {
            viewModel.refreshSystemStatus()
        }
        
        // 配置按钮
        binding.btnConfig.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
          // 订阅按钮
        binding.btnSubscription.setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }
        
        // 生成器按钮
        binding.btnGenerator.setOnClickListener {
            startActivity(Intent(this, com.example.regionswitcher.ui.generator.SubscriptionGeneratorActivity::class.java))
        }
    }
    
    private fun selectRegion(regionCode: String) {
        viewModel.selectRegion(regionCode)
        Toast.makeText(this, "已选择地区: $regionCode", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateSystemStatus(status: com.example.regionswitcher.data.model.SystemStatus) {
        // 更新连接状态
        when (status.connectionStatus) {
            ConnectionStatus.ONLINE -> {
                binding.statusIndicator.isEnabled = true
                binding.tvConnectionStatus.text = getString(R.string.status_online)
                binding.tvConnectionStatus.setTextColor(getColor(R.color.status_online))
            }
            ConnectionStatus.OFFLINE -> {
                binding.statusIndicator.isEnabled = false
                binding.tvConnectionStatus.text = getString(R.string.status_offline)
                binding.tvConnectionStatus.setTextColor(getColor(R.color.status_offline))
            }
            ConnectionStatus.CONNECTING -> {
                binding.statusIndicator.isEnabled = true
                binding.tvConnectionStatus.text = getString(R.string.status_connecting)
                binding.tvConnectionStatus.setTextColor(getColor(R.color.status_connecting))
            }
            ConnectionStatus.ERROR -> {
                binding.statusIndicator.isEnabled = false
                binding.tvConnectionStatus.text = getString(R.string.status_error)
                binding.tvConnectionStatus.setTextColor(getColor(R.color.status_offline))
            }
        }
        
        // 更新检测方式
        binding.tvDetectionMethod.text = status.detectionMethod
        
        // 更新地区匹配状态
        binding.tvRegionMatching.text = if (status.regionMatching) "启用" else "禁用"
        binding.tvRegionMatching.setTextColor(
            if (status.regionMatching) getColor(R.color.status_online) 
            else getColor(R.color.status_warning)
        )
    }
}

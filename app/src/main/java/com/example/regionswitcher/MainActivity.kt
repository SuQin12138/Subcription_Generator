package com.example.regionswitcher

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.regionswitcher.data.model.ConnectionStatus
import com.example.regionswitcher.databinding.ActivityMainBinding
import com.example.regionswitcher.ui.generator.SubscriptionGeneratorActivity
import com.example.regionswitcher.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var regionAdapter: ArrayAdapter<String>
    private var regionOptions: List<RegionOption> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRegionDropdown()
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

    private fun setupRegionDropdown() {
        regionAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.acRegionSelector.setAdapter(regionAdapter)
        binding.acRegionSelector.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !binding.acRegionSelector.isPopupShowing) {
                binding.acRegionSelector.showDropDown()
            }
        }
        binding.acRegionSelector.setOnClickListener {
            if (!binding.acRegionSelector.isPopupShowing) {
                binding.acRegionSelector.showDropDown()
            }
        }
        binding.acRegionSelector.setOnItemClickListener { _, _, position, _ ->
            regionOptions.getOrNull(position)?.let { option ->
                selectRegion(option.code)
            }
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.availableRegionUi.collect { regions ->
                regionOptions = regions.map { regionUi ->
                    RegionOption(
                        code = regionUi.code,
                        label = regionUi.formatted(includeCode = false)
                    )
                }
                regionAdapter.clear()
                regionAdapter.addAll(regionOptions.map { it.label })
                regionAdapter.notifyDataSetChanged()
            }
        }

        lifecycleScope.launch {
            viewModel.selectedRegionUi.collect { regionUi ->
                val label = regionUi?.formatted(includeCode = false)
                if (label.isNullOrEmpty()) {
                    if (!binding.acRegionSelector.text.isNullOrEmpty()) {
                        binding.acRegionSelector.setText("", false)
                    }
                } else if (binding.acRegionSelector.text?.toString() != label) {
                    binding.acRegionSelector.setText(label, false)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.deviceRegionUi.collect { regionUi ->
                val text = regionUi?.formatted(includeCode = false)
                    ?: getString(R.string.device_region_placeholder)
                binding.tvCurrentRegion.text = text
            }
        }

        lifecycleScope.launch {
            viewModel.systemStatus.collect { status ->
                updateSystemStatus(status)
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect {
                // 可在此处处理加载状态
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        // 自动检测按钮
        binding.btnAutoDetect.setOnClickListener {
            viewModel.enableAutoDetection()
            Toast.makeText(this, "已启用自动地区检测", Toast.LENGTH_SHORT).show()
        }

        // 刷新按钮
        binding.btnRefreshRegion.setOnClickListener {
            viewModel.refreshSystemStatus()
        }

        binding.btnGenerator.setOnClickListener {
            startActivity(Intent(this, SubscriptionGeneratorActivity::class.java))
        }
    }
    
    private fun selectRegion(regionCode: String) {
        viewModel.selectRegion(regionCode)
        Toast.makeText(this, "已选择地区: $regionCode", Toast.LENGTH_SHORT).show()
    }

    private data class RegionOption(
        val code: String,
        val label: String
    )
    
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

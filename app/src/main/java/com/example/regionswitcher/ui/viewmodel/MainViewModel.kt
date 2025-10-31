package com.example.regionswitcher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.regionswitcher.data.model.Region
import com.example.regionswitcher.data.model.SystemStatus
import com.example.regionswitcher.data.model.ConnectionStatus
import com.example.regionswitcher.data.repository.RegionRepository
import com.example.regionswitcher.data.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val regionRepository: RegionRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _systemStatus = MutableStateFlow(
        SystemStatus(
            connectionStatus = ConnectionStatus.OFFLINE,
            detectionMethod = "初始化中...",
            regionMatching = true
        )
    )
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()
    
    // 获取当前激活的地区
    val currentRegion: StateFlow<Region?> = regionRepository.getCurrentRegion()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    // 获取所有地区列表
    val allRegions: StateFlow<List<Region>> = regionRepository.getAllRegions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        // 初始化时检查系统状态
        refreshSystemStatus()
        
        // 监听配置变化
        viewModelScope.launch {
            configRepository.getSystemConfig().collect { config ->
                _systemStatus.value = _systemStatus.value.copy(
                    regionMatching = config.enableRegionMatching
                )
            }
        }
    }
    
    /**
     * 选择地区
     */
    fun selectRegion(regionCode: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                regionRepository.setActiveRegion(regionCode)
                
                // 更新系统状态
                _systemStatus.value = _systemStatus.value.copy(
                    detectionMethod = "手动指定地区: $regionCode",
                    connectionStatus = ConnectionStatus.CONNECTING
                )
                
                // 模拟连接检测
                kotlinx.coroutines.delay(1000)
                _systemStatus.value = _systemStatus.value.copy(
                    connectionStatus = ConnectionStatus.ONLINE
                )
                
            } catch (e: Exception) {
                _errorMessage.value = "切换地区失败: ${e.message}"
                _systemStatus.value = _systemStatus.value.copy(
                    connectionStatus = ConnectionStatus.ERROR
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 启用自动检测
     */
    fun enableAutoDetection() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 清除手动选择的地区
                regionRepository.clearManualRegion()
                
                // 更新检测方式
                _systemStatus.value = _systemStatus.value.copy(
                    detectionMethod = "API自动检测",
                    connectionStatus = ConnectionStatus.CONNECTING
                )
                
                // 模拟自动检测过程
                kotlinx.coroutines.delay(1500)
                
                // 假设检测到香港地区
                regionRepository.setActiveRegion("HK")
                
                _systemStatus.value = _systemStatus.value.copy(
                    connectionStatus = ConnectionStatus.ONLINE
                )
                
            } catch (e: Exception) {
                _errorMessage.value = "自动检测失败: ${e.message}"
                _systemStatus.value = _systemStatus.value.copy(
                    connectionStatus = ConnectionStatus.ERROR
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 刷新系统状态
     */
    fun refreshSystemStatus() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _systemStatus.value = _systemStatus.value.copy(
                    connectionStatus = ConnectionStatus.CONNECTING
                )
                
                // 模拟状态检测
                kotlinx.coroutines.delay(1000)
                
                val config = configRepository.getSystemConfig().first()
                val currentRegion = regionRepository.getCurrentRegion().first()
                
                val detectionMethod = when {
                    config.manualRegion.isNotEmpty() -> "手动指定地区: ${config.manualRegion}"
                    config.customProxyIP.isNotEmpty() -> "自定义ProxyIP模式"
                    else -> "API自动检测"
                }
                
                _systemStatus.value = SystemStatus(
                    connectionStatus = ConnectionStatus.ONLINE,
                    currentIP = "检测中...",
                    detectionMethod = detectionMethod,
                    regionMatching = config.enableRegionMatching,
                    backupIPStatus = "可用",
                    lastUpdateTime = System.currentTimeMillis()
                )
                
            } catch (e: Exception) {
                _errorMessage.value = "状态刷新失败: ${e.message}"
                _systemStatus.value = _systemStatus.value.copy(
                    connectionStatus = ConnectionStatus.ERROR
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

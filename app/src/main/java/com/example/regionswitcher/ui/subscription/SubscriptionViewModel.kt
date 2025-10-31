package com.example.regionswitcher.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.regionswitcher.data.model.ClientType
import com.example.regionswitcher.data.repository.ConfigRepository
import com.example.regionswitcher.data.repository.RegionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val regionRepository: RegionRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    private val _subscriptionUrl = MutableStateFlow("")
    val subscriptionUrl: StateFlow<String> = _subscriptionUrl.asStateFlow()
    
    private val _nodeCount = MutableStateFlow(0)
    val nodeCount: StateFlow<Int> = _nodeCount.asStateFlow()
    
    private val _regionCount = MutableStateFlow(0)
    val regionCount: StateFlow<Int> = _regionCount.asStateFlow()
    
    private val _lastUpdate = MutableStateFlow(System.currentTimeMillis())
    val lastUpdate: StateFlow<Long> = _lastUpdate.asStateFlow()
    
    /**
     * 生成订阅链接
     */
    fun generateSubscription(clientType: ClientType) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 获取配置
                val protocolConfig = configRepository.getProtocolConfig().first()
                val systemConfig = configRepository.getSystemConfig().first()
                val regions = regionRepository.getAllRegions().first()
                
                val baseDomain = systemConfig.workerDomain.trim()
                if (baseDomain.isEmpty()) {
                    _message.value = "请先在配置页面设置 Worker 域名"
                    return@launch
                }

                val authToken = protocolConfig.authToken.trim()
                if (authToken.isEmpty()) {
                    _message.value = "请先设置认证令牌"
                    return@launch
                }

                val customPath = protocolConfig.customPath.trim().ifEmpty { authToken }

                val baseUrl = if (baseDomain.startsWith("http", ignoreCase = true)) {
                    baseDomain
                } else {
                    "https://$baseDomain"
                }.trimEnd('/')
                
                // 构建订阅URL
                val subscriptionPath = "/${customPath.trim('/')}/sub"
                
                val targetParam = when (clientType) {
                    ClientType.CLASH -> "clash"
                    ClientType.SURGE -> "surge"
                    ClientType.SINGBOX -> "singbox"
                    ClientType.LOON -> "loon"
                    ClientType.QUANTUMULT -> "quanx"
                    ClientType.V2RAY -> "v2ray"
                }
                
                val fullUrl = "$baseUrl$subscriptionPath?target=$targetParam"
                
                _subscriptionUrl.value = fullUrl
                
                // 计算统计信息
                calculateStats(regions, protocolConfig)
                
                _lastUpdate.value = System.currentTimeMillis()
                _message.value = "订阅链接生成成功"
                
            } catch (e: Exception) {
                _message.value = "生成订阅链接失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 计算统计信息
     */
    private fun calculateStats(
        regions: List<com.example.regionswitcher.data.model.Region>,
        protocolConfig: com.example.regionswitcher.data.model.ProtocolConfig
    ) {
        // 计算启用的协议数量
        var protocolCount = 0
        if (protocolConfig.enableVless) protocolCount++
        if (protocolConfig.enableTrojan) protocolCount++
        if (protocolConfig.enableXhttp) protocolCount++
        
        // 每个地区大约有2-4个端口配置，每个协议都会生成对应的节点
        val avgNodesPerRegion = 3 * protocolCount
        val totalNodes = regions.size * avgNodesPerRegion
        
        _nodeCount.value = totalNodes
        _regionCount.value = regions.size
    }
    
    /**
     * 刷新订阅
     */
    fun refreshSubscription() {
        // 重新生成当前客户端类型的订阅
        generateSubscription(ClientType.CLASH) // 默认使用Clash
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _message.value = null
    }
}

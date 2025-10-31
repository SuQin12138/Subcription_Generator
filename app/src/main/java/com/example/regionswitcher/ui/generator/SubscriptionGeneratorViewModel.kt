package com.example.regionswitcher.ui.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.regionswitcher.utils.CloudflareWorkerUtils
import com.example.regionswitcher.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionGeneratorViewModel @Inject constructor() : ViewModel() {
    
    private val _subscriptionLinks = MutableStateFlow<List<String>>(emptyList())
    val subscriptionLinks: StateFlow<List<String>> = _subscriptionLinks.asStateFlow()
    
    private val _base64Subscription = MutableStateFlow("")
    val base64Subscription: StateFlow<String> = _base64Subscription.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _customSources = MutableStateFlow<List<CloudflareWorkerUtils.NodeSource>>(emptyList())
    val customSources: StateFlow<List<CloudflareWorkerUtils.NodeSource>> = _customSources.asStateFlow()
    
    data class GeneratorConfig(
        val uuid: String,
        val workerDomain: String,
        val enableVless: Boolean = true,
        val enableTrojan: Boolean = false,
        val trojanPassword: String? = null,
        val disableNonTLS: Boolean = false,
        val customPath: String? = null
    )
    
    fun generateDefaultSubscription() {
        val defaultConfig = GeneratorConfig(
            uuid = "00000000-0000-0000-0000-000000000000",
            workerDomain = "your-worker.workers.dev"
        )
        generateSubscription(defaultConfig)
    }
    
    fun generateSubscription(config: GeneratorConfig) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                // 验证输入
                if (!NetworkUtils.isValidUUID(config.uuid)) {
                    _errorMessage.value = "UUID格式不正确"
                    return@launch
                }
                
                if (config.workerDomain.isEmpty()) {
                    _errorMessage.value = "Worker域名不能为空"
                    return@launch
                }
                
                // 准备节点源列表
                val sources = if (_customSources.value.isNotEmpty()) {
                    // 使用自定义IP
                    _customSources.value
                } else {
                    // 使用默认源
                    CloudflareWorkerUtils.getDefaultNodeSources(config.workerDomain)
                }
                
                // 生成订阅链接
                val links = CloudflareWorkerUtils.generateSubscriptionLinks(
                    uuid = config.uuid,
                    workerDomain = config.workerDomain,
                    enableVless = config.enableVless,
                    enableTrojan = config.enableTrojan,
                    trojanPassword = config.trojanPassword,
                    customSources = sources,
                    disableNonTLS = config.disableNonTLS
                )
                
                _subscriptionLinks.value = links
                
                // 生成Base64编码的订阅内容
                val base64Content = CloudflareWorkerUtils.generateBase64Subscription(links)
                _base64Subscription.value = base64Content
                
            } catch (e: Exception) {
                _errorMessage.value = "生成订阅失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addCustomIP(ip: String, port: Int, name: String) {
        viewModelScope.launch {
            try {
                // 验证IP格式
                if (!NetworkUtils.isValidIP(ip)) {
                    _errorMessage.value = "IP地址格式不正确"
                    return@launch
                }
                
                // 验证端口范围
                if (port !in 1..65535) {
                    _errorMessage.value = "端口范围应在 1-65535 之间"
                    return@launch
                }
                
                val newSource = CloudflareWorkerUtils.NodeSource(
                    ip = ip,
                    port = port,
                    name = name
                )
                
                val currentSources = _customSources.value.toMutableList()
                
                // 检查是否已存在相同IP和端口的配置
                val existingIndex = currentSources.indexOfFirst { 
                    it.ip == ip && it.port == port 
                }
                
                if (existingIndex >= 0) {
                    // 更新现有配置
                    currentSources[existingIndex] = newSource
                } else {
                    // 添加新配置
                    currentSources.add(newSource)
                }
                
                _customSources.value = currentSources
                
            } catch (e: Exception) {
                _errorMessage.value = "添加自定义IP失败: ${e.message}"
            }
        }
    }
    
    fun removeCustomIP(index: Int) {
        viewModelScope.launch {
            try {
                val currentSources = _customSources.value.toMutableList()
                if (index in currentSources.indices) {
                    currentSources.removeAt(index)
                    _customSources.value = currentSources
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除自定义IP失败: ${e.message}"
            }
        }
    }
    
    fun clearCustomIPs() {
        _customSources.value = emptyList()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun getSmartRegionSelection(workerRegion: String): List<String> {
        return CloudflareWorkerUtils.getSmartRegionSelection(workerRegion)
    }
    
    fun getBestProxyIPForRegion(region: String): CloudflareWorkerUtils.ProxyIPInfo? {
        return CloudflareWorkerUtils.getBestBackupIP(region)
    }
    
    fun generateLinksForRegion(config: GeneratorConfig, region: String): List<String> {
        return try {
            // 获取该地区的最佳ProxyIP
            val proxyIP = getBestProxyIPForRegion(region)
            
            val sources = if (proxyIP != null) {
                listOf(
                    CloudflareWorkerUtils.NodeSource(
                        ip = proxyIP.domain,
                        port = proxyIP.port,
                        name = "ProxyIP-${proxyIP.region}"
                    )
                )
            } else {
                CloudflareWorkerUtils.getDefaultNodeSources(config.workerDomain)
            }
            
            CloudflareWorkerUtils.generateSubscriptionLinks(
                uuid = config.uuid,
                workerDomain = config.workerDomain,
                workerRegion = region,
                enableVless = config.enableVless,
                enableTrojan = config.enableTrojan,
                trojanPassword = config.trojanPassword,
                customSources = sources,
                disableNonTLS = config.disableNonTLS
            )
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun generateTrojanPassword(uuid: String): String {
        return CloudflareWorkerUtils.sha224Hash(uuid)
    }
}

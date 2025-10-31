package com.example.regionswitcher.ui.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.regionswitcher.data.model.ProtocolConfig
import com.example.regionswitcher.data.model.Region
import com.example.regionswitcher.data.model.SystemConfig
import com.example.regionswitcher.data.repository.ConfigRepository
import com.example.regionswitcher.data.repository.RegionRepository
import com.example.regionswitcher.utils.CloudflareWorkerUtils
import com.example.regionswitcher.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

@HiltViewModel
class SubscriptionGeneratorViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val regionRepository: RegionRepository
) : ViewModel() {
    
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

    private val _configLoading = MutableStateFlow(false)
    val configLoading: StateFlow<Boolean> = _configLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val systemConfigFlow: StateFlow<SystemConfig> = configRepository.getSystemConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SystemConfig()
        )

    private val protocolConfigFlow: StateFlow<ProtocolConfig> = configRepository.getProtocolConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProtocolConfig()
        )

    data class PrefillData(
        val uuid: String = "",
        val workerDomain: String = "",
        val customPath: String = "",
        val trojanPassword: String = "",
        val enableVless: Boolean = true,
        val enableTrojan: Boolean = false,
        val disableNonTls: Boolean = false
    )

    val prefillData: StateFlow<PrefillData> = combine(systemConfigFlow, protocolConfigFlow) { system, protocol ->
        PrefillData(
            uuid = protocol.authToken,
            workerDomain = system.workerDomain,
            customPath = protocol.customPath,
            trojanPassword = protocol.trojanPassword,
            enableVless = protocol.enableVless,
            enableTrojan = protocol.enableTrojan,
            disableNonTls = system.disableNonTLS
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrefillData()
    )

    val systemConfig: StateFlow<SystemConfig> = systemConfigFlow

    val protocolConfig: StateFlow<ProtocolConfig> = protocolConfigFlow

    val availableRegions: StateFlow<List<Region>> = regionRepository.getAllRegions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
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
        viewModelScope.launch {
            val protocol = protocolConfigFlow.value
            val system = systemConfigFlow.value

            val uuid = protocol.authToken.ifBlank { "00000000-0000-0000-0000-000000000000" }
            val workerDomain = system.workerDomain.ifBlank { "your-worker.workers.dev" }

            val defaultConfig = GeneratorConfig(
                uuid = uuid,
                workerDomain = workerDomain,
                enableVless = protocol.enableVless,
                enableTrojan = protocol.enableTrojan,
                trojanPassword = protocol.trojanPassword.ifBlank { null },
                disableNonTLS = system.disableNonTLS,
                customPath = protocol.customPath.ifBlank { null }
            )

            generateSubscription(defaultConfig)
        }
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

    fun refreshConfigFromRemote() {
        viewModelScope.launch {
            _configLoading.value = true
            val result = configRepository.fetchRemoteConfig()
            if (result.isSuccess) {
                _message.value = "配置已刷新"
            } else {
                val error = result.exceptionOrNull()
                val errorMsg = error?.message?.takeIf { it.isNotBlank() }
                _message.value = errorMsg?.let { "远程加载失败: $it" } ?: "远程加载失败"
            }
            _configLoading.value = false
        }
    }

    fun saveConfigs(systemConfig: SystemConfig, protocolConfig: ProtocolConfig) {
        viewModelScope.launch {
            _configLoading.value = true
            try {
                configRepository.saveSystemConfig(systemConfig)
                configRepository.saveProtocolConfig(protocolConfig)

                val remoteResult = configRepository.updateRemoteConfig(systemConfig, protocolConfig)
                if (remoteResult.isSuccess) {
                    val message = remoteResult.getOrNull()
                    _message.value = message ?: "配置保存成功"

                    val refreshResult = configRepository.fetchRemoteConfig()
                    if (refreshResult.isFailure) {
                        val refreshError = refreshResult.exceptionOrNull()
                        val refreshMsg = refreshError?.message?.takeIf { it.isNotBlank() }
                        _message.value = refreshMsg?.let { "配置保存成功，但刷新失败: $it" }
                            ?: "配置保存成功，但刷新失败"
                    }
                } else {
                    val error = remoteResult.exceptionOrNull()
                    val errorMsg = error?.message?.takeIf { it.isNotBlank() }
                    _message.value = errorMsg?.let { "远程保存失败: $it" } ?: "远程保存失败"
                }

            } finally {
                _configLoading.value = false
            }
        }
    }

    fun resetConfigs() {
        viewModelScope.launch {
            try {
                _configLoading.value = true
                configRepository.resetConfig()
                _message.value = "配置已重置为默认值"
            } catch (e: Exception) {
                _message.value = "配置重置失败: ${e.message}"
            } finally {
                _configLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
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

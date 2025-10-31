package com.example.regionswitcher.ui.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.regionswitcher.data.model.SystemConfig
import com.example.regionswitcher.data.model.ProtocolConfig
import com.example.regionswitcher.data.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    // 系统配置
    val systemConfig: StateFlow<SystemConfig> = configRepository.getSystemConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SystemConfig()
        )
    
    // 协议配置
    val protocolConfig: StateFlow<ProtocolConfig> = configRepository.getProtocolConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProtocolConfig()
        )
    
    /**
     * 加载配置
     */
    fun loadConfigs() {
        viewModelScope.launch {
            _isLoading.value = true
            configRepository.fetchRemoteConfig()
                .onSuccess { _message.value = "配置已刷新" }
                .onFailure { error -> _message.value = "远程加载失败: ${error.message}" }
            _isLoading.value = false
        }
    }
    
    /**
     * 保存配置
     */
    fun saveConfigs(systemConfig: SystemConfig, protocolConfig: ProtocolConfig) {
        viewModelScope.launch {
            _isLoading.value = true

            val currentSystemConfig = this@ConfigViewModel.systemConfig.value
            val currentProtocolConfig = this@ConfigViewModel.protocolConfig.value

            val updatedSystemConfig = currentSystemConfig.copy(
                workerDomain = systemConfig.workerDomain,
                manualRegion = systemConfig.manualRegion,
                customProxyIP = systemConfig.customProxyIP,
                enableRegionMatching = systemConfig.enableRegionMatching,
                disableNonTLS = systemConfig.disableNonTLS,
                disablePreferred = systemConfig.disablePreferred,
                socks5Config = systemConfig.socks5Config,
                preferredIPs = systemConfig.preferredIPs,
                preferredIPsURL = systemConfig.preferredIPsURL,
                subConverterUrl = systemConfig.subConverterUrl,
                enablePreferredDomains = systemConfig.enablePreferredDomains,
                enablePreferredIPs = systemConfig.enablePreferredIPs,
                enableGithubIPs = systemConfig.enableGithubIPs,
                apiManagementEnabled = systemConfig.apiManagementEnabled,
                downgradeMode = systemConfig.downgradeMode
            )

            val updatedProtocolConfig = currentProtocolConfig.copy(
                enableVless = protocolConfig.enableVless,
                enableTrojan = protocolConfig.enableTrojan,
                enableXhttp = protocolConfig.enableXhttp,
                trojanPassword = protocolConfig.trojanPassword,
                customPath = protocolConfig.customPath,
                authToken = protocolConfig.authToken
            )

            configRepository.saveSystemConfig(updatedSystemConfig)
            configRepository.saveProtocolConfig(updatedProtocolConfig)

            configRepository.updateRemoteConfig(updatedSystemConfig, updatedProtocolConfig)
                .onSuccess { message ->
                    _message.value = message ?: "配置保存成功"
                    configRepository.fetchRemoteConfig()
                        .onFailure { refreshError ->
                            _message.value = "配置保存成功，但刷新失败: ${refreshError.message}"
                        }
                }
                .onFailure { error -> _message.value = "远程保存失败: ${error.message}" }

            _isLoading.value = false
        }
    }
    
    /**
     * 重置配置
     */
    fun resetConfigs() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                configRepository.resetConfig()
                _message.value = "配置已重置为默认值"
            } catch (e: Exception) {
                _message.value = "配置重置失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _message.value = null
    }
}

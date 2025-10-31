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
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val regionRepository: RegionRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {
    
    data class RegionUiModel(
        val code: String,
        val displayName: String,
        val flag: String
    ) {
        fun formatted(includeCode: Boolean = true): String {
            if (!includeCode) {
                return buildString {
                    if (flag.isNotBlank()) {
                        append(flag)
                        if (displayName.isNotBlank()) {
                            append(' ')
                            append(displayName)
                        }
                    } else {
                        append(displayName.ifBlank { code })
                    }
                }.ifBlank { code }
            }

            val builder = StringBuilder()
            if (flag.isNotBlank()) {
                builder.append(flag)
                builder.append(' ')
            }
            builder.append(code)
            if (displayName.isNotBlank() && !displayName.equals(code, ignoreCase = true)) {
                builder.append(" · ")
                builder.append(displayName)
            }
            return builder.toString()
        }
    }

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
    
    val availableRegionUi: StateFlow<List<RegionUiModel>> = regionRepository.getAllRegions()
        .map { regions ->
            regions
                .sortedWith(compareBy<Region> { it.isCustom }.thenBy { it.priority }.thenBy { it.code })
                .map { it.toUiModel() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedRegionUi: StateFlow<RegionUiModel?> = combine(
        regionRepository.getCurrentRegion(),
        configRepository.getSystemConfig()
    ) { region, config ->
        when {
            region != null -> region.toUiModel()
            config.manualRegion.isNotBlank() -> buildUiModelFromCode(config.manualRegion)
            else -> null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _deviceRegionUi = MutableStateFlow<RegionUiModel?>(null)
    val deviceRegionUi: StateFlow<RegionUiModel?> = _deviceRegionUi.asStateFlow()
    
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

    updateDeviceRegion()
    }
    
    /**
     * 选择地区
     */
    fun selectRegion(regionCode: String) {
        viewModelScope.launch {
            val normalized = regionCode.trim().uppercase(Locale.ROOT)
            try {
                _isLoading.value = true
                val detectionLabel = regionDisplayString(normalized)
                _systemStatus.value = _systemStatus.value.copy(
                    detectionMethod = "手动指定地区: $detectionLabel",
                    connectionStatus = ConnectionStatus.CONNECTING
                )

                regionRepository.setActiveRegion(normalized)
                val message = persistManualRegionSelection(normalized)

                _systemStatus.value = _systemStatus.value.copy(
                    connectionStatus = ConnectionStatus.ONLINE
                )

                if (message?.isNotBlank() == true) {
                    _errorMessage.value = message
                } else {
                    _errorMessage.value = null
                }

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
                val autoMessage = persistAutoDetectionPreferences()
                if (autoMessage?.isNotBlank() == true) {
                    _errorMessage.value = autoMessage
                } else {
                    _errorMessage.value = null
                }

                // 更新检测方式
                _systemStatus.value = _systemStatus.value.copy(
                    detectionMethod = "API自动检测",
                    connectionStatus = ConnectionStatus.CONNECTING
                )
                
                // 模拟自动检测过程
                kotlinx.coroutines.delay(1500)
                
                // 假设检测到香港地区
                val detectedCode = "HK"
                regionRepository.setActiveRegion(detectedCode)
                val detectionLabel = regionDisplayString(detectedCode)
                _systemStatus.value = _systemStatus.value.copy(
                    detectionMethod = "API自动检测: $detectionLabel",
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
                
                val detectionMethod = when {
                    config.manualRegion.isNotEmpty() -> "手动指定地区: ${regionDisplayString(config.manualRegion)}"
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

    private fun Region.toUiModel(): RegionUiModel {
        val normalizedCode = code.trim().uppercase(Locale.ROOT)
        val nameSansFlag = nameZh.replace(flag, "").trim()
        val displayName = when {
            nameSansFlag.isNotEmpty() -> nameSansFlag
            nameEn.isNotEmpty() -> nameEn
            else -> normalizedCode
        }
        val flagEmoji = if (flag.isNotBlank()) flag else codeToFlagEmoji(normalizedCode)
        return RegionUiModel(
            code = normalizedCode,
            displayName = displayName,
            flag = flagEmoji
        )
    }

    private fun buildUiModelFromCode(code: String): RegionUiModel? {
        val normalized = code.trim().uppercase(Locale.ROOT)
        if (normalized.isEmpty()) return null
        val locale = Locale("", normalized)
        val zhName = locale.getDisplayCountry(Locale.SIMPLIFIED_CHINESE)
        val enName = locale.getDisplayCountry(Locale.ENGLISH)
        val displayName = when {
            zhName.isNotBlank() -> zhName
            enName.isNotBlank() -> enName
            else -> normalized
        }
        return RegionUiModel(
            code = normalized,
            displayName = displayName,
            flag = codeToFlagEmoji(normalized)
        )
    }

    private fun codeToFlagEmoji(code: String): String {
        val normalized = code.trim().uppercase(Locale.ROOT)
        if (normalized.length != 2) return ""
        val first = normalized[0]
        val second = normalized[1]
        if (first !in 'A'..'Z' || second !in 'A'..'Z') return ""
        val firstCodePoint = 0x1F1E6 + (first.code - 'A'.code)
        val secondCodePoint = 0x1F1E6 + (second.code - 'A'.code)
        return String(Character.toChars(firstCodePoint)) + String(Character.toChars(secondCodePoint))
    }

    private fun updateDeviceRegion() {
        viewModelScope.launch {
            val localeCode = Locale.getDefault().country?.trim()?.uppercase(Locale.ROOT)
            val region = localeCode?.let { regionRepository.getRegionByCode(it) }
            _deviceRegionUi.value = when {
                region != null -> region.toUiModel()
                localeCode != null -> buildUiModelFromCode(localeCode)
                else -> null
            }
        }
    }

    private suspend fun regionDisplayString(code: String): String {
        val model = regionRepository.getRegionByCode(code)?.toUiModel()
            ?: buildUiModelFromCode(code)
        return model?.formatted() ?: code.trim().uppercase(Locale.ROOT)
    }

    private suspend fun persistManualRegionSelection(normalizedRegion: String): String? {
        val systemConfig = configRepository.getSystemConfig().first()
        val protocolConfig = configRepository.getProtocolConfig().first()
        val updatedSystemConfig = systemConfig.copy(
            currentRegion = normalizedRegion,
            manualRegion = normalizedRegion
        )
        return configRepository.updateRemoteConfig(updatedSystemConfig, protocolConfig).getOrThrow()
    }

    private suspend fun persistAutoDetectionPreferences(): String? {
        val systemConfig = configRepository.getSystemConfig().first()
        val protocolConfig = configRepository.getProtocolConfig().first()
        val updatedSystemConfig = systemConfig.copy(
            manualRegion = ""
        )
        return configRepository.updateRemoteConfig(updatedSystemConfig, protocolConfig).getOrThrow()
    }
}

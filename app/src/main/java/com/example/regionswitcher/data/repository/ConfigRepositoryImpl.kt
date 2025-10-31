package com.example.regionswitcher.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.regionswitcher.data.api.WorkerApiService
import com.example.regionswitcher.data.api.model.WorkerConfigPayload
import com.example.regionswitcher.data.api.model.WorkerUpdateResponse
import com.example.regionswitcher.data.model.ProtocolConfig
import com.example.regionswitcher.data.model.SystemConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

// 创建DataStore实例
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workerApiService: WorkerApiService
) : ConfigRepository {
    
    companion object {
        // 系统配置键
        val CURRENT_REGION = stringPreferencesKey("current_region")
        val MANUAL_REGION = stringPreferencesKey("manual_region")
        val CUSTOM_PROXY_IP = stringPreferencesKey("custom_proxy_ip")
        val ENABLE_REGION_MATCHING = booleanPreferencesKey("enable_region_matching")
        val DISABLE_NON_TLS = booleanPreferencesKey("disable_non_tls")
        val DISABLE_PREFERRED = booleanPreferencesKey("disable_preferred")
        val SOCKS5_CONFIG = stringPreferencesKey("socks5_config")
        val PREFERRED_IPS = stringPreferencesKey("preferred_ips")
        val PREFERRED_IPS_URL = stringPreferencesKey("preferred_ips_url")
        val SUB_CONVERTER_URL = stringPreferencesKey("sub_converter_url")
        val ENABLE_PREFERRED_DOMAINS = booleanPreferencesKey("enable_preferred_domains")
        val ENABLE_PREFERRED_IPS = booleanPreferencesKey("enable_preferred_ips")
        val ENABLE_GITHUB_IPS = booleanPreferencesKey("enable_github_ips")
        val WORKER_DOMAIN = stringPreferencesKey("worker_domain")
        val API_MANAGEMENT_ENABLED = booleanPreferencesKey("api_management_enabled")
        val DOWNGRADE_MODE = booleanPreferencesKey("downgrade_mode")
        
        // 协议配置键
        val ENABLE_VLESS = booleanPreferencesKey("enable_vless")
        val ENABLE_TROJAN = booleanPreferencesKey("enable_trojan")
        val ENABLE_XHTTP = booleanPreferencesKey("enable_xhttp")
        val TROJAN_PASSWORD = stringPreferencesKey("trojan_password")
        val CUSTOM_PATH = stringPreferencesKey("custom_path")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
    }
    
    /**
     * 获取系统配置
     */
    override fun getSystemConfig(): Flow<SystemConfig> {
        return context.dataStore.data.map { preferences ->
            SystemConfig(
                currentRegion = preferences[CURRENT_REGION] ?: "",
                manualRegion = preferences[MANUAL_REGION] ?: "",
                customProxyIP = preferences[CUSTOM_PROXY_IP] ?: "",
                enableRegionMatching = preferences[ENABLE_REGION_MATCHING] ?: true,
                disableNonTLS = preferences[DISABLE_NON_TLS] ?: false,
                disablePreferred = preferences[DISABLE_PREFERRED] ?: false,
                socks5Config = preferences[SOCKS5_CONFIG] ?: "",
                preferredIPs = preferences[PREFERRED_IPS] ?: "",
                preferredIPsURL = preferences[PREFERRED_IPS_URL] ?: "https://raw.githubusercontent.com/qwer-search/bestip/refs/heads/main/kejilandbestip.txt",
                subConverterUrl = preferences[SUB_CONVERTER_URL] ?: "https://url.v1.mk/sub",
                enablePreferredDomains = preferences[ENABLE_PREFERRED_DOMAINS] ?: true,
                enablePreferredIPs = preferences[ENABLE_PREFERRED_IPS] ?: true,
                enableGithubIPs = preferences[ENABLE_GITHUB_IPS] ?: true,
                workerDomain = preferences[WORKER_DOMAIN] ?: "",
                apiManagementEnabled = preferences[API_MANAGEMENT_ENABLED] ?: false,
                downgradeMode = preferences[DOWNGRADE_MODE] ?: false
            )
        }
    }
    
    /**
     * 获取协议配置
     */
    override fun getProtocolConfig(): Flow<ProtocolConfig> {
        return context.dataStore.data.map { preferences ->
            ProtocolConfig(
                enableVless = preferences[ENABLE_VLESS] ?: true,
                enableTrojan = preferences[ENABLE_TROJAN] ?: false,
                enableXhttp = preferences[ENABLE_XHTTP] ?: false,
                trojanPassword = preferences[TROJAN_PASSWORD] ?: "",
                customPath = preferences[CUSTOM_PATH] ?: "",
                authToken = preferences[AUTH_TOKEN] ?: ""
            )
        }
    }
    
    /**
     * 保存系统配置
     */
    override suspend fun saveSystemConfig(config: SystemConfig) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_REGION] = config.currentRegion
            preferences[MANUAL_REGION] = config.manualRegion
            preferences[CUSTOM_PROXY_IP] = config.customProxyIP
            preferences[ENABLE_REGION_MATCHING] = config.enableRegionMatching
            preferences[DISABLE_NON_TLS] = config.disableNonTLS
            preferences[DISABLE_PREFERRED] = config.disablePreferred
            preferences[SOCKS5_CONFIG] = config.socks5Config
            preferences[PREFERRED_IPS] = config.preferredIPs
            preferences[PREFERRED_IPS_URL] = config.preferredIPsURL
            preferences[SUB_CONVERTER_URL] = config.subConverterUrl
            preferences[ENABLE_PREFERRED_DOMAINS] = config.enablePreferredDomains
            preferences[ENABLE_PREFERRED_IPS] = config.enablePreferredIPs
            preferences[ENABLE_GITHUB_IPS] = config.enableGithubIPs
            preferences[WORKER_DOMAIN] = config.workerDomain
            preferences[API_MANAGEMENT_ENABLED] = config.apiManagementEnabled
            preferences[DOWNGRADE_MODE] = config.downgradeMode
        }
    }
    
    /**
     * 保存协议配置
     */
    override suspend fun saveProtocolConfig(config: ProtocolConfig) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_VLESS] = config.enableVless
            preferences[ENABLE_TROJAN] = config.enableTrojan
            preferences[ENABLE_XHTTP] = config.enableXhttp
            preferences[TROJAN_PASSWORD] = config.trojanPassword
            preferences[CUSTOM_PATH] = config.customPath
            preferences[AUTH_TOKEN] = config.authToken
        }
    }
    
    /**
     * 重置所有配置为默认值
     */
    override suspend fun resetConfig() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun fetchRemoteConfig(): Result<Unit> = runCatching {
        val systemConfig = getSystemConfig().first()
        val protocolConfig = getProtocolConfig().first()
        ensureRemoteConfigReady(systemConfig, protocolConfig)

        val targetUrl = buildConfigUrl(systemConfig, protocolConfig)
        val response = workerApiService.fetchConfig(targetUrl)
        val payload = response.requireBody()

        persistRemoteConfig(systemConfig, protocolConfig, payload)
    }

    override suspend fun updateRemoteConfig(systemConfig: SystemConfig, protocolConfig: ProtocolConfig): Result<String?> = runCatching {
        ensureRemoteConfigReady(systemConfig, protocolConfig)

        val payload = WorkerConfigPayload.fromConfigs(systemConfig, protocolConfig)
        val targetUrl = buildConfigUrl(systemConfig, protocolConfig)
        val response = workerApiService.updateConfig(targetUrl, payload)
        val updateResponse = response.requireBody()

        if (!updateResponse.success) {
            throw IllegalStateException(updateResponse.message ?: "配置保存失败")
        }

        val latestPayload = updateResponse.config ?: payload
        persistRemoteConfig(systemConfig, protocolConfig, latestPayload)

        updateResponse.message
    }

    private suspend fun persistRemoteConfig(
        localSystemConfig: SystemConfig,
        localProtocolConfig: ProtocolConfig,
        payload: WorkerConfigPayload
    ) {
        val updatedSystem = localSystemConfig.copy(
            manualRegion = payload.wk?.takeUnless { it.isBlank() } ?: localSystemConfig.manualRegion,
            customProxyIP = payload.p?.takeUnless { it.isBlank() } ?: localSystemConfig.customProxyIP,
            preferredIPs = payload.yx?.takeUnless { it.isBlank() } ?: localSystemConfig.preferredIPs,
            preferredIPsURL = payload.yxURL?.takeUnless { it.isBlank() } ?: localSystemConfig.preferredIPsURL,
            socks5Config = payload.s?.takeUnless { it.isBlank() } ?: localSystemConfig.socks5Config,
            subConverterUrl = payload.subConverterUrl?.takeUnless { it.isBlank() } ?: localSystemConfig.subConverterUrl,
            enablePreferredDomains = payload.enablePreferredDomains.asYesDefaultTrue(localSystemConfig.enablePreferredDomains),
            enablePreferredIPs = payload.enablePreferredIPs.asYesDefaultTrue(localSystemConfig.enablePreferredIPs),
            enableGithubIPs = payload.enableGithubIPs.asYesDefaultTrue(localSystemConfig.enableGithubIPs),
            enableRegionMatching = payload.rm?.let { !it.equals("no", ignoreCase = true) }
                ?: localSystemConfig.enableRegionMatching,
            disableNonTLS = payload.dkby.asYesDefaultFalse(localSystemConfig.disableNonTLS),
            disablePreferred = payload.yxby.asYesDefaultFalse(localSystemConfig.disablePreferred),
            apiManagementEnabled = payload.apiEnabled?.equals("yes", ignoreCase = true)
                ?: localSystemConfig.apiManagementEnabled,
            downgradeMode = payload.qj?.equals("no", ignoreCase = true)
                ?: localSystemConfig.downgradeMode
        )
        saveSystemConfig(updatedSystem)

        val updatedProtocol = localProtocolConfig.copy(
            enableVless = payload.enableVless.asYesDefaultTrue(localProtocolConfig.enableVless),
            enableTrojan = payload.enableTrojan?.equals("yes", ignoreCase = true)
                ?: localProtocolConfig.enableTrojan,
            enableXhttp = payload.enableXhttp?.equals("yes", ignoreCase = true)
                ?: localProtocolConfig.enableXhttp,
            trojanPassword = payload.trojanPassword?.takeUnless { it.isBlank() }
                ?: localProtocolConfig.trojanPassword,
            customPath = payload.d.normalizePath() ?: localProtocolConfig.customPath,
            authToken = payload.u?.lowercase()?.takeUnless { it.isNullOrBlank() }
                ?: localProtocolConfig.authToken
        )
        saveProtocolConfig(updatedProtocol)
    }

    private suspend fun buildConfigUrl(systemConfig: SystemConfig, protocolConfig: ProtocolConfig): String {
        val base = normalizeBaseUrl(systemConfig.workerDomain)
        val identifier = protocolConfig.customPath.ifNotEmpty() ?: protocolConfig.authToken
        require(identifier.isNotEmpty()) { "未配置认证令牌或路径" }
        val cleanIdentifier = identifier.trim('/')
        val uri = Uri.parse(base).buildUpon()
            .appendPath(cleanIdentifier)
            .appendPath("api")
            .appendPath("config")
            .build()
        return uri.toString()
    }

    private fun String.ifNotEmpty(): String? = if (isNotEmpty()) this else null

    private fun normalizeBaseUrl(domain: String): String {
        val trimmed = domain.trim()
        return if (trimmed.startsWith("http", ignoreCase = true)) trimmed else "https://$trimmed"
    }

    private fun ensureRemoteConfigReady(systemConfig: SystemConfig, protocolConfig: ProtocolConfig) {
        require(systemConfig.workerDomain.isNotBlank()) { "Worker域名未配置" }
        val identifier = protocolConfig.customPath.ifNotEmpty() ?: protocolConfig.authToken
        require(!identifier.isNullOrBlank()) { "未配置认证令牌或路径" }
    }

    private fun String?.asYesDefaultTrue(current: Boolean): Boolean {
        return this?.let { !it.equals("no", ignoreCase = true) } ?: current
    }

    private fun String?.asYesDefaultFalse(current: Boolean): Boolean {
        return this?.equals("yes", ignoreCase = true) ?: current
    }

    private fun String?.normalizePath(): String? {
        return this?.trim()?.trim('/')?.takeIf { it.isNotEmpty() }
    }

    private fun <T> Response<T>.requireBody(): T {
        if (!isSuccessful) {
            throw HttpException(this)
        }
        return body() ?: throw IllegalStateException("响应体为空")
    }
}

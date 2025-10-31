package com.example.regionswitcher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 地区信息数据类
 */
@Entity(tableName = "regions")
data class Region(
    @PrimaryKey
    val code: String,
    val nameZh: String,
    val nameEn: String,
    val flag: String,
    val isCustom: Boolean = false,
    val isActive: Boolean = false,
    val priority: Int = 0
)

/**
 * 代理IP配置数据类
 */
@Entity(tableName = "proxy_ips")
data class ProxyIP(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ip: String,
    val port: Int,
    val domain: String? = null,
    val region: String,
    val regionCode: String,
    val isp: String? = null,
    val isPreferred: Boolean = false,
    val isAvailable: Boolean = true,
    val responseTime: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 协议配置数据类
 */
data class ProtocolConfig(
    val enableVless: Boolean = true,
    val enableTrojan: Boolean = false,
    val enableXhttp: Boolean = false,
    val trojanPassword: String = "",
    val customPath: String = "",
    val authToken: String = ""
)

/**
 * 系统配置数据类
 */
data class SystemConfig(
    val currentRegion: String = "",
    val manualRegion: String = "",
    val customProxyIP: String = "",
    val enableRegionMatching: Boolean = true,
    val disableNonTLS: Boolean = false,
    val disablePreferred: Boolean = false,
    val socks5Config: String = "",
    val preferredIPs: String = "",
    val preferredIPsURL: String = "",
    val subConverterUrl: String = "https://url.v1.mk/sub",
    val enablePreferredDomains: Boolean = true,
    val enablePreferredIPs: Boolean = true,
    val enableGithubIPs: Boolean = true,
    val workerDomain: String = "",
    val apiManagementEnabled: Boolean = false,
    val downgradeMode: Boolean = false
)

/**
 * 订阅配置数据类
 */
data class SubscriptionConfig(
    val url: String,
    val clientType: ClientType,
    val isBase64: Boolean = false
)

/**
 * 客户端类型枚举
 */
enum class ClientType(val displayName: String, val apiName: String) {
    CLASH("Clash", "clash"),
    SURGE("Surge", "surge"),
    SINGBOX("Sing-Box", "singbox"),
    LOON("Loon", "loon"),
    QUANTUMULT("Quantumult X", "quanx"),
    V2RAY("V2Ray", "v2ray")
}

/**
 * 连接状态枚举
 */
enum class ConnectionStatus {
    ONLINE,
    OFFLINE,
    CONNECTING,
    ERROR
}

/**
 * 系统状态数据类
 */
data class SystemStatus(
    val connectionStatus: ConnectionStatus = ConnectionStatus.OFFLINE,
    val currentIP: String = "",
    val detectionMethod: String = "",
    val regionMatching: Boolean = true,
    val backupIPStatus: String = "",
    val lastUpdateTime: Long = System.currentTimeMillis()
)

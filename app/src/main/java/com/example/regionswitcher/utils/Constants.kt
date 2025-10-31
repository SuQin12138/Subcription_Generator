package com.example.regionswitcher.utils

/**
 * 应用常量
 */
object Constants {
    
    // 地区代码映射
    val REGION_MAPPING = mapOf(
        "US" to listOf("🇺🇸 美国", "US", "United States"),
        "SG" to listOf("🇸🇬 新加坡", "SG", "Singapore"),
        "JP" to listOf("🇯🇵 日本", "JP", "Japan"),
        "HK" to listOf("🇭🇰 香港", "HK", "Hong Kong"),
        "KR" to listOf("🇰🇷 韩国", "KR", "South Korea"),
        "DE" to listOf("🇩🇪 德国", "DE", "Germany"),
        "SE" to listOf("🇸🇪 瑞典", "SE", "Sweden"),
        "NL" to listOf("🇳🇱 荷兰", "NL", "Netherlands"),
        "FI" to listOf("🇫🇮 芬兰", "FI", "Finland"),
        "GB" to listOf("🇬🇧 英国", "GB", "United Kingdom")
    )
    
    // Cloudflare 端口配置
    val CF_HTTP_PORTS = listOf(80, 8080, 8880, 2052, 2082, 2086, 2095)
    val CF_HTTPS_PORTS = listOf(443, 2053, 2083, 2087, 2096, 8443)
    
    // 默认配置
    const val DEFAULT_SUB_CONVERTER_URL = "https://url.v1.mk/sub"
    const val DEFAULT_PREFERRED_IPS_URL = "https://raw.githubusercontent.com/qwer-search/bestip/refs/heads/main/kejilandbestip.txt"
    const val DEFAULT_AUTH_TOKEN = "351c9981-04b6-4103-aa4b-864aa9c91469"
}
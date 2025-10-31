package com.example.regionswitcher.utils

import android.util.Base64
import java.security.MessageDigest
import java.util.UUID

/**
 * Cloudflare Worker 脚本功能工具类
 * 基于原始脚本改编，提供代理配置和订阅生成功能
 */
object CloudflareWorkerUtils {
    
    // Cloudflare支持的HTTP端口
    private val CF_HTTP_PORTS = listOf(80, 8080, 8880, 2052, 2082, 2086, 2095)
    
    // Cloudflare支持的HTTPS端口
    private val CF_HTTPS_PORTS = listOf(443, 2053, 2083, 2087, 2096, 8443)
    
    // 地区映射（与原始脚本一致）
    private val regionMapping = mapOf(
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
    
    // 备份ProxyIP列表
    private val backupIPs = listOf(
        ProxyIPInfo("ProxyIP.US.CMLiussss.net", "US", 443),
        ProxyIPInfo("ProxyIP.SG.CMLiussss.net", "SG", 443),
        ProxyIPInfo("ProxyIP.JP.CMLiussss.net", "JP", 443),
        ProxyIPInfo("ProxyIP.HK.CMLiussss.net", "HK", 443),
        ProxyIPInfo("ProxyIP.KR.CMLiussss.net", "KR", 443),
        ProxyIPInfo("ProxyIP.DE.CMLiussss.net", "DE", 443),
        ProxyIPInfo("ProxyIP.SE.CMLiussss.net", "SE", 443),
        ProxyIPInfo("ProxyIP.NL.CMLiussss.net", "NL", 443),
        ProxyIPInfo("ProxyIP.FI.CMLiussss.net", "FI", 443),
        ProxyIPInfo("ProxyIP.GB.CMLiussss.net", "GB", 443)
    )
    
    data class ProxyIPInfo(
        val domain: String,
        val region: String,
        val port: Int
    )
    
    data class NodeConfig(
        val ip: String,
        val port: Int,
        val name: String,
        val isTLS: Boolean = true
    )
    
    /**
     * 生成VLESS协议链接
     */
    fun generateVlessLink(
        uuid: String,
        address: String,
        port: Int,
        host: String,
        nodeName: String,
        isTLS: Boolean = true
    ): String {
        val encryption = "none"
        val security = if (isTLS) "tls" else "none"
        val type = "ws"
        val path = "/"
        
        val params = mutableListOf<String>().apply {
            add("encryption=$encryption")
            add("security=$security")
            if (isTLS) {
                add("sni=$host")
                add("fp=chrome")
            }
            add("type=$type")
            add("host=$host")
            add("path=${java.net.URLEncoder.encode(path, "UTF-8")}")
        }.joinToString("&")
        
        val safeAddress = if (address.contains(":")) "[$address]" else address
        return "vless://$uuid@$safeAddress:$port?$params#${java.net.URLEncoder.encode(nodeName, "UTF-8")}"
    }
    
    /**
     * 生成Trojan协议链接
     */
    fun generateTrojanLink(
        password: String,
        address: String,
        port: Int,
        host: String,
        nodeName: String,
        isTLS: Boolean = true
    ): String {
        val security = if (isTLS) "tls" else "none"
        val type = "ws"
        val path = "/"
        
        val params = mutableListOf<String>().apply {
            add("security=$security")
            if (isTLS) {
                add("sni=$host")
                add("fp=chrome")
            }
            add("type=$type")
            add("host=$host")
            add("path=${java.net.URLEncoder.encode(path, "UTF-8")}")
        }.joinToString("&")
        
        val safeAddress = if (address.contains(":")) "[$address]" else address
        return "trojan://$password@$safeAddress:$port?$params#${java.net.URLEncoder.encode(nodeName, "UTF-8")}"
    }
    
    /**
     * 生成节点配置列表
     */
    fun generateNodeConfigs(
        sourceList: List<NodeSource>,
        disableNonTLS: Boolean = false
    ): List<NodeConfig> {
        val configs = mutableListOf<NodeConfig>()
        
        val defaultHttpsPorts = listOf(443)
        val defaultHttpPorts = if (disableNonTLS) emptyList() else listOf(80)
        
        sourceList.forEach { source ->
            val nodeNameBase = source.name.replace("\\s".toRegex(), "_")
            val safeIP = if (source.ip.contains(":")) "[${source.ip}]" else source.ip
            
            val portsToGenerate = mutableListOf<Pair<Int, Boolean>>()
            
            if (source.port != null) {
                val port = source.port
                when {
                    CF_HTTPS_PORTS.contains(port) -> {
                        portsToGenerate.add(Pair(port, true))
                    }
                    CF_HTTP_PORTS.contains(port) -> {
                        if (!disableNonTLS) {
                            portsToGenerate.add(Pair(port, false))
                        }
                    }
                    else -> {
                        portsToGenerate.add(Pair(port, true))
                    }
                }
            } else {
                defaultHttpsPorts.forEach { port ->
                    portsToGenerate.add(Pair(port, true))
                }
                defaultHttpPorts.forEach { port ->
                    portsToGenerate.add(Pair(port, false))
                }
            }
            
            portsToGenerate.forEach { (port, tls) ->
                val wsNodeName = if (tls) {
                    "${nodeNameBase}-${port}-WS-TLS"
                } else {
                    "${nodeNameBase}-${port}-WS"
                }
                
                configs.add(NodeConfig(
                    ip = source.ip,
                    port = port,
                    name = wsNodeName,
                    isTLS = tls
                ))
            }
        }
        
        return configs
    }
    
    /**
     * 计算SHA224哈希（用于Trojan密码）
     */
    fun sha224Hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-224")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 生成Base64编码的订阅内容
     */
    fun generateBase64Subscription(links: List<String>): String {
        val content = links.joinToString("\n")
        return Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)
    }
    
    /**
     * 获取地区的邻近地区列表
     */
    fun getNearbyRegions(region: String): List<String> {
        val nearbyMap = mapOf(
            "US" to listOf("SG", "JP", "HK", "KR"),
            "SG" to listOf("JP", "HK", "KR", "US"),
            "JP" to listOf("SG", "HK", "KR", "US"),
            "HK" to listOf("SG", "JP", "KR", "US"),
            "KR" to listOf("JP", "HK", "SG", "US"),
            "DE" to listOf("NL", "GB", "SE", "FI"),
            "SE" to listOf("DE", "NL", "FI", "GB"),
            "NL" to listOf("DE", "GB", "SE", "FI"),
            "FI" to listOf("SE", "DE", "NL", "GB"),
            "GB" to listOf("DE", "NL", "SE", "FI")
        )
        
        return nearbyMap[region] ?: emptyList()
    }
    
    /**
     * 智能地区选择排序
     */
    fun getSmartRegionSelection(workerRegion: String): List<String> {
        val nearbyRegions = getNearbyRegions(workerRegion)
        val allRegions = listOf("US", "SG", "JP", "HK", "KR", "DE", "SE", "NL", "FI", "GB")
        
        return listOf(
            listOf(workerRegion),
            nearbyRegions,
            allRegions.filter { it != workerRegion && !nearbyRegions.contains(it) }
        ).flatten()
    }
    
    /**
     * 获取最佳备份IP
     */
    fun getBestBackupIP(workerRegion: String): ProxyIPInfo? {
        val priorityRegions = getSmartRegionSelection(workerRegion)
        
        for (region in priorityRegions) {
            val regionIP = backupIPs.find { it.region == region }
            if (regionIP != null) {
                return regionIP
            }
        }
        
        return backupIPs.firstOrNull()
    }
    
    /**
     * 节点源数据类
     */
    data class NodeSource(
        val ip: String,
        val port: Int? = null,
        val name: String
    )
    
    /**
     * 生成默认的节点源列表
     */
    fun getDefaultNodeSources(workerDomain: String): List<NodeSource> {
        return listOf(
            NodeSource(workerDomain, null, "原生地址")
        )
    }
    
    /**
     * 生成完整的订阅链接列表
     */
    fun generateSubscriptionLinks(
        uuid: String,
        workerDomain: String,
        workerRegion: String = "HK",
        enableVless: Boolean = true,
        enableTrojan: Boolean = false,
        trojanPassword: String? = null,
        customSources: List<NodeSource> = emptyList(),
        disableNonTLS: Boolean = false
    ): List<String> {
        val finalLinks = mutableListOf<String>()
        
        // 使用自定义源或默认源
        val sources = if (customSources.isNotEmpty()) {
            customSources
        } else {
            getDefaultNodeSources(workerDomain)
        }
        
        // 生成节点配置
        val nodeConfigs = generateNodeConfigs(sources, disableNonTLS)
        
        // 生成VLESS链接
        if (enableVless) {
            nodeConfigs.forEach { config ->
                val link = generateVlessLink(
                    uuid = uuid,
                    address = config.ip,
                    port = config.port,
                    host = workerDomain,
                    nodeName = config.name,
                    isTLS = config.isTLS
                )
                finalLinks.add(link)
            }
        }
        
        // 生成Trojan链接
        if (enableTrojan) {
            val password = trojanPassword ?: uuid
            nodeConfigs.forEach { config ->
                val link = generateTrojanLink(
                    password = password,
                    address = config.ip,
                    port = config.port,
                    host = workerDomain,
                    nodeName = "${config.name}-Trojan",
                    isTLS = config.isTLS
                )
                finalLinks.add(link)
            }
        }
        
        // 如果没有链接，添加错误节点
        if (finalLinks.isEmpty()) {
            val errorLink = generateVlessLink(
                uuid = "00000000-0000-0000-0000-000000000000",
                address = "127.0.0.1",
                port = 80,
                host = "error.com",
                nodeName = "所有节点获取失败",
                isTLS = false
            )
            finalLinks.add(errorLink)
        }
        
        return finalLinks
    }
}

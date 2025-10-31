package com.example.regionswitcher.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.util.regex.Pattern
import kotlin.math.ln
import kotlin.math.pow

/**
 * 网络工具类
 */
object NetworkUtils {
    
    /**
     * 检查网络连接状态
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * 验证UUID格式
     */
    fun isValidUUID(uuid: String): Boolean {
        val uuidPattern = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE
        )
        return uuidPattern.matcher(uuid).matches()
    }
    
    /**
     * 验证IP地址格式
     */
    fun isValidIP(ip: String): Boolean {
        // IPv4 验证
        val ipv4Pattern = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
        )
        if (ipv4Pattern.matcher(ip).matches()) return true
        
        // IPv6 验证（简化版）
        val ipv6Pattern = Pattern.compile(
            "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$"
        )
        if (ipv6Pattern.matcher(ip).matches()) return true
        
        // IPv6 简写格式验证
        val ipv6ShortPattern = Pattern.compile(
            "^::1$|^::$|^(?:[0-9a-fA-F]{1,4}:)*::(?:[0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4}$"
        )
        return ipv6ShortPattern.matcher(ip).matches()
    }
    
    /**
     * 解析地址和端口
     */
    fun parseAddressAndPort(input: String): Pair<String, Int?> {
        // 处理IPv6格式 [::1]:8080
        if (input.contains('[') && input.contains(']')) {
            val regex = Regex("""^\[([^\]]+)\](?::(\d+))?$""")
            val matchResult = regex.find(input)
            if (matchResult != null) {
                val address = matchResult.groupValues[1]
                val port = matchResult.groupValues[2].toIntOrNull()
                return Pair(address, port)
            }
        }
        
        // 处理IPv4格式 192.168.1.1:8080
        val lastColonIndex = input.lastIndexOf(':')
        if (lastColonIndex > 0) {
            val address = input.substring(0, lastColonIndex)
            val portStr = input.substring(lastColonIndex + 1)
            val port = portStr.toIntOrNull()
            
            if (port != null && port in 1..65535) {
                return Pair(address, port)
            }
        }
        
        return Pair(input, null)
    }
    
    /**
     * 格式化字节大小
     */
    fun formatBytes(bytes: Long): String {
        val unit = 1024
        if (bytes < unit) return "$bytes B"
        
    val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1]

    return "%.1f %sB".format(bytes / unit.toDouble().pow(exp.toDouble()), pre)
    }
    
    /**
     * 获取网络类型
     */
    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "无网络"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "未知"
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动网络"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
            else -> "其他"
        }
    }
}
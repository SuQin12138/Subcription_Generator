package com.example.regionswitcher.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.regionswitcher.databinding.ItemSubscriptionLinkBinding

class SubscriptionLinksAdapter(
    private val onLinkClick: (String) -> Unit
) : ListAdapter<String, SubscriptionLinksAdapter.LinkViewHolder>(LinkDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkViewHolder {
        val binding = ItemSubscriptionLinkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LinkViewHolder(binding, onLinkClick)
    }
    
    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    fun updateLinks(links: List<String>) {
        submitList(links)
    }
    
    class LinkViewHolder(
        private val binding: ItemSubscriptionLinkBinding,
        private val onLinkClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(link: String) {
            // 解析链接信息
            val linkInfo = parseLinkInfo(link)
            
            binding.tvProtocol.text = linkInfo.protocol
            binding.tvNodeName.text = linkInfo.nodeName
            binding.tvAddress.text = "${linkInfo.address}:${linkInfo.port}"
            binding.tvSecurity.text = linkInfo.security
            
            // 设置协议颜色
            val protocolColor = when (linkInfo.protocol.lowercase()) {
                "vless" -> android.graphics.Color.parseColor("#00FF00")
                "trojan" -> android.graphics.Color.parseColor("#FF6B35")
                "vmess" -> android.graphics.Color.parseColor("#00BFFF")
                else -> android.graphics.Color.parseColor("#FFFFFF")
            }
            binding.tvProtocol.setTextColor(protocolColor)
            
            // 设置安全性颜色
            val securityColor = if (linkInfo.security.contains("TLS", ignoreCase = true)) {
                android.graphics.Color.parseColor("#00FF00")
            } else {
                android.graphics.Color.parseColor("#FFA500")
            }
            binding.tvSecurity.setTextColor(securityColor)
            
            // 点击事件
            binding.root.setOnClickListener {
                onLinkClick(link)
            }
            
            // 长按显示完整链接
            binding.root.setOnLongClickListener {
                binding.tvFullLink.text = link
                binding.tvFullLink.visibility = if (binding.tvFullLink.visibility == android.view.View.VISIBLE) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
                true
            }
        }
        
        private fun parseLinkInfo(link: String): LinkInfo {
            return try {
                when {
                    link.startsWith("vless://") -> parseVlessLink(link)
                    link.startsWith("trojan://") -> parseTrojanLink(link)
                    link.startsWith("vmess://") -> parseVmessLink(link)
                    else -> LinkInfo("未知", "未知节点", "未知", "未知", "未知")
                }
            } catch (e: Exception) {
                LinkInfo("解析错误", "无法解析", "N/A", "N/A", "N/A")
            }
        }
        
        private fun parseVlessLink(link: String): LinkInfo {
            // vless://uuid@address:port?params#name
            val uri = android.net.Uri.parse(link)
            val address = uri.host ?: "未知"
            val port = uri.port.toString()
            val name = java.net.URLDecoder.decode(uri.fragment ?: "VLESS节点", "UTF-8")
            val security = uri.getQueryParameter("security") ?: "none"
            val securityDisplay = if (security == "tls") "TLS" else "无加密"
            
            return LinkInfo("VLESS", name, address, port, securityDisplay)
        }
        
        private fun parseTrojanLink(link: String): LinkInfo {
            // trojan://password@address:port?params#name
            val uri = android.net.Uri.parse(link)
            val address = uri.host ?: "未知"
            val port = uri.port.toString()
            val name = java.net.URLDecoder.decode(uri.fragment ?: "Trojan节点", "UTF-8")
            val security = uri.getQueryParameter("security") ?: "tls"
            val securityDisplay = if (security == "tls") "TLS" else "无加密"
            
            return LinkInfo("Trojan", name, address, port, securityDisplay)
        }
        
        private fun parseVmessLink(link: String): LinkInfo {
            // VMess 链接解析比较复杂，这里简化处理
            return LinkInfo("VMess", "VMess节点", "未知", "未知", "TLS")
        }
        
        data class LinkInfo(
            val protocol: String,
            val nodeName: String,
            val address: String,
            val port: String,
            val security: String
        )
    }
    
    private class LinkDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
        
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

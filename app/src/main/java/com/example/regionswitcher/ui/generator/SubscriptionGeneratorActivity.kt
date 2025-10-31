package com.example.regionswitcher.ui.generator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.regionswitcher.databinding.ActivitySubscriptionGeneratorBinding
import com.example.regionswitcher.ui.adapter.SubscriptionLinksAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SubscriptionGeneratorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySubscriptionGeneratorBinding
    private val viewModel: SubscriptionGeneratorViewModel by viewModels()
    private lateinit var adapter: SubscriptionLinksAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
        setupClickListeners()
        
        // 初始化生成
        viewModel.generateDefaultSubscription()
    }
    
    private fun setupUI() {
        // 设置RecyclerView
        adapter = SubscriptionLinksAdapter { link ->
            copyToClipboard(link, "订阅链接已复制")
        }
        
        binding.recyclerViewLinks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewLinks.adapter = adapter
        
        // 设置初始配置
        binding.switchVless.isChecked = true
        binding.switchTrojan.isChecked = false
        binding.switchNonTls.isChecked = false
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.subscriptionLinks.collect { links ->
                adapter.updateLinks(links)
                binding.tvLinkCount.text = "共 ${links.size} 个节点"
            }
        }
        
        lifecycleScope.launch {
            viewModel.base64Subscription.collect { base64 ->
                binding.tvBase64Content.text = base64
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(this@SubscriptionGeneratorActivity, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        // 生成订阅按钮
        binding.btnGenerate.setOnClickListener {
            val config = SubscriptionGeneratorViewModel.GeneratorConfig(
                uuid = binding.etUuid.text.toString().trim(),
                workerDomain = binding.etWorkerDomain.text.toString().trim(),
                enableVless = binding.switchVless.isChecked,
                enableTrojan = binding.switchTrojan.isChecked,
                trojanPassword = binding.etTrojanPassword.text.toString().trim().ifEmpty { null },
                disableNonTLS = binding.switchNonTls.isChecked,
                customPath = binding.etCustomPath.text.toString().trim().ifEmpty { null }
            )
            
            viewModel.generateSubscription(config)
        }
        
        // 复制Base64按钮
        binding.btnCopyBase64.setOnClickListener {
            val base64 = binding.tvBase64Content.text.toString()
            if (base64.isNotEmpty()) {
                copyToClipboard(base64, "Base64订阅内容已复制")
            }
        }
        
        // 客户端按钮们
        binding.btnClash.setOnClickListener {
            generateClientSubscription("clash")
        }
        
        binding.btnSurge.setOnClickListener {
            generateClientSubscription("surge")
        }
        
        binding.btnSingBox.setOnClickListener {
            generateClientSubscription("singbox")
        }
        
        binding.btnQuantumultX.setOnClickListener {
            generateClientSubscription("quanx")
        }
        
        binding.btnV2ray.setOnClickListener {
            generateClientSubscription("v2ray")
        }
        
        binding.btnLoon.setOnClickListener {
            generateClientSubscription("loon")
        }
        
        // 协议开关监听
        binding.switchVless.setOnCheckedChangeListener { _, _ ->
            checkProtocolSelection()
        }
        
        binding.switchTrojan.setOnCheckedChangeListener { _, _ ->
            checkProtocolSelection()
            binding.layoutTrojanPassword.visibility = if (binding.switchTrojan.isChecked) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
        
        // 添加自定义IP按钮
        binding.btnAddCustomIP.setOnClickListener {
            val ip = binding.etCustomIP.text.toString().trim()
            val port = binding.etCustomPort.text.toString().trim()
            val name = binding.etCustomName.text.toString().trim()
            
            if (ip.isNotEmpty() && port.isNotEmpty() && name.isNotEmpty()) {
                viewModel.addCustomIP(ip, port.toIntOrNull() ?: 443, name)
                
                // 清空输入框
                binding.etCustomIP.text?.clear()
                binding.etCustomPort.text?.clear()
                binding.etCustomName.text?.clear()
                
                Toast.makeText(this, "已添加自定义IP: $name", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "请填写完整的IP信息", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 清除自定义IP按钮
        binding.btnClearCustomIPs.setOnClickListener {
            viewModel.clearCustomIPs()
            Toast.makeText(this, "已清除所有自定义IP", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkProtocolSelection() {
        if (!binding.switchVless.isChecked && !binding.switchTrojan.isChecked) {
            binding.switchVless.isChecked = true
            Toast.makeText(this, "至少需要启用一个协议", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun generateClientSubscription(clientType: String) {
        val workerDomain = binding.etWorkerDomain.text.toString().trim()
        val customPath = binding.etCustomPath.text.toString().trim()
        
        if (workerDomain.isEmpty()) {
            Toast.makeText(this, "请先填写Worker域名", Toast.LENGTH_SHORT).show()
            return
        }
        
        val baseUrl = "https://$workerDomain"
        val subPath = if (customPath.isNotEmpty()) "/$customPath" else "/${binding.etUuid.text.toString().trim()}"
        val subscriptionUrl = "$baseUrl$subPath/sub"
        
        val clientUrl = when (clientType) {
            "v2ray" -> subscriptionUrl
            else -> {
                val converterUrl = "https://url.v1.mk/sub"
                val encodedUrl = java.net.URLEncoder.encode(subscriptionUrl, "UTF-8")
                "$converterUrl?target=$clientType&url=$encodedUrl&insert=false"
            }
        }
        
        copyToClipboard(clientUrl, "${clientType.uppercase()} 订阅链接已复制")
    }
    
    private fun copyToClipboard(text: String, message: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("订阅链接", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

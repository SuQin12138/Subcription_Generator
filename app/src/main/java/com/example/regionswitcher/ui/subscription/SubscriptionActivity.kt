package com.example.regionswitcher.ui.subscription

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.regionswitcher.R
import com.example.regionswitcher.data.model.ClientType
import com.example.regionswitcher.databinding.ActivitySubscriptionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SubscriptionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySubscriptionBinding
    private val viewModel: SubscriptionViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        observeViewModel()
        setupClickListeners()
        
        // 初始生成订阅
        viewModel.generateSubscription(ClientType.CLASH)
    }
    
    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.subscription_title)
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.subscriptionUrl.collect { url ->
                binding.tvSubscriptionUrl.text = url
            }
        }
        
        lifecycleScope.launch {
            viewModel.nodeCount.collect { count ->
                binding.tvNodeCount.text = count.toString()
            }
        }
        
        lifecycleScope.launch {
            viewModel.regionCount.collect { count ->
                binding.tvRegionCount.text = count.toString()
            }
        }
        
        lifecycleScope.launch {
            viewModel.lastUpdate.collect { timestamp ->
                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                binding.tvLastUpdate.text = dateFormat.format(Date(timestamp))
            }
        }
        
        lifecycleScope.launch {
            viewModel.message.collect { message ->
                message?.let {
                    Toast.makeText(this@SubscriptionActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.btnGenerateSubscription.isEnabled = !isLoading
                binding.btnCopySubscription.isEnabled = !isLoading && binding.tvSubscriptionUrl.text.isNotEmpty()
            }
        }
    }
    
    private fun setupClickListeners() {
        // 生成订阅
        binding.btnGenerateSubscription.setOnClickListener {
            val selectedClientType = getSelectedClientType()
            viewModel.generateSubscription(selectedClientType)
        }
        
        // 复制订阅链接
        binding.btnCopySubscription.setOnClickListener {
            copyToClipboard(binding.tvSubscriptionUrl.text.toString())
        }
        
        // 客户端类型选择监听
        binding.rgClientType.setOnCheckedChangeListener { _, checkedId ->
            val selectedClientType = getSelectedClientType()
            viewModel.generateSubscription(selectedClientType)
        }
    }
    
    private fun getSelectedClientType(): ClientType {
        return when (binding.rgClientType.checkedRadioButtonId) {
            R.id.rbClash -> ClientType.CLASH
            R.id.rbSurge -> ClientType.SURGE
            R.id.rbSingBox -> ClientType.SINGBOX
            R.id.rbLoon -> ClientType.LOON
            R.id.rbQuantumult -> ClientType.QUANTUMULT
            R.id.rbV2Ray -> ClientType.V2RAY
            else -> ClientType.CLASH // 默认Base64，这里映射为Clash
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("订阅链接", text)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, getString(R.string.subscription_copied), Toast.LENGTH_SHORT).show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

package com.example.regionswitcher.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * 响应 Cloudflare Worker 配置更新接口的结果。
 */
data class WorkerUpdateResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("config") val config: WorkerConfigPayload? = null
)

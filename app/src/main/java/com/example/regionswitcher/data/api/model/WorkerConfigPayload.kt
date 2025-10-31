package com.example.regionswitcher.data.api.model

import com.example.regionswitcher.data.model.ProtocolConfig
import com.example.regionswitcher.data.model.SystemConfig
import com.google.gson.annotations.SerializedName

data class WorkerConfigPayload(
    @SerializedName("wk") val wk: String? = null,
    @SerializedName("p") val p: String? = null,
    @SerializedName("yx") val yx: String? = null,
    @SerializedName("yxURL") val yxURL: String? = null,
    @SerializedName("s") val s: String? = null,
    @SerializedName("d") val d: String? = null,
    @SerializedName("u") val u: String? = null,
    @SerializedName("rm") val rm: String? = null,
    @SerializedName("qj") val qj: String? = null,
    @SerializedName("dkby") val dkby: String? = null,
    @SerializedName("yxby") val yxby: String? = null,
    @SerializedName("enableVless") val enableVless: String? = null,
    @SerializedName("enableTrojan") val enableTrojan: String? = null,
    @SerializedName("enableXhttp") val enableXhttp: String? = null,
    @SerializedName("trojanPassword") val trojanPassword: String? = null,
    @SerializedName("enablePreferredDomains") val enablePreferredDomains: String? = null,
    @SerializedName("enablePreferredIPs") val enablePreferredIPs: String? = null,
    @SerializedName("enableGithubIPs") val enableGithubIPs: String? = null,
    @SerializedName("subConverterUrl") val subConverterUrl: String? = null,
    @SerializedName("apiEnabled") val apiEnabled: String? = null,
    @SerializedName("kvEnabled") val kvEnabled: Boolean? = null
) {
    companion object {
        fun fromConfigs(system: SystemConfig, protocol: ProtocolConfig): WorkerConfigPayload {
            return WorkerConfigPayload(
                wk = system.manualRegion.ifBlank { null },
                p = system.customProxyIP.ifBlank { null },
                yx = system.preferredIPs.ifBlank { null },
                yxURL = system.preferredIPsURL.ifBlank { null },
                s = system.socks5Config.ifBlank { null },
                d = protocol.customPath.ifBlank { null },
                u = protocol.authToken.ifBlank { null },
                rm = if (system.enableRegionMatching) null else "no",
                qj = if (system.downgradeMode) "no" else null,
                dkby = if (system.disableNonTLS) "yes" else null,
                yxby = if (system.disablePreferred) "yes" else null,
                enableVless = if (protocol.enableVless) null else "no",
                enableTrojan = if (protocol.enableTrojan) "yes" else null,
                enableXhttp = if (protocol.enableXhttp) "yes" else null,
                trojanPassword = protocol.trojanPassword.ifBlank { null },
                enablePreferredDomains = if (system.enablePreferredDomains) null else "no",
                enablePreferredIPs = if (system.enablePreferredIPs) null else "no",
                enableGithubIPs = if (system.enableGithubIPs) null else "no",
                subConverterUrl = system.subConverterUrl.ifBlank { null },
                apiEnabled = if (system.apiManagementEnabled) "yes" else null
            )
        }
    }
}
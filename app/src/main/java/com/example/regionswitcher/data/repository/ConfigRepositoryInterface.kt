package com.example.regionswitcher.data.repository

import com.example.regionswitcher.data.model.SystemConfig
import com.example.regionswitcher.data.model.ProtocolConfig
import kotlinx.coroutines.flow.Flow

interface ConfigRepository {
    fun getSystemConfig(): Flow<SystemConfig>
    fun getProtocolConfig(): Flow<ProtocolConfig>
    suspend fun saveSystemConfig(config: SystemConfig)
    suspend fun saveProtocolConfig(config: ProtocolConfig)
    suspend fun resetConfig()
    suspend fun fetchRemoteConfig(): Result<Unit>
    suspend fun updateRemoteConfig(systemConfig: SystemConfig, protocolConfig: ProtocolConfig): Result<String?>
}

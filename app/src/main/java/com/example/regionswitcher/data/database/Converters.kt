package com.example.regionswitcher.data.database

import androidx.room.TypeConverter
import com.example.regionswitcher.data.model.ConnectionStatus

/**
 * Room数据库类型转换器
 */
class Converters {
    
    @TypeConverter
    fun fromConnectionStatus(status: ConnectionStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toConnectionStatus(status: String): ConnectionStatus {
        return try {
            ConnectionStatus.valueOf(status)
        } catch (e: IllegalArgumentException) {
            ConnectionStatus.OFFLINE
        }
    }
}

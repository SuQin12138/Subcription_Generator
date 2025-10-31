package com.example.regionswitcher.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.regionswitcher.data.model.Region
import com.example.regionswitcher.data.model.ProxyIP

/**
 * 应用数据库
 */
@Database(
    entities = [Region::class, ProxyIP::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun regionDao(): RegionDao
    abstract fun proxyIPDao(): ProxyIPDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "region_switcher_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * 数据库回调，用于初始化数据
 */
class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        super.onCreate(db)
        // 插入默认地区数据
        db.execSQL("""
            INSERT INTO regions (code, nameZh, nameEn, flag, isCustom, isActive, priority) VALUES 
            ('US', '🇺🇸 美国', 'United States', '🇺🇸', 0, 0, 1),
            ('SG', '🇸🇬 新加坡', 'Singapore', '🇸🇬', 0, 0, 2),
            ('JP', '🇯🇵 日本', 'Japan', '🇯🇵', 0, 0, 3),
            ('HK', '🇭🇰 香港', 'Hong Kong', '🇭🇰', 0, 1, 4),
            ('KR', '🇰🇷 韩国', 'South Korea', '🇰🇷', 0, 0, 5),
            ('DE', '🇩🇪 德国', 'Germany', '🇩🇪', 0, 0, 6),
            ('SE', '🇸🇪 瑞典', 'Sweden', '🇸🇪', 0, 0, 7),
            ('NL', '🇳🇱 荷兰', 'Netherlands', '🇳🇱', 0, 0, 8),
            ('FI', '🇫🇮 芬兰', 'Finland', '🇫🇮', 0, 0, 9),
            ('GB', '🇬🇧 英国', 'United Kingdom', '🇬🇧', 0, 0, 10)
        """)
    }
}

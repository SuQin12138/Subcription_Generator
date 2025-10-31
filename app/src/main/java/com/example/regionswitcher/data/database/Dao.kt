package com.example.regionswitcher.data.database

import androidx.room.*
import com.example.regionswitcher.data.model.Region
import com.example.regionswitcher.data.model.ProxyIP
import kotlinx.coroutines.flow.Flow

/**
 * 地区数据访问对象
 */
@Dao
interface RegionDao {
    
    @Query("SELECT * FROM regions ORDER BY priority ASC")
    fun getAllRegions(): Flow<List<Region>>
    
    @Query("SELECT * FROM regions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveRegion(): Region?
    
    @Query("SELECT * FROM regions WHERE code = :code")
    suspend fun getRegionByCode(code: String): Region?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegion(region: Region)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegions(regions: List<Region>)
    
    @Update
    suspend fun updateRegion(region: Region)
    
    @Query("UPDATE regions SET isActive = 0")
    suspend fun deactivateAllRegions()
    
    @Query("UPDATE regions SET isActive = 1 WHERE code = :code")
    suspend fun activateRegion(code: String)
    
    @Delete
    suspend fun deleteRegion(region: Region)
    
    @Query("DELETE FROM regions WHERE isCustom = 1")
    suspend fun deleteCustomRegions()
}

/**
 * 代理IP数据访问对象
 */
@Dao
interface ProxyIPDao {
    
    @Query("SELECT * FROM proxy_ips ORDER BY responseTime ASC")
    fun getAllProxyIPs(): Flow<List<ProxyIP>>
    
    @Query("SELECT * FROM proxy_ips WHERE region = :region ORDER BY responseTime ASC")
    fun getProxyIPsByRegion(region: String): Flow<List<ProxyIP>>
    
    @Query("SELECT * FROM proxy_ips WHERE isPreferred = 1 ORDER BY responseTime ASC")
    fun getPreferredProxyIPs(): Flow<List<ProxyIP>>
    
    @Query("SELECT * FROM proxy_ips WHERE isAvailable = 1 ORDER BY responseTime ASC")
    fun getAvailableProxyIPs(): Flow<List<ProxyIP>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxyIP(proxyIP: ProxyIP)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxyIPs(proxyIPs: List<ProxyIP>)
    
    @Update
    suspend fun updateProxyIP(proxyIP: ProxyIP)
    
    @Query("UPDATE proxy_ips SET isAvailable = :isAvailable, responseTime = :responseTime WHERE id = :id")
    suspend fun updateAvailability(id: Long, isAvailable: Boolean, responseTime: Long)
    
    @Delete
    suspend fun deleteProxyIP(proxyIP: ProxyIP)
    
    @Query("DELETE FROM proxy_ips")
    suspend fun deleteAllProxyIPs()
    
    @Query("DELETE FROM proxy_ips WHERE region = :region")
    suspend fun deleteProxyIPsByRegion(region: String)
}

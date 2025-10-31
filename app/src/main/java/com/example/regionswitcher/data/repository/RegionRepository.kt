package com.example.regionswitcher.data.repository

import com.example.regionswitcher.data.database.RegionDao
import com.example.regionswitcher.data.model.Region
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RegionRepository @Inject constructor(
    private val regionDao: RegionDao
) {
    
    /**
     * 获取所有地区
     */
    fun getAllRegions(): Flow<List<Region>> {
        return regionDao.getAllRegions()
    }
      /**
     * 获取当前激活的地区
     */
    fun getCurrentRegion(): Flow<Region?> {
        return regionDao.getAllRegions().map { regions ->
            regions.find { it.isActive }
        }
    }
    
    /**
     * 根据代码获取地区
     */
    suspend fun getRegionByCode(code: String): Region? {
        return regionDao.getRegionByCode(code)
    }
    
    /**
     * 设置激活地区
     */
    suspend fun setActiveRegion(regionCode: String) {
        regionDao.deactivateAllRegions()
        regionDao.activateRegion(regionCode)
    }
    
    /**
     * 清除手动选择的地区
     */
    suspend fun clearManualRegion() {
        regionDao.deactivateAllRegions()
        // 可以设置默认地区或等待自动检测
        regionDao.activateRegion("HK") // 默认香港
    }
    
    /**
     * 添加自定义地区
     */
    suspend fun addCustomRegion(region: Region) {
        val customRegion = region.copy(isCustom = true)
        regionDao.insertRegion(customRegion)
    }
    
    /**
     * 删除自定义地区
     */
    suspend fun deleteCustomRegions() {
        regionDao.deleteCustomRegions()
    }
}

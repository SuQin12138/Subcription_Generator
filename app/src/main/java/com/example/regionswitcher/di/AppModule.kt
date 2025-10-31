package com.example.regionswitcher.di

import android.content.Context
import androidx.room.Room
import com.example.regionswitcher.data.database.AppDatabase
import com.example.regionswitcher.data.database.RegionDao
import com.example.regionswitcher.data.database.ProxyIPDao
import com.example.regionswitcher.data.api.WorkerApiService
import com.example.regionswitcher.data.repository.ConfigRepository
import com.example.regionswitcher.data.repository.ConfigRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "region_switcher_database"
        )
        .addCallback(com.example.regionswitcher.data.database.DatabaseCallback())
        .build()
    }
    
    @Provides
    fun provideRegionDao(database: AppDatabase): RegionDao {
        return database.regionDao()
    }
    
    @Provides
    fun provideProxyIPDao(database: AppDatabase): ProxyIPDao {
        return database.proxyIPDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideConfigRepository(
        @ApplicationContext context: Context,
        workerApiService: WorkerApiService
    ): ConfigRepository {
        return ConfigRepositoryImpl(context, workerApiService)
    }
}

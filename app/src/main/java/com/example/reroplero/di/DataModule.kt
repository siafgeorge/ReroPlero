package com.example.reroplero.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.reroplero.R
import com.example.reroplero.data.local.AppDao
import com.example.reroplero.data.local.AppDatabase
import com.example.reroplero.data.remote.CoinGeckoApi
import com.example.reroplero.data.remote.FrankfurterApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context):
            AppDatabase = Room.databaseBuilder(
                context,
        AppDatabase::class.java,
                context.getString(R.string.database)
            ).build()

    @Provides
    fun provideDao(db: AppDatabase) : AppDao = db.dao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context):
        DataStore<Preferences> = PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("session")
    }

    @Provides
    @Singleton
    fun provideFrankfurterApi(): FrankfurterApi =
        Retrofit.Builder()
            .baseUrl(FrankfurterApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FrankfurterApi::class.java)

    @Provides
    @Singleton
    fun provideCoinGeckoApi(): CoinGeckoApi =
        Retrofit.Builder()
            .baseUrl(CoinGeckoApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CoinGeckoApi::class.java)
}
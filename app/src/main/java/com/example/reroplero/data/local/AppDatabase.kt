package com.example.reroplero.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.data.local.models.User

@Database(entities = [User::class, Payment::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao() : AppDao

    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                 INSTANCE ?: Room.databaseBuilder(
                     context.applicationContext,
                     AppDatabase::class.java,
                     "reroplero.db").build().also {INSTANCE = it} //TODO make it constant
            }
    }
}

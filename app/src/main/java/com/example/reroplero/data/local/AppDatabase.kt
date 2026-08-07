package com.example.reroplero.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.reroplero.R
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.data.local.models.User

@Database(entities = [User::class, Payment::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao() : AppDao

    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1,2){
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN profilePicturePath TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                 INSTANCE ?: Room.databaseBuilder(
                     context.applicationContext,
                     AppDatabase::class.java,
                     context.getString(R.string.database)
                 ).addMigrations(MIGRATION_1_2)
                  .build().also {INSTANCE = it}
            }

    }
}

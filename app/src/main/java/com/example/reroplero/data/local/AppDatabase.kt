package com.example.reroplero.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.data.local.models.User

@Database(entities = [User::class, Payment::class], version = 5)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao() : AppDao

    companion object{
        val MIGRATION_1_2 = object : Migration(1,2){
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN profilePicturePath TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2,3){
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payments ADD COLUMN receiptUid TEXT")
                // The index name must match what Room generates for the entity
                // (index_<table>_<col>_<col>) or the schema check fails on open.
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_payments_username_receiptUid` " +
                            "ON `payments` (`username`, `receiptUid`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3,4){
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payments ADD COLUMN receiptLine INTEGER")
                // A receipt now imports as one payment per line, so the old
                // (username, receiptUid) index would reject every line after the first.
                db.execSQL("DROP INDEX IF EXISTS `index_payments_username_receiptUid`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_payments_username_receiptUid_receiptLine` " +
                            "ON `payments` (`username`, `receiptUid`, `receiptLine`)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4,5) {
            override fun migrate(db: SupportSQLiteDatabase){
                db.execSQL("ALTER TABLE payments ADD COLUMN note TEXT")
            }
        }
    }
}

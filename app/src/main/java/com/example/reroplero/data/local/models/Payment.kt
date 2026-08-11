package com.example.reroplero.data.local.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


// A receipt imports as one payment per line, so the natural key is
// (username, receiptUid, receiptLine): the same receipt can't be imported twice,
// but its individual lines don't collide with each other. Username is included so
// two users can each record the same receipt. SQLite treats NULLs as distinct in a
// unique index, so hand-entered payments (receiptUid = null) are unaffected.
@Entity(
    tableName = "payments",
    indices = [Index(value = ["username", "receiptUid", "receiptLine"], unique = true)]
)
data class Payment(
    @PrimaryKey val id: String,
    val username: String,
    val category: String,
    val cost: Double,
    val timestamp: Long,
    val receiptUid: String? = null,
    val receiptLine: Int? = null,
    val note: String? = null
){
    operator fun plus(other: Payment): Double {
        return this.cost + other.cost
    }
}

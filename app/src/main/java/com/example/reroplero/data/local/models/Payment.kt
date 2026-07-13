package com.example.reroplero.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey val id: String,
    val username: String,
    val category: String,
    val cost: Double,
    val timestamp: Long

){
    operator fun plus(other: Payment): Double {
        return this.cost + other.cost
    }
}

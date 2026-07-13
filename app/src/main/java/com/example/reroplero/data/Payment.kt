package com.example.reroplero.data

data class Payment(
    val id: String,
    val category: String,
    val cost: Double,
    val timestamp: Long

){
    operator fun plus(other: Payment): Double {
        return this.cost + other.cost
    }
}

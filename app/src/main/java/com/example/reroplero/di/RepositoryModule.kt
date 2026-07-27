package com.example.reroplero.di

import com.example.reroplero.data.PaymentRepoImpl
import com.example.reroplero.data.PaymentRepository
import com.example.reroplero.data.UserRepoImpl
import com.example.reroplero.data.UserRepository
import com.example.reroplero.data.remote.CurrencyRepoImpl
import com.example.reroplero.data.remote.CurrencyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindUserRepository(impl: UserRepoImpl): UserRepository

    @Binds
    abstract fun bindPaymentRepository(impl: PaymentRepoImpl) : PaymentRepository

    @Binds
    abstract fun bindCurrencyRepository(impl: CurrencyRepoImpl) : CurrencyRepository
}
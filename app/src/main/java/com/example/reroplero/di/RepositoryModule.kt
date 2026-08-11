package com.example.reroplero.di

import com.example.reroplero.data.PaymentRepoImpl
import com.example.reroplero.data.SessionRepoImpl
import com.example.reroplero.data.UserRepoImpl
import com.example.reroplero.data.remote.CryptoRepoImpl
import com.example.reroplero.data.remote.CurrencyRepoImpl
import com.example.reroplero.data.remote.ReceiptRepoImpl
import com.example.reroplero.domain.CryptoRepository
import com.example.reroplero.domain.CurrencyRepository
import com.example.reroplero.domain.PaymentRepository
import com.example.reroplero.domain.ReceiptRepository
import com.example.reroplero.domain.SessionRepository
import com.example.reroplero.domain.UserRepository
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

    @Binds
    abstract fun bindCryptoRepository(impl: CryptoRepoImpl) : CryptoRepository

    @Binds
    abstract fun bindSessionStore(impl: SessionRepoImpl) : SessionRepository

    @Binds
    abstract fun bindReceiptRepository(impl: ReceiptRepoImpl) : ReceiptRepository
}
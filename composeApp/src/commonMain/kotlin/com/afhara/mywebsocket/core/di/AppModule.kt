package com.afhara.mywebsocket.core.di

import com.afhara.mywebsocket.core.network.CoinGeckoApi
import com.afhara.mywebsocket.core.network.KrakenWebSocket
import com.afhara.mywebsocket.core.network.HttpClientFactory
import com.afhara.mywebsocket.data.datasource.CryptoRemoteDataSource
import com.afhara.mywebsocket.data.repository.CryptoRepository
import com.afhara.mywebsocket.data.repository.CryptoRepositoryImpl
import com.afhara.mywebsocket.feature.dashboard.persentation.DashboardViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

val appModule = module {
    // Network
    single { HttpClientFactory.create() }
    single { CoinGeckoApi(get()) }
    single { KrakenWebSocket(get()) }

    // DataSource
    single { CryptoRemoteDataSource(get(), get()) }

    // Repository
    single<CryptoRepository> { CryptoRepositoryImpl(get()) }

    // ViewModel
    viewModel { DashboardViewModel(get()) }
}
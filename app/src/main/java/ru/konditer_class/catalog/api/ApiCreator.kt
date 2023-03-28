package ru.konditer_class.catalog.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import ru.konditer_class.catalog.BuildConfig
import java.util.concurrent.TimeUnit

class ApiCreator {

    private val clientBuilder = OkHttpClient.Builder()

    private val retrofitBuilder = Retrofit.Builder()
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create())

    fun create(): Api {
        val apiUrl = "http://vpn.konditer-class.ru:9175/Trade/hs/"
        val okHttp = clientBuilder
            .readTimeout(120, TimeUnit.SECONDS)
            .connectTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor()
                .setLevel(if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE))
            .build()
        val retrofit = retrofitBuilder
            .baseUrl(apiUrl)
            .client(okHttp)
            .build()
        return retrofit.create(Api::class.java)
    }
}

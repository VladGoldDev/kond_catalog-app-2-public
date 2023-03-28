package ru.konditer_class.catalog.api

import io.reactivex.Single
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import ru.konditer_class.catalog.data.OstTov
import ru.konditer_class.catalog.data.Price
import ru.konditer_class.catalog.data.User

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming as Streaming1


interface Api {

    @POST("GetOstTov")
    fun getOstTov(@Body code: RequestBody): Single<List<OstTov>>

    @POST("GetPrice")
    fun getPrice(@Body code: RequestBody): Single<List<Price>>

    @POST("GetIdUser")
    fun getIdUser(@Body auth: RequestBody): Single<List<User>>

    @Streaming1
    @POST("GetFoto")
    fun getPhoto(@Body name: RequestBody): Single<ResponseBody>

    @POST("GetDocFromRK")
    fun sendOrder(@Body data: RequestBody): Single<ResponseBody>

    @GET("SendMessEntr/{id}")
    fun sendTelegramKey(@Path("id") userId: String): Single<ResponseBody>

    @GET("GetCurStatus/{id}")
    fun getAccountStatus(@Path("id") userId: String): Single<ResponseBody>

}
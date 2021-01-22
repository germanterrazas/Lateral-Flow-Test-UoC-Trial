package uoc.ifm.dial.saamd.LFDApp.api

import uoc.ifm.dial.saamd.LFDApp.model.Post
import retrofit2.Response
import retrofit2.http.*

interface SimpleApi {
    @GET("posts1/1")
    suspend fun getPost(): Response<Post>

    @Headers("Accept: application/json","Content-Type: application/json")
    @POST("lfdtests")
    suspend fun pushPost(
            @Body post: Post
    ):Response<Post>
}
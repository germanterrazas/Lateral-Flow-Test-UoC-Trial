package uoc.ifm.dial.saamd.LFDApp.repository

import uoc.ifm.dial.saamd.LFDApp.api.RetrofitInstance
import uoc.ifm.dial.saamd.LFDApp.model.Post
import retrofit2.Response

class Repository {

    suspend fun getPost(): Response<Post> {
        return RetrofitInstance.api.getPost()
    }

    suspend fun pushPost(post: Post): Response<Post> {
        return RetrofitInstance.api.pushPost(post)
    }
}
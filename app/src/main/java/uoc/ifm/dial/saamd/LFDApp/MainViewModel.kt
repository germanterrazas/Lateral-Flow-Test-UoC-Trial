package uoc.ifm.dial.saamd.LFDApp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import uoc.ifm.dial.saamd.LFDApp.model.Post
import uoc.ifm.dial.saamd.LFDApp.repository.Repository
import kotlinx.coroutines.launch
import retrofit2.Response


class MainViewModel(private val repository: Repository): ViewModel() {

    val myResponse: MutableLiveData<Response<Post>> = MutableLiveData()

    fun getPost(){
        viewModelScope.launch {
            val response :Response<Post> = repository.getPost()
            myResponse.value = response
        }
    }

    fun pushPost(post: Post){
        viewModelScope.launch {
            val response :Response<Post> = repository.pushPost(post)
            myResponse.value = response
        }
    }
}
package com.ams.linkme.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class ProfileViewModel : ViewModel() {
    val interests = MutableLiveData<List<String>>()
    val name = MutableLiveData<String>()
    val age = MutableLiveData<Int>()
    val gender = MutableLiveData<String>()

    fun submit() {
        val selectedInterests = interests.value!!
        val enteredName = name.value
        val enteredAge = age.value
        val enteredGender = gender.value

        // 将收集到的参数提交信息
        // 在这里进行提交逻辑的处理，例如发送网络请求或保存到数据库等

        // 打印提交的参数
        println("Selected Interests: $selectedInterests")
        println("Name: $enteredName")
        println("Age: $enteredAge")
        println("Gender: $enteredGender")
    }
}





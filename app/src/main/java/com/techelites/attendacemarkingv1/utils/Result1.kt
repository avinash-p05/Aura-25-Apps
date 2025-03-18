package com.techelites.attendacemarkingv1.utils

sealed class Result1<out T> {
    data class Success<out T>(val data: T) : Result1<T>()
    data class Error(val exception: Exception) : Result1<Nothing>()
    object Loading : Result1<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    fun isLoading(): Boolean = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun exceptionOrNull(): Exception? = when (this) {
        is Error -> exception
        else -> null
    }
}
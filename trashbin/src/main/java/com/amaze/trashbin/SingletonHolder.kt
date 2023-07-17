package com.amaze.trashbin

open class SingletonSingleArgHolder<out T, in A, in B, in C>(private val constructor: (A, B, C) -> T) {

    @Volatile
    private var instance: T? = null

    fun getInstance(arg1: A, arg2: B, arg3: C): T =
        instance ?: synchronized(this) {
            instance ?: constructor(arg1, arg2, arg3).also { instance = it }
        }
}
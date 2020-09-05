package com.epicqueststudios.beamtaxi.data.common

open class BaseEvent<out T>(private val content: T) {

    private var handled = false
        private set

    fun getContentIfNotHandled(): T? {
        synchronized(this) {
            return if (handled) {
                null
            } else {
                handled = true
                content
            }
        }
    }

    fun peekContent(): T = content
}
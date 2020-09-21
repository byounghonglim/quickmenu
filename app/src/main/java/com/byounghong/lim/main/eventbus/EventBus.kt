package com.byounghong.lim.main.eventbus

import com.jakewharton.rxrelay2.PublishRelay

/**
 *
 * @author byounghong
 * @since 08/10/2018 2:47 PM
 **/
class EventBus<Any>() {
    val bus = PublishRelay.create<Any>()

    fun <E : Any> post(event : E) = bus.accept(event)
    fun bus() = bus
}
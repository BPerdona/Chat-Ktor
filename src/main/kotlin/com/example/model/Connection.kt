package com.example.model

import io.ktor.websocket.*
import java.util.concurrent.atomic.AtomicInteger

class Connection(val session: DefaultWebSocketSession) {

    companion object{
        val lastId = AtomicInteger(0)
    }
    var name = "user${lastId.getAndIncrement()}"
        private set

    fun setUserName(newName: String){
        name = newName
    }
}
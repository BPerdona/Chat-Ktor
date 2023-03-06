package com.example.plugins

import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*

fun Application.configureRouting() {
    routing {
        route("/chat"){
            get {
                call.respond(message = "All User on the chat -> ${WebSocketsConnections.chat.count()}", status= HttpStatusCode.OK)
            }
        }
    }
}

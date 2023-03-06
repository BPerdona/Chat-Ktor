package com.example.plugins

import com.example.model.Connection
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

object WebSocketsConnections{
    val chat = Collections.synchronizedSet<Connection?>(LinkedHashSet())
}

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(60)
        timeout = Duration.ofSeconds(60)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {

        webSocket("/chat") {
            println("Adding User!")
            val thisConnection = Connection(this)
            WebSocketsConnections.chat += thisConnection
            try{
                send("You are connected! There are ${WebSocketsConnections.chat.count()} users here.")
                send("Use /commands to see all the commands")

                for (frame in incoming){
                    frame as? Frame.Text ?: continue
                    val incomingMessage = frame.readText()

                    val type = if(frame.hasCommands())
                        handleNewMessage(incomingMessage, thisConnection)
                    else
                        MessageType.CommonMessage(incomingMessage)

                    when(type){
                        is MessageType.Info -> send(type.message)
                        is MessageType.CommonMessage -> {
                            WebSocketsConnections.chat.forEach {
                                it.session.send("[${thisConnection.name}]: ${type.message}")
                            }
                        }
                        is MessageType.Whisper -> {
                            val receiver = WebSocketsConnections.chat.find {
                                it.name == type.receiver
                            }
                            receiver?.session?.send("[Whisper - ${thisConnection.name}]: ${type.message}")
                            send("[Whisper - ${thisConnection.name}]: ${type.message}")
                        }
                    }
                }
            }catch (e: Exception){
                println(e.localizedMessage)
            }finally {
                println("Removing $thisConnection!")
                WebSocketsConnections.chat -= WebSocketsConnections.chat
            }
        }
    }
}

private fun handleNewMessage(message: String, connection: Connection): MessageType{
    return when(message.substringBefore(" ").substring(1)){
        "commands" ->MessageType.Info(
            "All Commands:\n" +
            "/commands -> Return All Commands\n" +
            "/changeName yourname -> Change you chat name\n" +
            "/whisper username message -> Send a message to that specific user")
        "changeName" ->{
            val paramsList = message.split(" ")
            if (paramsList.size<=1){
                MessageType.Info("Error!")
            }
            else{
                connection.setUserName(paramsList[1])
                MessageType.Info("Username changed to ${paramsList[1]}")
            }
        }
        "whisper" ->{
            val paramsList = message.split(" ")
            if(paramsList.size<=2){
                MessageType.Info("Error! No Message or User found")
            }else{
                val user = paramsList[1]
                val receiverMessage = message.substringAfter(paramsList[1])
                MessageType.Whisper(receiverMessage, user)
            }
        }
        else -> MessageType.Info("Unknown Command")
    }
}

fun Frame.Text.hasCommands(): Boolean{
    if(readText().startsWith("/"))
        return true
    return false
}

sealed class MessageType{
    class Whisper(val message: String, val receiver: String):MessageType()
    class Info(val message: String): MessageType()
    class CommonMessage(val message:String): MessageType()
}

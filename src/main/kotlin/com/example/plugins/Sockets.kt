package com.example.plugins

import com.example.Connection
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*
import kotlin.collections.LinkedHashSet

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(60)
        timeout = Duration.ofSeconds(60)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/chat") {
            println("Adding User!")
            val thisConnection = Connection(this)
            connections += thisConnection
            try{
                send("You are connected! There are ${connections.count()} users here.")
                send("Use /commands to see all the commands")

                for (frame in incoming){
                    frame as? Frame.Text ?: continue
                    if (frame.hasCommands()){
                        send(handleCommand(frame.readText(), thisConnection))
                        continue
                    }
                    val receivedText = frame.readText()
                    val textWithUsername = "[${thisConnection.name}]: $receivedText"
                    connections.forEach{
                        it.session.send(textWithUsername)
                    }
                }
            }catch (e: Exception){
                println(e.localizedMessage)
            }finally {
                println("Removing $thisConnection!")
                connections -= connections
            }
        }
    }
}

private fun handleCommand(message: String, connection: Connection): String{
    val command = message.substringBefore(" ").substring(1)
    return when(command){
        "commands" ->{
            "All Commands:\n" +
                    "/commands -> Return All Commands\n" +
                    "/changeName yourname -> Change you chat name\n" +
                    "/whisper username message -> Send a message to that specific user"
        }
        "changeName" ->{
            val newName = message.split(" ")[1]
            connection.setUserName(newName)
            "Username changed to $newName"
        }
        else -> "Unknown command!"
    }
}

fun Frame.Text.hasCommands(): Boolean{
    if(readText().startsWith("/"))
        return true
    return false
}

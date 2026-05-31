package com.example.digitalpass

import android.content.Context
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import org.json.JSONObject

object SocketManager {
     lateinit var socket: Socket

    fun connect() {
        val options = IO.Options.builder()
            .setTransports(arrayOf(WebSocket.NAME)) // FORCE WEBSOCKET ONLY
            .setReconnection(true)
            .build()

        socket = IO.socket(RetrofitClient.BASE_URL, options)
        socket.connect()
        socket.on(Socket.EVENT_CONNECT) {
            joinRoom()
        }
        socket.on(Manager.EVENT_RECONNECT){
            joinRoom()
        }

        socket.on("visitorUpdate"){args ->
            var data=args[0] as JSONObject
            if(data.getString("operation")=="exit"||data.getString("operation")=="meet") LoginUserDataHolder.updateVisitorStatus(data)
            else LoginUserDataHolder.updatedVisitor(data.get("operation").toString(),data.getString("visitorId"))

        }
        socket.on("visitorInsert"){args ->
            var data=args[0] as JSONObject
            LoginUserDataHolder.updatedVisitor("insert",data.getString("visitorId"))
        }

        socket.on("gatePassInsert"){args ->
            var data=args[0] as JSONObject
            LoginUserDataHolder.insertNewGatePass(data.getString("gatePassId"))
            }
        socket.on("gatePassUpdate") { args ->
            var data = args[0] as JSONObject
            LoginUserDataHolder.updateGatePass("updateRemark",data)
        }
        socket.on("gatePassStatusUpdate"){args ->
            var data=args[0] as JSONObject
            LoginUserDataHolder.updateGatePass("updateStatus",data)
        }
    }

    fun disconnect(){
        if(::socket.isInitialized)socket.disconnect()
    }

    private fun joinRoom(){
        var data= JSONObject()
        data.put("token",LoginUserDataHolder.token)
        socket.emit("joinRoom",LoginUserDataHolder.token)
    }


}
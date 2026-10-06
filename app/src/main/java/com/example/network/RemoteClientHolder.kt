package com.example.network

import android.content.Context
import com.example.data.ClientSettings

object RemoteClientHolder {
    @Volatile
    private var clientInstance: RemoteNodeClient? = null

    fun getClient(context: Context): RemoteNodeClient {
        val current = clientInstance
        if (current != null) return current

        synchronized(this) {
            val existing = clientInstance
            if (existing != null) return existing

            val settings = ClientSettings(context.applicationContext)
            val newClient = RemoteNodeClient(settings.deviceAlias, context.applicationContext)
            clientInstance = newClient
            return newClient
        }
    }

    fun connect(context: Context) {
        val settings = ClientSettings(context.applicationContext)
        val client = getClient(context)
        client.updateSenderName(settings.deviceAlias)
        client.connect(
            rawHost = settings.hostIp,
            defaultPort = settings.hostPort,
            room = settings.satelliteRoom
        )
        if (settings.nodeMeshSyncEnabled) {
            client.startMeshServices()
        }
    }

    fun disconnect() {
        clientInstance?.disconnect()
    }
}

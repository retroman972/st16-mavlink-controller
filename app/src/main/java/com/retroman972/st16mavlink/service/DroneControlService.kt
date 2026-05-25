package com.retroman972.st16mavlink.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.retroman972.st16mavlink.hardware.ST16StickReader
import com.retroman972.st16mavlink.mavlink.MAVLinkConnection
import kotlinx.coroutines.*
import timber.log.Timber

class DroneControlService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    private var mavlinkConnection: MAVLinkConnection? = null
    private var stickReader: ST16StickReader? = null
    private var controlLoopJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): DroneControlService = this@DroneControlService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            "CONNECT" -> {
                val ipAddress = intent.getStringExtra("ip_address") ?: "127.0.0.1"
                val port = intent.getIntExtra("port", 14550)
                connectToDrone(ipAddress, port)
            }
            "DISCONNECT" -> {
                disconnectFromDrone()
            }
        }

        return START_STICKY
    }

    private fun connectToDrone(ipAddress: String, port: Int) {
        serviceScope.launch {
            try {
                Timber.d("Initialisation de la connexion MAVLink: $ipAddress:$port")

                // Initialize MAVLink connection
                mavlinkConnection = MAVLinkConnection(ipAddress, port)
                mavlinkConnection?.connect()

                // Initialize stick reader
                stickReader = ST16StickReader(applicationContext)
                stickReader?.initialize()

                // Start control loop
                startControlLoop()

                Timber.i("Connexion établie avec succès")
            } catch (e: Exception) {
                Timber.e(e, "Erreur lors de la connexion au drone")
                disconnectFromDrone()
            }
        }
    }

    private fun startControlLoop() {
        controlLoopJob?.cancel()
        controlLoopJob = serviceScope.launch {
            while (isActive) {
                try {
                    val stickData = stickReader?.readSticks()
                    if (stickData != null) {
                        mavlinkConnection?.sendManualControl(stickData)
                    }
                    delay(50) // 20 Hz control rate
                } catch (e: Exception) {
                    Timber.e(e, "Erreur dans la boucle de contrôle")
                }
            }
        }
    }

    private fun disconnectFromDrone() {
        controlLoopJob?.cancel()
        stickReader?.cleanup()
        mavlinkConnection?.disconnect()
        
        mavlinkConnection = null
        stickReader = null
        
        Timber.i("Déconnexion complète du drone")
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromDrone()
        serviceScope.cancel()
    }
}

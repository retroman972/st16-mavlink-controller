package com.retroman972.st16mavlink

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.retroman972.st16mavlink.service.DroneControlService
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var ipAddressInput: EditText
    private lateinit var portInput: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var statusText: TextView
    private lateinit var telemetryText: TextView

    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        initializeViews()
        setupListeners()

        // Default values
        ipAddressInput.setText("127.0.0.1")
        portInput.setText("14550")

        updateStatus("Prêt à se connecter")
    }

    private fun initializeViews() {
        ipAddressInput = findViewById(R.id.ipAddressInput)
        portInput = findViewById(R.id.portInput)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        statusText = findViewById(R.id.statusText)
        telemetryText = findViewById(R.id.telemetryText)

        disconnectButton.isEnabled = false
    }

    private fun setupListeners() {
        connectButton.setOnClickListener {
            val ipAddress = ipAddressInput.text.toString()
            val port = portInput.text.toString().toIntOrNull()

            when {
                ipAddress.isEmpty() -> showError("Entrez une adresse IP")
                port == null || port <= 0 -> showError("Entrez un port valide")
                else -> connectToDrone(ipAddress, port)
            }
        }

        disconnectButton.setOnClickListener {
            disconnectFromDrone()
        }
    }

    private fun connectToDrone(ipAddress: String, port: Int) {
        Timber.d("Connexion au drone: $ipAddress:$port")
        updateStatus("Connexion en cours...")

        val intent = Intent(this, DroneControlService::class.java).apply {
            action = "CONNECT"
            putExtra("ip_address", ipAddress)
            putExtra("port", port)
        }

        startService(intent)

        // Update UI
        connectButton.isEnabled = false
        disconnectButton.isEnabled = true
        ipAddressInput.isEnabled = false
        portInput.isEnabled = false
        isConnected = true

        updateStatus("Connecté à $ipAddress:$port")
        Toast.makeText(
            this,
            "Connexion établie avec le drone",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun disconnectFromDrone() {
        Timber.d("Déconnexion du drone")
        updateStatus("Déconnexion...")

        val intent = Intent(this, DroneControlService::class.java).apply {
            action = "DISCONNECT"
        }

        stopService(intent)

        // Update UI
        connectButton.isEnabled = true
        disconnectButton.isEnabled = false
        ipAddressInput.isEnabled = true
        portInput.isEnabled = true
        isConnected = false

        updateStatus("Déconnecté")
        Toast.makeText(
            this,
            "Déconnexion du drone",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateStatus(message: String) {
        statusText.text = message
        Timber.i(message)
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Timber.e(message)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isConnected) {
            disconnectFromDrone()
        }
    }
}

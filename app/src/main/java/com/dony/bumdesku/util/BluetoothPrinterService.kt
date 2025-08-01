package com.dony.bumdesku.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterService(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) return emptyList()
        return bluetoothAdapter!!.bondedDevices.toList()
    }

    suspend fun printText(device: BluetoothDevice, text: String) {
        if (!hasBluetoothPermissions()) {
            throw SecurityException("Bluetooth permission not granted.")
        }
        withContext(Dispatchers.IO) {
            var outputStream: OutputStream? = null
            try {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SerialPortService ID
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                socket.connect()
                outputStream = socket.outputStream

                // Menggunakan charset yang umum untuk thermal printer
                outputStream.write(text.toByteArray(charset("CP437")))
                outputStream.flush()
                Log.d("PrinterService", "Print successful")
            } catch (e: IOException) {
                Log.e("PrinterService", "Error printing", e)
                throw e
            } finally {
                try {
                    outputStream?.close()
                } catch (e: IOException) {
                    Log.e("PrinterService", "Error closing stream", e)
                }
            }
        }
    }
}
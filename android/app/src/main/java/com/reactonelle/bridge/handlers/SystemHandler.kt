package com.reactonelle.bridge.handlers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject

/**
 * Handler para obter versão do app
 * Payload: -
 * Retorna: { version: string, build: number, name: string }
 */
class AppVersionHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            onSuccess(JSONObject().apply {
                put("version", packageInfo.versionName ?: "1.0.0")
                put("build", versionCode)
                put("name", context.applicationInfo.loadLabel(context.packageManager).toString())
            })
        } catch (e: Exception) {
            onError(e.message ?: "Failed to get app version")
        }
    }
}

/**
 * Handler para obter status da bateria
 * Payload: -
 * Retorna: { level: number (0-100), charging: boolean, state: string }
 */
class BatteryStatusHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val batteryIntent = context.registerReceiver(
                null, 
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            
            val batteryPct = if (level >= 0 && scale > 0) {
                (level * 100 / scale)
            } else {
                -1
            }
            
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            
            val stateStr = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
                else -> "unknown"
            }
            
            onSuccess(JSONObject().apply {
                put("level", batteryPct)
                put("charging", charging)
                put("state", stateStr)
            })
        } catch (e: Exception) {
            onError(e.message ?: "Failed to get battery status")
        }
    }
}

/**
 * Handler para obter status da rede
 * Payload: -
 * Retorna: { connected: boolean, type: string ('wifi'|'cellular'|'ethernet'|'none') }
 */
class NetworkStatusHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            val connected = capabilities != null
            val type = when {
                capabilities == null -> "none"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                else -> "other"
            }
            
            onSuccess(JSONObject().apply {
                put("connected", connected)
                put("type", type)
            })
        } catch (e: Exception) {
            onError(e.message ?: "Failed to get network status")
        }
    }
}

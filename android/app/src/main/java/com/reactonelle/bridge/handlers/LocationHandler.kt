package com.reactonelle.bridge.handlers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONObject

/**
 * Handler para obter localização atual
 * Payload: { accuracy?: 'high'|'balanced'|'low' }
 * Retorna: { lat: number, lng: number, accuracy: number, timestamp: number }
 * 
 * Requer: ACCESS_FINE_LOCATION ou ACCESS_COARSE_LOCATION
 */
class LocationCurrentHandler : BridgeHandler {
    
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val accuracyLevel = payload.optString("accuracy", "high")
        
        // Verifica permissão
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasFineLocation && !hasCoarseLocation) {
            onError("Location permission not granted")
            return
        }
        
        val priority = when (accuracyLevel) {
            "low" -> Priority.PRIORITY_LOW_POWER
            "balanced" -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            else -> Priority.PRIORITY_HIGH_ACCURACY
        }
        
        try {
            val fusedLocationClient: FusedLocationProviderClient = 
                LocationServices.getFusedLocationProviderClient(context)
            
            val cancellationToken = CancellationTokenSource()
            
            fusedLocationClient.getCurrentLocation(priority, cancellationToken.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        onSuccess(JSONObject().apply {
                            put("lat", location.latitude)
                            put("lng", location.longitude)
                            put("accuracy", location.accuracy)
                            put("altitude", location.altitude)
                            put("timestamp", location.time)
                        })
                    } else {
                        // Tenta última localização conhecida
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLocation: Location? ->
                                if (lastLocation != null) {
                                    onSuccess(JSONObject().apply {
                                        put("lat", lastLocation.latitude)
                                        put("lng", lastLocation.longitude)
                                        put("accuracy", lastLocation.accuracy)
                                        put("timestamp", lastLocation.time)
                                        put("cached", true)
                                    })
                                } else {
                                    onError("Unable to get location")
                                }
                            }
                            .addOnFailureListener { e ->
                                onError(e.message ?: "Failed to get location")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    onError(e.message ?: "Failed to get location")
                }
                
        } catch (e: SecurityException) {
            onError("Location permission denied")
        } catch (e: Exception) {
            onError(e.message ?: "Location error")
        }
    }
}

package com.reactonelle.bridge.handlers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mapeamento de nomes de permissão para constantes Android
 */
object PermissionMap {
    val permissions = mapOf(
        "camera" to Manifest.permission.CAMERA,
        "location" to Manifest.permission.ACCESS_FINE_LOCATION,
        "locationCoarse" to Manifest.permission.ACCESS_COARSE_LOCATION,
        "microphone" to Manifest.permission.RECORD_AUDIO,
        "contacts" to Manifest.permission.READ_CONTACTS,
        "calendar" to Manifest.permission.READ_CALENDAR,
        "storage" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        },
        "phone" to Manifest.permission.CALL_PHONE,
        "sms" to Manifest.permission.SEND_SMS,
        "notifications" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null // Não precisa em versões antigas
        }
    )
    
    fun get(name: String): String? = permissions[name]
    
    fun getAll(names: List<String>): List<String> = 
        names.mapNotNull { permissions[it] }
}

/**
 * Handler para verificar status de uma permissão
 * Payload: { permission: 'camera'|'location'|'microphone'|... }
 * Retorna: { granted: boolean, canAsk: boolean }
 */
class PermissionCheckHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val permissionName = payload.optString("permission", "")
        
        if (permissionName.isEmpty()) {
            onError("Missing 'permission' field")
            return
        }
        
        val androidPermission = PermissionMap.get(permissionName)
        
        if (androidPermission == null) {
            // Permissão não precisa ser solicitada (ex: notifications em Android < 13)
            onSuccess(JSONObject().apply {
                put("granted", true)
                put("canAsk", false)
                put("status", "granted")
            })
            return
        }
        
        val granted = ContextCompat.checkSelfPermission(context, androidPermission) == 
            PackageManager.PERMISSION_GRANTED
            
        val canAsk = if (context is Activity) {
            !ActivityCompat.shouldShowRequestPermissionRationale(context, androidPermission) || !granted
        } else {
            true
        }
        
        val status = when {
            granted -> "granted"
            canAsk -> "denied"
            else -> "blocked" // Usuário marcou "Não perguntar novamente"
        }
        
        onSuccess(JSONObject().apply {
            put("granted", granted)
            put("canAsk", canAsk)
            put("status", status)
        })
    }
}

/**
 * Handler para solicitar uma permissão
 * Payload: { permission: 'camera'|'location'|'microphone'|... }
 * Retorna: { granted: boolean }
 */
class PermissionRequestHandler : BridgeHandler {
    
    companion object {
        private const val REQUEST_CODE = 9001
        private var pendingCallback: ((JSONObject?) -> Unit)? = null
        private var pendingErrorCallback: ((String) -> Unit)? = null
        private var pendingPermission: String? = null
        
        /**
         * Deve ser chamado pela Activity no onRequestPermissionsResult
         */
        fun handlePermissionResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ): Boolean {
            if (requestCode != REQUEST_CODE) return false
            
            val granted = grantResults.isNotEmpty() && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            
            pendingCallback?.invoke(JSONObject().apply {
                put("granted", granted)
                put("permission", pendingPermission)
            })
            
            cleanup()
            return true
        }
        
        private fun cleanup() {
            pendingCallback = null
            pendingErrorCallback = null
            pendingPermission = null
        }
    }
    
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val permissionName = payload.optString("permission", "")
        
        if (permissionName.isEmpty()) {
            onError("Missing 'permission' field")
            return
        }
        
        val androidPermission = PermissionMap.get(permissionName)
        
        if (androidPermission == null) {
            // Permissão não precisa ser solicitada
            onSuccess(JSONObject().apply {
                put("granted", true)
                put("permission", permissionName)
            })
            return
        }
        
        // Verifica se já tem a permissão
        if (ContextCompat.checkSelfPermission(context, androidPermission) == 
            PackageManager.PERMISSION_GRANTED) {
            onSuccess(JSONObject().apply {
                put("granted", true)
                put("permission", permissionName)
            })
            return
        }
        
        if (context !is Activity) {
            onError("Context must be an Activity to request permissions")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            pendingCallback = onSuccess
            pendingErrorCallback = onError
            pendingPermission = permissionName
            
            ActivityCompat.requestPermissions(
                context,
                arrayOf(androidPermission),
                REQUEST_CODE
            )
        }
    }
}

/**
 * Handler para solicitar múltiplas permissões de uma vez
 * Payload: { permissions: ['camera', 'location', 'microphone'] }
 * Retorna: { results: { camera: true, location: false, ... } }
 */
class PermissionRequestMultipleHandler : BridgeHandler {
    
    companion object {
        private const val REQUEST_CODE = 9002
        private var pendingCallback: ((JSONObject?) -> Unit)? = null
        private var pendingPermissionNames: List<String> = emptyList()
        private var pendingAndroidPermissions: List<String> = emptyList()
        
        fun handlePermissionResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ): Boolean {
            if (requestCode != REQUEST_CODE) return false
            
            val results = JSONObject()
            
            permissions.forEachIndexed { index, permission ->
                val granted = grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED
                
                // Encontra o nome amigável
                val friendlyName = PermissionMap.permissions.entries
                    .find { it.value == permission }?.key ?: permission
                    
                results.put(friendlyName, granted)
            }
            
            pendingCallback?.invoke(JSONObject().put("results", results))
            cleanup()
            return true
        }
        
        private fun cleanup() {
            pendingCallback = null
            pendingPermissionNames = emptyList()
            pendingAndroidPermissions = emptyList()
        }
    }
    
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val permissionsArray = payload.optJSONArray("permissions")
        
        if (permissionsArray == null || permissionsArray.length() == 0) {
            onError("Missing 'permissions' array")
            return
        }
        
        val permissionNames = mutableListOf<String>()
        for (i in 0 until permissionsArray.length()) {
            permissionNames.add(permissionsArray.getString(i))
        }
        
        val androidPermissions = PermissionMap.getAll(permissionNames)
        
        if (androidPermissions.isEmpty()) {
            // Todas as permissões já são concedidas automaticamente
            val results = JSONObject()
            permissionNames.forEach { results.put(it, true) }
            onSuccess(JSONObject().put("results", results))
            return
        }
        
        // Verifica quais já estão concedidas
        val needToRequest = androidPermissions.filter { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
        
        if (needToRequest.isEmpty()) {
            // Todas já concedidas
            val results = JSONObject()
            permissionNames.forEach { results.put(it, true) }
            onSuccess(JSONObject().put("results", results))
            return
        }
        
        if (context !is Activity) {
            onError("Context must be an Activity")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            pendingCallback = onSuccess
            pendingPermissionNames = permissionNames
            pendingAndroidPermissions = needToRequest
            
            ActivityCompat.requestPermissions(
                context,
                needToRequest.toTypedArray(),
                REQUEST_CODE
            )
        }
    }
}

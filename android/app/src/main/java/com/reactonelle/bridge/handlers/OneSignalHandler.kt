package com.reactonelle.bridge.handlers

import android.content.Context
import android.util.Log
import com.onesignal.OneSignal
import com.reactonelle.bridge.BridgeHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Handlers para operações do OneSignal
 */

private const val TAG = "OneSignalHandler"

/**
 * Handler para login com External ID
 * Payload: { externalId: string }
 */
class OneSignalLoginHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val externalId = payload.optString("externalId", "")
        
        if (externalId.isEmpty()) {
            onError("externalId is required")
            return
        }
        
        try {
            OneSignal.login(externalId)
            Log.d(TAG, "User logged in with externalId: $externalId")
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to login")
        }
    }
}

/**
 * Handler para logout do usuário
 */
class OneSignalLogoutHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            OneSignal.logout()
            Log.d(TAG, "User logged out")
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to logout")
        }
    }
}

/**
 * Handler para definir uma tag
 * Payload: { key: string, value: string }
 */
class OneSignalSetTagHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val key = payload.optString("key", "")
        val value = payload.optString("value", "")
        
        if (key.isEmpty()) {
            onError("key is required")
            return
        }
        
        try {
            OneSignal.User.addTag(key, value)
            Log.d(TAG, "Tag set: $key = $value")
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to set tag")
        }
    }
}

/**
 * Handler para definir múltiplas tags
 * Payload: { tags: { key1: value1, key2: value2 } }
 */
class OneSignalSetTagsHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val tagsObj = payload.optJSONObject("tags")
        
        if (tagsObj == null) {
            onError("tags object is required")
            return
        }
        
        try {
            val tagsMap = mutableMapOf<String, String>()
            val keys = tagsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                tagsMap[key] = tagsObj.getString(key)
            }
            
            OneSignal.User.addTags(tagsMap)
            Log.d(TAG, "Tags set: $tagsMap")
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to set tags")
        }
    }
}

/**
 * Handler para remover uma tag
 * Payload: { key: string }
 */
class OneSignalDeleteTagHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val key = payload.optString("key", "")
        
        if (key.isEmpty()) {
            onError("key is required")
            return
        }
        
        try {
            OneSignal.User.removeTag(key)
            Log.d(TAG, "Tag deleted: $key")
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to delete tag")
        }
    }
}

/**
 * Handler para obter todas as tags (não suportado diretamente no SDK v5)
 * Retorna: { tags: object }
 */
class OneSignalGetTagsHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val tags = OneSignal.User.getTags()
            val result = JSONObject()
            val tagsJson = JSONObject()
            
            for ((key, value) in tags) {
                tagsJson.put(key, value)
            }
            
            result.put("tags", tagsJson)
            onSuccess(result)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to get tags")
        }
    }
}

/**
 * Handler para solicitar permissão de notificação
 * Retorna: { granted: boolean }
 */
class OneSignalRequestPermissionHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val granted = OneSignal.Notifications.requestPermission(true)
                val result = JSONObject().put("granted", granted)
                Log.d(TAG, "Permission request result: $granted")
                onSuccess(result)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to request permission")
            }
        }
    }
}

/**
 * Handler para verificar status da permissão
 * Retorna: { permission: boolean }
 */
class OneSignalGetPermissionStatusHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val hasPermission = OneSignal.Notifications.permission
            val result = JSONObject().put("permission", hasPermission)
            onSuccess(result)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to get permission status")
        }
    }
}

/**
 * Handler para obter Subscription ID (Push Token ID)
 * Retorna: { subscriptionId: string | null, optedIn: boolean }
 */
class OneSignalGetSubscriptionIdHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val subscriptionId = OneSignal.User.pushSubscription.id
            val optedIn = OneSignal.User.pushSubscription.optedIn
            
            val result = JSONObject()
            result.put("subscriptionId", subscriptionId ?: JSONObject.NULL)
            result.put("optedIn", optedIn)
            
            onSuccess(result)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to get subscription id")
        }
    }
}

/**
 * Handler para opt-in de notificações
 */
class OneSignalOptInHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            OneSignal.User.pushSubscription.optIn()
            Log.d(TAG, "User opted in to push notifications")
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to opt in")
        }
    }
}

/**
 * Handler para opt-out de notificações
 */
class OneSignalOptOutHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            OneSignal.User.pushSubscription.optOut()
            Log.d(TAG, "User opted out of push notifications")
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to opt out")
        }
    }
}

/**
 * Handler para adicionar email ao usuário
 * Payload: { email: string }
 */
class OneSignalAddEmailHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val email = payload.optString("email", "")
        
        if (email.isEmpty()) {
            onError("email is required")
            return
        }
        
        try {
            OneSignal.User.addEmail(email)
            Log.d(TAG, "Email added: $email")
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to add email")
        }
    }
}

/**
 * Handler para remover email do usuário
 * Payload: { email: string }
 */
class OneSignalRemoveEmailHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val email = payload.optString("email", "")
        
        if (email.isEmpty()) {
            onError("email is required")
            return
        }
        
        try {
            OneSignal.User.removeEmail(email)
            Log.d(TAG, "Email removed: $email")
            onSuccess(null)
        } catch (e: Exception) {
            onError(e.message ?: "Failed to remove email")
        }
    }
}

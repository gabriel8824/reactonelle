package com.reactonelle.bridge.handlers

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handler para action sheet (bottom sheet com opções)
 * Payload: { title?: string, options: [{ text: string, style?: 'default'|'destructive'|'cancel' }] }
 * Retorna: { index: number } (índice da opção selecionada, -1 se cancelou)
 */
class ActionSheetHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val title = payload.optString("title", "")
        val optionsArray = payload.optJSONArray("options")
        
        if (optionsArray == null || optionsArray.length() == 0) {
            onError("Missing 'options' array")
            return
        }
        
        if (context !is Activity) {
            onError("Context must be an Activity")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                val options = mutableListOf<String>()
                var cancelIndex = -1
                
                for (i in 0 until optionsArray.length()) {
                    val option = optionsArray.optJSONObject(i)
                    val text = option?.optString("text", "Option $i") ?: "Option $i"
                    val style = option?.optString("style", "default") ?: "default"
                    
                    options.add(text)
                    
                    if (style == "cancel") {
                        cancelIndex = i
                    }
                }
                
                val optionItems = options.toTypedArray()
                
                MaterialAlertDialogBuilder(context)
                    .apply {
                        if (title.isNotEmpty()) {
                            setTitle(title)
                        }
                    }
                    .setItems(optionItems) { dialog, which ->
                        dialog.dismiss()
                        onSuccess(JSONObject().put("index", which))
                    }
                    .setOnCancelListener {
                        onSuccess(JSONObject().put("index", -1))
                    }
                    .show()
                    
            } catch (e: Exception) {
                onError(e.message ?: "Failed to show action sheet")
            }
        }
    }
}

/**
 * Handler para date picker
 * Payload: { 
 *   mode: 'date'|'time'|'datetime', 
 *   date?: ISO string (data inicial), 
 *   min?: ISO string, 
 *   max?: ISO string 
 * }
 * Retorna: { date: ISO string } ou { cancelled: true }
 */
class DatePickerHandler : BridgeHandler {
    
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        val mode = payload.optString("mode", "date")
        val initialDateStr = payload.optString("date", "")
        val minDateStr = payload.optString("min", "")
        val maxDateStr = payload.optString("max", "")
        
        if (context !is Activity) {
            onError("Context must be an Activity")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                val calendar = Calendar.getInstance()
                
                // Parse data inicial se fornecida
                if (initialDateStr.isNotEmpty()) {
                    try {
                        val date = isoFormat.parse(initialDateStr)
                        if (date != null) {
                            calendar.time = date
                        }
                    } catch (e: Exception) {
                        // Ignora erro de parse
                    }
                }
                
                when (mode) {
                    "time" -> showTimePicker(context, calendar, onSuccess)
                    "datetime" -> showDateTimePicker(context, calendar, minDateStr, maxDateStr, onSuccess)
                    else -> showDatePicker(context, calendar, minDateStr, maxDateStr, onSuccess)
                }
                
            } catch (e: Exception) {
                onError(e.message ?: "Failed to show date picker")
            }
        }
    }
    
    private fun showDatePicker(
        context: Activity,
        calendar: Calendar,
        minDateStr: String,
        maxDateStr: String,
        onSuccess: (JSONObject?) -> Unit
    ) {
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onSuccess(JSONObject().put("date", isoFormat.format(calendar.time)))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Define min/max dates
        if (minDateStr.isNotEmpty()) {
            try {
                isoFormat.parse(minDateStr)?.let {
                    dialog.datePicker.minDate = it.time
                }
            } catch (e: Exception) { }
        }
        
        if (maxDateStr.isNotEmpty()) {
            try {
                isoFormat.parse(maxDateStr)?.let {
                    dialog.datePicker.maxDate = it.time
                }
            } catch (e: Exception) { }
        }
        
        dialog.setOnCancelListener {
            onSuccess(JSONObject().put("cancelled", true))
        }
        
        dialog.show()
    }
    
    private fun showTimePicker(
        context: Activity,
        calendar: Calendar,
        onSuccess: (JSONObject?) -> Unit
    ) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                onSuccess(JSONObject().put("date", isoFormat.format(calendar.time)))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24h format
        ).apply {
            setOnCancelListener {
                onSuccess(JSONObject().put("cancelled", true))
            }
            show()
        }
    }
    
    private fun showDateTimePicker(
        context: Activity,
        calendar: Calendar,
        minDateStr: String,
        maxDateStr: String,
        onSuccess: (JSONObject?) -> Unit
    ) {
        // Primeiro mostra date picker
        val dateDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                
                // Depois mostra time picker
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onSuccess(JSONObject().put("date", isoFormat.format(calendar.time)))
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).apply {
                    setOnCancelListener {
                        onSuccess(JSONObject().put("cancelled", true))
                    }
                    show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        if (minDateStr.isNotEmpty()) {
            try {
                isoFormat.parse(minDateStr)?.let {
                    dateDialog.datePicker.minDate = it.time
                }
            } catch (e: Exception) { }
        }
        
        if (maxDateStr.isNotEmpty()) {
            try {
                isoFormat.parse(maxDateStr)?.let {
                    dateDialog.datePicker.maxDate = it.time
                }
            } catch (e: Exception) { }
        }
        
        dateDialog.setOnCancelListener {
            onSuccess(JSONObject().put("cancelled", true))
        }
        
        dateDialog.show()
    }
}

package com.reactonelle.bridge.handlers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.reactonelle.bridge.BridgeHandler
import org.json.JSONArray
import org.json.JSONObject

const val REQUEST_PICK_CONTACT = 1003

/**
 * Handler para escolher um contato da lista nativa
 */
class ContactsPickHandler : BridgeHandler {
    
    companion object {
        private var pendingCallback: ((JSONObject?) -> Unit)? = null
        private var pendingErrorCallback: ((String) -> Unit)? = null
        
        fun handleActivityResult(context: Context, data: Intent?) {
            if (data == null || data.data == null) {
                pendingErrorCallback?.invoke("No contact selected")
                cleanup()
                return
            }
            
            Thread {
                try {
                    val contactUri = data.data!!
                    val result = getContactDetails(context, contactUri)
                    
                    Handler(Looper.getMainLooper()).post {
                        pendingCallback?.invoke(result)
                        cleanup()
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        pendingErrorCallback?.invoke(e.message ?: "Failed to read contact")
                        cleanup()
                    }
                }
            }.start()
        }
        
        private fun getContactDetails(context: Context, contactUri: Uri): JSONObject {
            val cursor = context.contentResolver.query(contactUri, null, null, null, null)
            val contact = JSONObject()
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    
                    val id = if (idIndex != -1) it.getString(idIndex) else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) else ""
                    
                    contact.put("id", id)
                    contact.put("name", name)
                    
                    // Buscar telefones
                    contact.put("phones", getPhones(context, id))
                    // Buscar emails
                    contact.put("emails", getEmails(context, id))
                }
            }
            return contact
        }
        
        private fun getPhones(context: Context, contactId: String): JSONArray {
            val phones = JSONArray()
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )
            
            cursor?.use {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                
                while (it.moveToNext()) {
                    if (numberIndex != -1) {
                        val number = it.getString(numberIndex)
                        val type = if (typeIndex != -1) {
                            ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                                context.resources,
                                it.getInt(typeIndex),
                                ""
                            ).toString()
                        } else "mobile"
                        
                        phones.put(JSONObject().apply {
                            put("number", number)
                            put("type", type)
                        })
                    }
                }
            }
            return phones
        }
        
        private fun getEmails(context: Context, contactId: String): JSONArray {
            val emails = JSONArray()
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )
            
            cursor?.use {
                val addressIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
                val typeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
                
                while (it.moveToNext()) {
                    if (addressIndex != -1) {
                        val address = it.getString(addressIndex)
                        val type = if (typeIndex != -1) {
                            ContactsContract.CommonDataKinds.Email.getTypeLabel(
                                context.resources,
                                it.getInt(typeIndex),
                                ""
                            ).toString()
                        } else "home"
                        
                        emails.put(JSONObject().apply {
                            put("email", address)
                            put("type", type)
                        })
                    }
                }
            }
            return emails
        }
        
        private fun cleanup() {
            pendingCallback = null
            pendingErrorCallback = null
        }
    }

    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (context !is Activity) {
            onError("Context must be an Activity")
            return
        }
        
        Handler(Looper.getMainLooper()).post {
            try {
                pendingCallback = onSuccess
                pendingErrorCallback = onError
                
                val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                context.startActivityForResult(intent, REQUEST_PICK_CONTACT)
                
            } catch (e: Exception) {
                onError(e.message ?: "Failed to open contacts picker")
            }
        }
    }
}

/**
 * Handler para listar contatos (requer permissão READ_CONTACTS)
 * Payload: { filter: string, limit: number }
 */
class ContactsGetAllHandler : BridgeHandler {
    override fun handle(
        context: Context,
        payload: JSONObject,
        onSuccess: (JSONObject?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            onError("Read contacts permission not granted")
            return
        }
        
        Thread {
            try {
                val filter = payload.optString("filter", "")
                val limit = payload.optInt("limit", 100)
                
                val contacts = JSONArray()
                val projection = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                )
                
                val selection = if (filter.isNotEmpty()) {
                    "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?"
                } else null
                
                val selectionArgs = if (filter.isNotEmpty()) {
                    arrayOf("%$filter%")
                } else null
                
                val sortOrder = "${ContactsContract.Contacts.DISPLAY_NAME} ASC LIMIT $limit"
                
                val cursor = context.contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )
                
                cursor?.use {
                   val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                   val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                   val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                   
                   while (it.moveToNext()) {
                       if (idIndex != -1 && nameIndex != -1) {
                           val id = it.getString(idIndex)
                           val name = it.getString(nameIndex)
                           val hasPhone = it.getInt(hasPhoneIndex) > 0
                           
                           if (name != null) {
                               contacts.put(JSONObject().apply {
                                   put("id", id)
                                   put("name", name)
                                   put("hasPhone", hasPhone)
                               })
                           }
                       }
                   }
                }
                
                Handler(Looper.getMainLooper()).post {
                    onSuccess(JSONObject().put("contacts", contacts))
                }
                
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    onError(e.message ?: "Failed to list contacts")
                }
            }
        }.start()
    }
}

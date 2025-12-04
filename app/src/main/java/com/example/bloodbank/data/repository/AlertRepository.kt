package com.example.bloodbank.data.repository

import android.content.ContentValues
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bloodbank.data.entity.BloodAlert
import com.example.bloodbank.data.entity.AlertPriority
import com.example.bloodbank.data.entity.AlertType
import com.example.bloodbank.data.sqlite.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlertRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context.applicationContext)
    private val _alerts = MutableLiveData<List<BloodAlert>>(emptyList())
    val alerts: LiveData<List<BloodAlert>> = _alerts
    
    companion object {
        const val TABLE_ALERTS = "alerts"
        const val COL_ALERT_ID = "alert_id"
        const val COL_TITLE = "title"
        const val COL_MESSAGE = "message"
        const val COL_BLOOD_GROUP = "blood_group"
        const val COL_PRIORITY = "priority"
        const val COL_TYPE = "type"
        const val COL_LOCATION = "location"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_IS_READ = "is_read"
        const val COL_IS_ACTIVE = "is_active"
        const val COL_EXPIRY_TIME = "expiry_time"
        const val COL_REQUIRED_UNITS = "required_units"
        const val COL_CONTACT_INFO = "contact_info"
    }
    
    init {
        createAlertsTable()
        refresh()
    }
    
    private fun createAlertsTable() {
        val createTableQuery = """
            CREATE TABLE IF NOT EXISTS $TABLE_ALERTS (
                $COL_ALERT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_MESSAGE TEXT NOT NULL,
                $COL_BLOOD_GROUP TEXT NOT NULL,
                $COL_PRIORITY TEXT NOT NULL,
                $COL_TYPE TEXT NOT NULL,
                $COL_LOCATION TEXT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_IS_READ INTEGER DEFAULT 0,
                $COL_IS_ACTIVE INTEGER DEFAULT 1,
                $COL_EXPIRY_TIME INTEGER,
                $COL_REQUIRED_UNITS INTEGER DEFAULT 1,
                $COL_CONTACT_INFO TEXT
            )
        """.trimIndent()
        
        dbHelper.writableDatabase.execSQL(createTableQuery)
    }
    
    suspend fun addAlert(alert: BloodAlert) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_TITLE, alert.title)
            put(COL_MESSAGE, alert.message)
            put(COL_BLOOD_GROUP, alert.bloodGroup)
            put(COL_PRIORITY, alert.priority.name)
            put(COL_TYPE, alert.type.name)
            put(COL_LOCATION, alert.location)
            put(COL_TIMESTAMP, alert.timestamp)
            put(COL_IS_READ, if (alert.isRead) 1 else 0)
            put(COL_IS_ACTIVE, if (alert.isActive) 1 else 0)
            put(COL_EXPIRY_TIME, alert.expiryTime)
            put(COL_REQUIRED_UNITS, alert.requiredUnits)
            put(COL_CONTACT_INFO, alert.contactInfo)
        }
        
        dbHelper.writableDatabase.insert(TABLE_ALERTS, null, values)
        refresh()
    }
    
    suspend fun markAsRead(alertId: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_IS_READ, 1)
        }
        
        dbHelper.writableDatabase.update(
            TABLE_ALERTS,
            values,
            "$COL_ALERT_ID = ?",
            arrayOf(alertId.toString())
        )
        refresh()
    }
    
    suspend fun markAsInactive(alertId: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_IS_ACTIVE, 0)
        }
        
        dbHelper.writableDatabase.update(
            TABLE_ALERTS,
            values,
            "$COL_ALERT_ID = ?",
            arrayOf(alertId.toString())
        )
        refresh()
    }
    
    suspend fun getActiveAlerts(): List<BloodAlert> = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val list = mutableListOf<BloodAlert>()
        
        val query = """
            SELECT * FROM $TABLE_ALERTS 
            WHERE $COL_IS_ACTIVE = 1 
            AND ($COL_EXPIRY_TIME IS NULL OR $COL_EXPIRY_TIME > ?)
            ORDER BY $COL_PRIORITY DESC, $COL_TIMESTAMP DESC
        """.trimIndent()
        
        val cursor = dbHelper.readableDatabase.rawQuery(query, arrayOf(currentTime.toString()))
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToAlert(it))
            }
        }
        list
    }
    
    suspend fun getUnreadCount(): Int = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val query = """
            SELECT COUNT(*) FROM $TABLE_ALERTS 
            WHERE $COL_IS_READ = 0 
            AND $COL_IS_ACTIVE = 1 
            AND ($COL_EXPIRY_TIME IS NULL OR $COL_EXPIRY_TIME > ?)
        """.trimIndent()
        
        val cursor = dbHelper.readableDatabase.rawQuery(query, arrayOf(currentTime.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                it.getInt(0)
            } else {
                0
            }
        }
    }
    
    suspend fun cleanupExpiredAlerts() = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        dbHelper.writableDatabase.delete(
            TABLE_ALERTS,
            "$COL_EXPIRY_TIME IS NOT NULL AND $COL_EXPIRY_TIME < ?",
            arrayOf(currentTime.toString())
        )
        refresh()
    }
    
    private fun cursorToAlert(cursor: android.database.Cursor): BloodAlert {
        return BloodAlert(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ALERT_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
            message = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE)),
            bloodGroup = cursor.getString(cursor.getColumnIndexOrThrow(COL_BLOOD_GROUP)),
            priority = AlertPriority.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_PRIORITY))),
            type = AlertType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE))),
            location = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOCATION)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
            isRead = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_READ)) == 1,
            isActive = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_ACTIVE)) == 1,
            expiryTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EXPIRY_TIME)).takeIf { it != 0L },
            requiredUnits = cursor.getInt(cursor.getColumnIndexOrThrow(COL_REQUIRED_UNITS)),
            contactInfo = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTACT_INFO))
        )
    }
    
    private fun refresh() {
        Thread {
            val currentTime = System.currentTimeMillis()
            val list = mutableListOf<BloodAlert>()
            
            val query = """
                SELECT * FROM $TABLE_ALERTS 
                WHERE $COL_IS_ACTIVE = 1 
                AND ($COL_EXPIRY_TIME IS NULL OR $COL_EXPIRY_TIME > ?)
                ORDER BY $COL_PRIORITY DESC, $COL_TIMESTAMP DESC
                LIMIT 50
            """.trimIndent()
            
            val cursor = dbHelper.readableDatabase.rawQuery(query, arrayOf(currentTime.toString()))
            cursor.use {
                while (it.moveToNext()) {
                    list.add(cursorToAlert(it))
                }
            }
            _alerts.postValue(list)
        }.start()
    }
}

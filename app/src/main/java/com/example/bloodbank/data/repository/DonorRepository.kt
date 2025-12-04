package com.example.bloodbank.data.repository

import android.content.ContentValues
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bloodbank.data.entity.Donor
import com.example.bloodbank.data.sqlite.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.bloodbank.data.cloud.CloudSync

class DonorRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context.applicationContext)
    private val _donors = MutableLiveData<List<Donor>>(emptyList())
    val donors: LiveData<List<Donor>> = _donors
    private val cloud = CloudSync()

    init {
        // Initial load
        refresh()
    }

    suspend fun addDonor(donor: Donor) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_NAME, donor.name)
            put(DatabaseHelper.COL_BLOOD_GROUP, donor.bloodGroup)
            put(DatabaseHelper.COL_PHONE, donor.phone)
            put(DatabaseHelper.COL_CITY, donor.city)
        }
        dbHelper.writableDatabase.insert(DatabaseHelper.TABLE_DONORS, null, values)
        refresh()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DatabaseHelper.TABLE_DONORS, null, null)
        refresh()
    }

    suspend fun loadSamples() = withContext(Dispatchers.IO) {
        val names = listOf(
            "Raghu","Raghavan","Arun","Arnav","Aryan","Shreyash","Suman","Vikram","Rahul","Rohit",
            "Amit","Aman","Ankit","Anil","Karan","Kunal","Kapil","Varun","Vivek","Vikas",
            "Abhishek","Saurabh","Siddharth","Sandeep","Sanjay","Sachin","Sameer","Sumit","Sunil","Suraj",
            "Ashish","Deepak","Gaurav","Harsh","Himanshu","Jatin","Jay","Manish","Mayank","Mohit",
            "Neeraj","Nikhil","Nilesh","Pankaj","Pradeep","Prashant","Pratik","Rajat","Rakesh","Ramesh",
            "Srinivas","Srikanth","Shivam","Shyam","Tarun","Uday","Ujjwal","Yash","Yogesh","Aakash",
            "Aditya","Akash","Alok","Anshul","Arvind","Balaji","Bhavesh","Chetan","Chirag","Darshan",
            "Dev","Dinesh","Dipesh","Farhan","Girish","Imran","Ishan","Jagdish","Jaspreet","Karthik",
            "Krishna","Lalit","Lokesh","Madhav","Mahesh","Naveen","Navin","Omkar","Parth","Raghav",
            "Rishi","Sahil","Sarvesh","Shaurya","Shivansh","Tejas","Tushar","Vedant","Vishal","Zeeshan"
        )
        val cities = listOf(
            "Mumbai","Delhi","Bengaluru","Hyderabad","Chennai","Kolkata","Pune","Ahmedabad","Jaipur","Surat",
            "Lucknow","Indore","Bhopal","Nagpur","Patna","Kanpur","Thane","Nashik","Vadodara","Rajkot"
        )
        val bloodGroups = listOf("A+","A-","B+","B-","AB+","AB-","O+","O-")

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (i in 0 until 100) {
                val name = names[i % names.size]
                val city = cities[(i * 3) % cities.size]
                val bg = bloodGroups[(i * 5) % bloodGroups.size]
                val phone = (9000000000L + (100000L * (i % 100)) + (i % 1000)).toString()

                val values = ContentValues().apply {
                    put(DatabaseHelper.COL_NAME, name)
                    put(DatabaseHelper.COL_BLOOD_GROUP, bg)
                    put(DatabaseHelper.COL_PHONE, phone)
                    put(DatabaseHelper.COL_CITY, city)
                }
                db.insert(DatabaseHelper.TABLE_DONORS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refresh()
    }

    private fun refresh() {
        // Load on background and post
        Thread {
            val list = mutableListOf<Donor>()
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                DatabaseHelper.TABLE_DONORS,
                arrayOf(DatabaseHelper.COL_ID, DatabaseHelper.COL_NAME, DatabaseHelper.COL_BLOOD_GROUP, DatabaseHelper.COL_PHONE, DatabaseHelper.COL_CITY),
                null, null, null, null,
                DatabaseHelper.COL_NAME + " ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val name = it.getString(1)
                    val bg = it.getString(2)
                    val phone = it.getString(3)
                    val city = it.getString(4)
                    list.add(Donor(id = id, name = name, bloodGroup = bg, phone = phone, city = city))
                }
            }
            _donors.postValue(list)
        }.start()
    }

    // Helpers for cloud sync
    suspend fun getAllLocal(): List<Donor> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Donor>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_DONORS,
            arrayOf(DatabaseHelper.COL_ID, DatabaseHelper.COL_NAME, DatabaseHelper.COL_BLOOD_GROUP, DatabaseHelper.COL_PHONE, DatabaseHelper.COL_CITY),
            null, null, null, null,
            DatabaseHelper.COL_NAME + " ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val name = it.getString(1)
                val bg = it.getString(2)
                val phone = it.getString(3)
                val city = it.getString(4)
                list.add(Donor(id = id, name = name, bloodGroup = bg, phone = phone, city = city))
            }
        }
        list
    }

    suspend fun replaceAll(newList: List<Donor>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(DatabaseHelper.TABLE_DONORS, null, null)
            newList.forEach { donor ->
                val values = ContentValues().apply {
                    put(DatabaseHelper.COL_NAME, donor.name)
                    put(DatabaseHelper.COL_BLOOD_GROUP, donor.bloodGroup)
                    put(DatabaseHelper.COL_PHONE, donor.phone)
                    put(DatabaseHelper.COL_CITY, donor.city)
                }
                db.insert(DatabaseHelper.TABLE_DONORS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refresh()
    }

    // Cloud sync methods
    suspend fun pushToCloudSafe() = withContext(Dispatchers.IO) {
        runCatching { cloud.pushAll(getAllLocal()) }
    }

    suspend fun pullFromCloudSafe() = withContext(Dispatchers.IO) {
        runCatching {
            val remote = cloud.pullAll()
            replaceAll(remote)
        }
    }
}

package com.example.bloodbank.data.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DONORS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_BLOOD_GROUP TEXT NOT NULL,
                $COL_PHONE TEXT NOT NULL,
                $COL_CITY TEXT NOT NULL
            );
            """.trimIndent()
        )
        seedIfEmpty(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DONORS")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        seedIfEmpty(db)
    }

    private fun seedIfEmpty(db: SQLiteDatabase) {
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_DONORS", null)
        cursor.use {
            if (it.moveToFirst()) {
                val count = it.getLong(0)
                if (count > 0) return
            }
        }

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

        // Insert 100 donors
        for (i in 0 until 100) {
            val name = names[i % names.size]
            val city = cities[(i * 3) % cities.size]
            val bg = bloodGroups[(i * 5) % bloodGroups.size]
            val phone = (9000000000L + (100000L * (i % 100)) + (i % 1000)).toString()

            val values = ContentValues().apply {
                put(COL_NAME, name)
                put(COL_BLOOD_GROUP, bg)
                put(COL_PHONE, phone)
                put(COL_CITY, city)
            }
            db.insert(TABLE_DONORS, null, values)
        }
    }

    companion object {
        const val DB_NAME = "bloodbank.db"
        const val DB_VERSION = 1

        const val TABLE_DONORS = "donors"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_BLOOD_GROUP = "blood_group"
        const val COL_PHONE = "phone"
        const val COL_CITY = "city"
    }
}

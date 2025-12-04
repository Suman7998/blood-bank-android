package com.example.bloodbank.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MultimediaManager(private val context: Context) {
    
    companion object {
        private const val TAG = "MultimediaManager"
        const val REQUEST_CAMERA_PERMISSION = 1001
        const val REQUEST_AUDIO_PERMISSION = 1002
        const val REQUEST_STORAGE_PERMISSION = 1003
        const val REQUEST_CALL_PERMISSION = 1004
        
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE
        )
    }
    
    private val storage = FirebaseStorage.getInstance()
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    
    // Initialize TTS
    fun initializeTextToSpeech(onInitComplete: (Boolean) -> Unit) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                onInitComplete(true)
            } else {
                onInitComplete(false)
            }
        }
    }
    
    // Check if all required permissions are granted
    fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    // Request permissions
    fun requestPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CAMERA_PERMISSION)
    }
    
    // Create image file
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = File(context.getExternalFilesDir(null), "images")
        if (!storageDir.exists()) storageDir.mkdirs()
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
    
    // Create audio file
    fun createAudioFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val audioFileName = "AUDIO_${timeStamp}_"
        val storageDir = File(context.getExternalFilesDir(null), "audio")
        if (!storageDir.exists()) storageDir.mkdirs()
        return File.createTempFile(audioFileName, ".3gp", storageDir)
    }
    
    // Create video file
    fun createVideoFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val videoFileName = "VIDEO_${timeStamp}_"
        val storageDir = File(context.getExternalFilesDir(null), "videos")
        if (!storageDir.exists()) storageDir.mkdirs()
        return File.createTempFile(videoFileName, ".mp4", storageDir)
    }
    
    // Compress image
    suspend fun compressImage(imageFile: File): File = withContext(Dispatchers.IO) {
        try {
            Compressor.compress(context, imageFile) {
                resolution(1280, 720)
                quality(80)
                format(Bitmap.CompressFormat.JPEG)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image", e)
            imageFile
        }
    }
    
    // Create thumbnail
    suspend fun createThumbnail(imageFile: File): File = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val thumbnailBitmap = Bitmap.createScaledBitmap(bitmap, 150, 150, false)
            
            val thumbnailFile = File(imageFile.parent, "thumb_${imageFile.name}")
            FileOutputStream(thumbnailFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            
            bitmap.recycle()
            thumbnailBitmap.recycle()
            thumbnailFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating thumbnail", e)
            imageFile
        }
    }
    
    // Upload file to Firebase Storage
    suspend fun uploadFile(file: File, path: String): String = withContext(Dispatchers.IO) {
        try {
            val storageRef = storage.reference.child(path)
            val uri = Uri.fromFile(file)
            val uploadTask = storageRef.putFile(uri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file", e)
            throw e
        }
    }
    
    // Start audio recording
    fun startAudioRecording(outputFile: File): Boolean {
        return try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(outputFile.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording", e)
            false
        }
    }
    
    // Stop audio recording
    fun stopAudioRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording", e)
        }
    }
    
    // Play audio file
    fun playAudio(audioFile: File, onComplete: (() -> Unit)? = null) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnCompletionListener { onComplete?.invoke() }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
        }
    }
    
    // Stop audio playback
    fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
    }
    
    // Speak text using TTS
    fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    
    // Get video thumbnail
    fun getVideoThumbnail(videoFile: File): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)
            val bitmap = retriever.getFrameAtTime(1000000) // Get frame at 1 second
            retriever.release()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video thumbnail", e)
            null
        }
    }
    
    // Make phone call
    fun makePhoneCall(activity: Activity, phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            == PackageManager.PERMISSION_GRANTED) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                activity.startActivity(callIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error making phone call", e)
            }
        } else {
            ActivityCompat.requestPermissions(
                activity, 
                arrayOf(Manifest.permission.CALL_PHONE), 
                REQUEST_CALL_PERMISSION
            )
        }
    }
    
    // Clean up resources
    fun cleanup() {
        stopAudio()
        stopAudioRecording()
        textToSpeech?.shutdown()
    }
    
    // Get file URI for camera intent
    fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}

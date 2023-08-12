package com.example.test000

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.util.*


class Video2Text : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private var cameraRequestCode: Int = 123
    private var galleryRequestCode: Int = 122
    private lateinit var txtTranslated: TextView
    private lateinit var tts: TextToSpeech
    private lateinit var pythonModule: PyObject
    private var selectedVideos: MutableList<File> = mutableListOf()
    private var videos: ArrayList<Uri> = ArrayList()
    private var currentVideoPosition = 0


    internal var currentTranslationLanguage: String = ""

    //Change Pages
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video2text)

        val intent = intent
        // Check if the intent contains an extra with key "translationLanguage"
        if (intent.hasExtra("currentTranslationLanguage")) {
            currentTranslationLanguage = intent.getStringExtra("currentTranslationLanguage")!!
        }

        val languagesOptions = findViewById<ImageView>(R.id.languagesMenu)
        languagesOptions.setOnClickListener {
            val popupMenu = PopupMenu(this, it)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.englishLanguage -> {
                        currentTranslationLanguage = "En"
                        true
                    }

                    R.id.arabicLanguage -> {
                        currentTranslationLanguage = "Ar"
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
            popupMenu.inflate(R.menu.menu_main)
            popupMenu.show()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.tranSignBetaBtn

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.signifyBtn -> {
                    val int = Intent(applicationContext, TextVoice2SL::class.java)
                    int.putExtra("currentTranslationLanguage", currentTranslationLanguage)
                    startActivity(int)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }

                R.id.tranSignBtn -> {
                    val int = Intent(applicationContext, Image2Text::class.java)
                    int.putExtra("currentTranslationLanguage", currentTranslationLanguage)
                    startActivity(int)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }

                R.id.tranSignBetaBtn -> true
                else -> false
            }
        }

        tts = TextToSpeech(applicationContext) { status ->
            if (status != TextToSpeech.ERROR) {
                if (currentTranslationLanguage != "") {
                    if (currentTranslationLanguage == "En") {
                        // Set language for TextToSpeech
                        tts.language = Locale.US
                    } else if (currentTranslationLanguage == "Ar") {
                        tts.language = Locale("ar")
                    }
                } else {
                    tts.language = Locale.US
                }
            }
        }
        txtTranslated = findViewById(R.id.out_trans_text)


        val videoBtn = findViewById<ImageButton>(R.id.videoButton)
        videoBtn.setOnClickListener {
            askCameraPermissions()
        }

        val galleryBtn = findViewById<ImageButton>(R.id.galleryButton)
        galleryBtn.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)
            galleryIntent.type = "video/*"
            startActivityForResult(galleryIntent, galleryRequestCode)
        }

        val txt2speechButton = findViewById<ImageButton>(R.id.out_voice_button)
        txt2speechButton.setOnClickListener {

            tts.speak((txtTranslated.text), TextToSpeech.QUEUE_FLUSH, null, null)
        }


        val transBtn = findViewById<Button>(R.id.trans_button)
        transBtn.setOnClickListener {
            if (currentTranslationLanguage == "Ar") {
                Toast.makeText(
                    this,
                    "This Feature Only Work in English Language, Change Translation Language to English !!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                uploadAndTranslateVideos()
            }
        }

        val nextBtn = findViewById<ImageButton>(R.id.nextBtn)
        nextBtn.setOnClickListener {
            if (currentVideoPosition < videos.size - 1) {
                currentVideoPosition++
                videoView.setVideoURI(videos[currentVideoPosition])
                videoView.setOnPreparedListener {
                    videoView.start()
                    // now set up media controller for the play pause next pre
                    val mediaCollection = MediaController(this)
                    mediaCollection.setAnchorView(videoView)
                    videoView.setMediaController(mediaCollection)
                }
            } else {
                //no more images
                Toast.makeText(this, "No More Videos...", Toast.LENGTH_SHORT).show()
            }
        }

        val prevBtn = findViewById<ImageButton>(R.id.previousBtn)
        prevBtn.setOnClickListener {
            if (currentVideoPosition > 0) {
                currentVideoPosition--
                videoView.setVideoURI(videos[currentVideoPosition])
                videoView.setOnPreparedListener {
                    videoView.start()
                    // now set up media controller for the play pause next pre
                    val mediaCollection = MediaController(this)
                    mediaCollection.setAnchorView(videoView)
                    videoView.setMediaController(mediaCollection)
                }
            } else {
                //no more Videos
                Toast.makeText(this, "No More Videos...", Toast.LENGTH_SHORT).show()
            }
        }

        videoView = findViewById(R.id.record_videoView)

        initializePython()
    }

    private fun initializePython() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        val py = Python.getInstance()
        pythonModule = py.getModule("client")
    }


    override fun onDestroy() {
        // Shut down TextToSpeech when the activity is destroyed
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        selectedVideos.clear()
        videos.clear()
        if (requestCode == cameraRequestCode && resultCode == RESULT_OK) {
            val videoUri = data?.data
            handleVideoUri(videoUri)
        } else if (requestCode == galleryRequestCode && resultCode == RESULT_OK) {
            // if multiple Videos are selected
            if (data?.clipData != null) {
                val count = data.clipData?.itemCount
                for (i in 0 until count!!) {
                    val videoUri: Uri = data.clipData?.getItemAt(i)!!.uri
                    videos.add(videoUri)
                    selectedVideos.add(createVideoFile(videoUri))
                }
                videoView.setVideoURI(videos[0])
                videoView.setOnPreparedListener {
                    videoView.start()
                    // now set up media controller for the play pause next pre
                    val mediaCollection = MediaController(this)
                    mediaCollection.setAnchorView(videoView)
                    videoView.setMediaController(mediaCollection)
                }
            } else if (data?.data != null) {
                // if single video is selected
                val videoUri: Uri = data.data!!
                handleVideoUri(videoUri)
            }
        }
    }

    private fun handleVideoUri(videoUri: Uri?) {
        if (videoUri != null) {
            videos.add(videoUri)
            selectedVideos.add(createVideoFile(videoUri))
            videoView.setVideoURI(videoUri)
            videoView.setOnPreparedListener {
                videoView.start()
                // now set up media controller for the play pause next pre
                val mediaCollection = MediaController(this)
                mediaCollection.setAnchorView(videoView)
                videoView.setMediaController(mediaCollection)
            }
        }
    }

    private fun createVideoFile(videoUri: Uri): File {
        val tempFile = File.createTempFile("vid", ".mp4")
        tempFile.createNewFile()
        val inputStream = contentResolver.openInputStream(videoUri!!)
        val outputStream = FileOutputStream(tempFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input?.copyTo(output)
            }
        }
        outputStream.flush()
        outputStream.close()
        return tempFile
    }

    private fun askCameraPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraRequestCode
            )
        } else {
            dispatchTakeVideoIntent()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakeVideoIntent()
            } else {
                Toast.makeText(
                    this,
                    "Camera Permission is Required to Use camera.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun uploadAndTranslateVideos() {
        val n = selectedVideos.size
        val translatedTextList: MutableList<String> = MutableList(n) { "" }
        val storageRef = FirebaseStorage.getInstance().reference
        var uploadedCount = 0
        for (file in selectedVideos) {
            val videoUri = Uri.fromFile(file)
            val videoRef = storageRef.child("videos/${file.name}")
            val uploadTask = videoRef.putFile(videoUri)
            uploadTask.addOnSuccessListener { taskSnapshot ->
                // File uploaded successfully, get the download URL
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener {
                    val fileName = file.name
                    val uploadedVideoIndex = selectedVideos.indexOf(file)
                    val prediction = translateVideo(fileName)
                    translatedTextList[uploadedVideoIndex] = prediction
                    uploadedCount++
                    if (uploadedCount == selectedVideos.size) {
                        val stringBuilder: StringBuilder = StringBuilder()
                        for (word in translatedTextList) {
                            stringBuilder.append(word)
                            stringBuilder.append(" ")
                        }
                        txtTranslated.post { // using txtTranslated.post to update TextView in UI thread
                            txtTranslated.text = stringBuilder.toString()
                        }
                    }
                }
            }.addOnFailureListener { exception ->
                // Handle the failure event here
                Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun translateVideo(fileName: String): String {
        try {
            return pythonModule.callAttr("translate_video_En", fileName)
                .toJava(String::class.java)
        } catch (e: PyException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
        return ""
    }


    private fun dispatchTakeVideoIntent() {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takeVideoIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takeVideoIntent, cameraRequestCode)
        }
    }
}
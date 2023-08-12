package com.example.test000


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class Image2Text : AppCompatActivity() {

    private var cameraRequestCode: Int = 123
    private var galleryRequestCode: Int = 122
    private lateinit var txtTranslated: TextView
    private lateinit var tts: TextToSpeech
    private lateinit var pythonModule: PyObject
    private var selectedImages: MutableList<File> = mutableListOf()
    private lateinit var cameraPhoto: File
    internal var currentTranslationLanguage: String = ""
    private lateinit var imageSwitcher: ImageSwitcher

    //store uris of picked images
    private var images: ArrayList<Uri> = ArrayList()

    // current position/index of selected image
    private var currentImagePosition = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image2text)

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
        bottomNavigationView.selectedItemId = R.id.tranSignBtn

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

                R.id.tranSignBtn -> true
                R.id.tranSignBetaBtn -> {
                    val int = Intent(applicationContext, Video2Text::class.java)
                    int.putExtra("currentTranslationLanguage", currentTranslationLanguage)
                    startActivity(int)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }

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
        imageSwitcher = findViewById(R.id.imageSwitcher)


        val cameraBtn = findViewById<ImageButton>(R.id.cameraButton)
        cameraBtn.setOnClickListener {
            askCameraPermissions()
        }
        val galleryBtn = findViewById<ImageButton>(R.id.galleryButton)
        galleryBtn.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)
            galleryIntent.type = "image/*"
            startActivityForResult(galleryIntent, galleryRequestCode)
        }

        val txt2speechButton = findViewById<ImageButton>(R.id.out_voice_button)
        txt2speechButton.setOnClickListener {

            tts.speak((txtTranslated.text), TextToSpeech.QUEUE_FLUSH, null, null)
        }

        val transBtn = findViewById<Button>(R.id.trans_button)
        transBtn.setOnClickListener {
            uploadAndTranslateImages()
        }

        val nextBtn = findViewById<ImageButton>(R.id.nextBtn)
        nextBtn.setOnClickListener {
            if (currentImagePosition < images.size - 1) {
                currentImagePosition++
                imageSwitcher.setImageURI(images[currentImagePosition])
            } else {
                //no more images
                Toast.makeText(this, "No More Images...", Toast.LENGTH_SHORT).show()
            }
        }

        val prevBtn = findViewById<ImageButton>(R.id.previousBtn)
        prevBtn.setOnClickListener {
            if (currentImagePosition > 0) {
                currentImagePosition--
                imageSwitcher.setImageURI(images[currentImagePosition])
            } else {
                //no more images
                Toast.makeText(this, "No More Images...", Toast.LENGTH_SHORT).show()
            }
        }

        imageSwitcher.setFactory { ImageView(this) }


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

    private fun uploadAndTranslateImages() {
        val n = selectedImages.size
        val translatedTextList: MutableList<String> = MutableList(n) { "" }
        val storageRef = FirebaseStorage.getInstance().reference
        var uploadedCount = 0
        for (file in selectedImages) {
            val photoUri = Uri.fromFile(file)
            val imageRef = storageRef.child("images/${file.name}")
            val uploadTask = imageRef.putFile(photoUri)
            uploadTask.addOnSuccessListener { taskSnapshot ->
                // File uploaded successfully, get the download URL
                taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener {
                    val fileName = file.name
                    val uploadedImageIndex = selectedImages.indexOf(file)
                    val prediction = translateImage(fileName)
                    translatedTextList[uploadedImageIndex] = prediction
                    uploadedCount++
                    if (uploadedCount == selectedImages.size) {
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

    private fun translateImage(fileName: String): String {
        try {
            if (currentTranslationLanguage != "") {
                if (currentTranslationLanguage == "En") {
                    return pythonModule.callAttr("translate_image_En", fileName)
                        .toJava(String::class.java)
                } else if (currentTranslationLanguage == "Ar") {
                    return pythonModule.callAttr("translate_image_Ar", fileName)
                        .toJava(String::class.java)
                }
            } else {
                return pythonModule.callAttr("translate_image_En", fileName)
                    .toJava(String::class.java)
            }
        } catch (e: PyException) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
        return ""
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
            dispatchTakePictureIntent()
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
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(
                    this,
                    "Camera Permission is Required to Use camera.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequestCode && resultCode == RESULT_OK) {
            selectedImages.clear()
            images.clear()

            val contentUri = Uri.fromFile(cameraPhoto)
            selectedImages.add(cameraPhoto)
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = contentUri
            this.sendBroadcast(mediaScanIntent)
            imageSwitcher.setImageURI(contentUri)
        }
        if (requestCode == galleryRequestCode && resultCode == RESULT_OK) {
            selectedImages.clear()
            images.clear()
            // if multiple images are selected
            if (data?.clipData != null) {
                val count = data.clipData?.itemCount
                for (i in 0 until count!!) {
                    val imageUri: Uri = data.clipData?.getItemAt(i)!!.uri
                    images.add(imageUri)
                    selectedImages.add(createGalleryImageFile(imageUri))
                }
                imageSwitcher.setImageURI(images[0])
            } else if (data?.data != null) {
                // if single image is selected
                val imageUri: Uri = data.data!!
                imageSwitcher.setImageURI(imageUri)
                selectedImages.add(createGalleryImageFile(imageUri))
            }
        }
    }

    private fun createGalleryImageFile(imageUri: Uri): File {
        val imageView = ImageView(this)
        imageView.setImageURI(imageUri)
        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
        val tempFile = File.createTempFile("image", ".jpg")
        val outputStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return tempFile
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: Exception) {
                // handle exception
            }
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.example.android.provider",
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                cameraPhoto = photoFile
                startActivityForResult(takePictureIntent, cameraRequestCode)
            }
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
}
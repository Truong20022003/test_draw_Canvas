package com.example.myapplication

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var customView: CustomCropVuong
    private var originalBitmap: Bitmap? = null
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            val isGranted = entry.value
            if (!isGranted) {
                Toast.makeText(this, "Permission ${entry.key} was denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        customView = findViewById(R.id.drawing_view)

        val saveCroppedImageButton = findViewById<Button>(R.id.btnUndo)
        saveCroppedImageButton.setOnClickListener {
            val croppedBitmap = customView.getCroppedBitmap()
            saveImageToGallery(croppedBitmap, this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestPermissions(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            checkAndRequestPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        val saveImg = findViewById<Button>(R.id.btn_save)
        saveImg.setOnClickListener {
//            val bitmap = customView.getBitmapFromCanvas()
//            saveImageToGallery(bitmap, this@MainActivity)
            startActivity(Intent(this, MainActivity2::class.java))
        }

//        val brushSizeSeekBar = findViewById<SeekBar>(R.id.brush_size_seekbar)
//        brushSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
//            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
//                customView.setBrushSize(progress.toFloat())
//            }
//
//            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
//            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
//        })



        val uploadImageButton = findViewById<Button>(R.id.btn_upload_image)
        uploadImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        val saveImageButton = findViewById<Button>(R.id.btn_clear_all)
//        saveImageButton.setOnClickListener {
//            customView.erase()
//        }
//
//        val zoomInButton = findViewById<ImageView>(R.id.imgPlus)
//        val zoomOutButton = findViewById<ImageView>(R.id.imgMinus)
//
//        zoomInButton.setOnClickListener {
//            customView.zoomIn()
//        }
//
//        zoomOutButton.setOnClickListener {
//            customView.zoomOut()
//        }
//        val btnUndo = findViewById<Button>(R.id.btnUndo)
//        btnUndo.setOnClickListener {
//            customView.undo()
//
//        }


        val imgBack = findViewById<ImageView>(R.id.imgBack)
        val imgNext = findViewById<ImageView>(R.id.imgNext)
        val imgUp = findViewById<ImageView>(R.id.imgUp)
        val imgDown = findViewById<ImageView>(R.id.imgDown)

//
//        imgBack.setOnClickListener {
//            customView.translateImage(-10f, 0f)
//        }
//
//        imgNext.setOnClickListener {
//            customView.translateImage(10f, 0f)
//        }
//
//        imgUp.setOnClickListener {
//            customView.translateImage(0f, -10f)
//        }
//
//        imgDown.setOnClickListener {
//            customView.translateImage(0f, 10f)
//        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    originalBitmap = bitmap
                    customView.setImageBitmap(bitmap) // Hiển thị ảnh lên view
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
//            data?.data?.let { uri ->
//                try {
//                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
//                    val bitmap = BitmapFactory.decodeStream(inputStream)
////                    customView.setBackgroundImage(bitmap)
////                    customView.clearCanvas()
//
//                    originalBitmap = bitmap
////                    customView.setBackgroundImage(bitmap)
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }

    private fun checkAndRequestPermissions(vararg permissions: String) {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun saveImageWithDrawing() {
        originalBitmap?.let { bitmap ->
            val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            customView.draw(canvas)
            try {
                val outputStream = FileOutputStream("/sdcard/saved_image.png")
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap, context: Context): String? {
        val filename = "${System.currentTimeMillis()}.jpg"
        var savedImagePath: String? = null

        val fos: OutputStream?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Trên Android 10 (API 29) trở lên, sử dụng MediaStore để lưu
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val imageUri: Uri? =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
            savedImagePath = imageUri?.toString()
        } else {
            // Trên Android 9 (API 28) trở xuống, lưu vào thư mục Pictures
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .toString()
            val image = File(imagesDir, filename)
            savedImagePath = image.absolutePath
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
        }

        // Cập nhật thư viện để hiển thị ảnh mới
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val file = File(savedImagePath)
            val uri = Uri.fromFile(file)
            mediaScanIntent.data = uri
            context.sendBroadcast(mediaScanIntent)
        }

        return savedImagePath
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }
}

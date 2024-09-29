package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity2 : AppCompatActivity() {

    private lateinit var customGetPixelColor: CustomGetPixelColor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        customGetPixelColor = findViewById(R.id.drawing_view)

        val selectImageResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val uri = data?.data
                uri?.let {
                    val inputStream = contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    customGetPixelColor.setBitmap(bitmap)
                }
            }
        }

        findViewById<Button>(R.id.upload_button).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            selectImageResultLauncher.launch(intent)
        }


        val saveImg = findViewById<Button>(R.id.btnStartactivy3)
        saveImg.setOnClickListener {
            startActivity(Intent(this,MainActivity3::class.java))
        }
    }
}

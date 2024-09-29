package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity3 : AppCompatActivity() {

    private lateinit var customDrawView: CusomDraw
    private lateinit var getImageLauncher: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main3)



        customDrawView = findViewById(R.id.custom_draw_view)

        findViewById<Button>(R.id.btn_solid).setOnClickListener {
            customDrawView.setBrushStyle(CusomDraw.BrushType.SOLID)
        }

        findViewById<Button>(R.id.btn_dashed).setOnClickListener {
            customDrawView.setBrushStyle(CusomDraw.BrushType.DASHED)
        }

        findViewById<Button>(R.id.btn_platinum).setOnClickListener {
//            customDrawView.undo()
            customDrawView.setBrushStyle(CusomDraw.BrushType.PLATINUM)
        }
        findViewById<Button>(R.id.btn_earser).setOnClickListener {
            customDrawView.erase()
        }
        val selectedColorString = "#44DCD3"

        customDrawView.updateSelectedColor(selectedColorString)

        getImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.data
                uri?.let {
                    customDrawView.loadBackgroundImage(it)
                }
            }
        }

        findViewById<Button>(R.id.btn_upLoadImage3).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            getImageLauncher.launch(intent)
        }
    }
}
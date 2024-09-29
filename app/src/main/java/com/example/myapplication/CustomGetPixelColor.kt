package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast

class CustomGetPixelColor(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var bitmap: Bitmap? = null
    private var popupWindow: PopupWindow? = null

    fun setBitmap(newBitmap: Bitmap) {
        bitmap = newBitmap
        invalidate() // Vẽ lại view sau khi ảnh thay đổi
    }

    init {
        // Khởi tạo PopupWindow cho kính lúp
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.popup_color, null)
        popupWindow = PopupWindow(popupView, 200, 200, false).apply {
            isOutsideTouchable = false
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let {
            // Vẽ ảnh trên canvas
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (x >= 0 && x < (bitmap?.width ?: 0) && y >= 0 && y < (bitmap?.height ?: 0)) {
                    val pixelColor = bitmap?.getPixel(x, y) ?: Color.TRANSPARENT
                    val hexColor = String.format("#%06X", 0xFFFFFF and pixelColor)

                    // Hiển thị kính lúp tại vị trí tay người dùng
                    showColorLens(event.x, event.y, hexColor)

                    // Hiển thị mã màu qua Toast (hoặc có thể bỏ qua nếu không cần)
                    Toast.makeText(context, "Color: $hexColor", Toast.LENGTH_SHORT).show()
                }
            }
            MotionEvent.ACTION_UP -> {
                // Ẩn kính lúp khi người dùng thả tay
                popupWindow?.dismiss()
            }
        }
        return true
    }

    private fun showColorLens(x: Float, y: Float, color: String) {
        popupWindow?.let {
            val popupView = it.contentView
            val colorTextView = popupView.findViewById<TextView>(R.id.color_text)
            colorTextView.text = color
            colorTextView.setBackgroundColor(Color.parseColor(color))

            // Hiển thị PopupWindow ngay cạnh vị trí chạm
            if (!it.isShowing) {
                it.showAtLocation(this, Gravity.NO_GRAVITY, (x + 50).toInt(), (y - 200).toInt())
            } else {
                it.update((x + 50).toInt(), (y - 200).toInt(), -1, -1)
            }
        }
    }
}

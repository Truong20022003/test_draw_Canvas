package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.InputStream

class CusomDraw(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val path = Path()
    private var paint: Paint = Paint()
    private var backgroundBitmap: Bitmap? = null
    private var drawingBitmap: Bitmap? = null
    private var drawingCanvas: Canvas? = null
    private var drawRect: RectF? = null
    private var selectedRect: Boolean = false

    private var minX = Float.MAX_VALUE
    private var minY = Float.MAX_VALUE
    private var maxX = Float.MIN_VALUE
    private var maxY = Float.MIN_VALUE

    private var selectedColor: Int = Color.BLACK

    enum class BrushType {
        SOLID, DASHED, PLATINUM
    }

    private var brushType: BrushType = BrushType.SOLID

    init {
        paint.isAntiAlias = true
        paint.strokeWidth = 40f
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = selectedColor
        setBrushStyle(brushType)
    }

    private fun createTextureFromImage(selectedColor: Int): Bitmap {
        val bitmapSize = 100
        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val red = Color.red(selectedColor)
        val green = Color.green(selectedColor)
        val blue = Color.blue(selectedColor)
        val squareSize = 1
        for (i in 0 until bitmapSize step squareSize) {
            for (j in 0 until bitmapSize step squareSize) {
                val randomFactor = (Math.random() * 0.4 + 0.8).toFloat()
                val newRed = (red * randomFactor).toInt().coerceIn(0, 255)
                val newGreen = (green * randomFactor).toInt().coerceIn(0, 255)
                val newBlue = (blue * randomFactor).toInt().coerceIn(0, 255)
                paint.color = Color.rgb(newRed, newGreen, newBlue)
                canvas.drawRect(
                    i.toFloat(),
                    j.toFloat(),
                    (i + squareSize).toFloat(),
                    (j + squareSize).toFloat(),
                    paint
                )
            }
        }
        return bitmap
    }

    fun setBrushStyle(type: BrushType) {
        brushType = type
        paint.xfermode = null
        when (brushType) {
            BrushType.SOLID -> {
                paint.pathEffect = null
                paint.shader = null
                paint.color = selectedColor
            }

            BrushType.DASHED -> {
                paint.pathEffect = DashPathEffect(floatArrayOf(60f, 40f, 60f, 80f), 0f)
                paint.shader = null
                paint.color = selectedColor
            }

            BrushType.PLATINUM -> {
                paint.pathEffect = null
                val bitmap = createTextureFromImage(selectedColor)
                val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                paint.shader = shader
            }
        }
        invalidate()
    }

    fun updateSelectedColor(colorString: String) {
        try {
            val color = Color.parseColor(colorString)
            this.selectedColor = color
            setBrushStyle(brushType)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        backgroundBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        drawingBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        drawRect?.let {
            val rectPaint = Paint().apply {
                style = Paint.Style.STROKE
                color = Color.RED
                strokeWidth = 5f
            }
            canvas.drawRect(it, rectPaint)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (drawingBitmap == null) {
            drawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawingCanvas = Canvas(drawingBitmap!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                path.moveTo(x, y)
                minX = x
                minY = y
                maxX = x
                maxY = y
                selectedRect = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y

                drawingCanvas?.drawPath(path, paint)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                path.reset()
                selectedRect = true

                drawRect = RectF(minX, minY, maxX, maxY)
                invalidate()
            }
        }
        return true
    }

    fun loadBackgroundImage(uri: Uri) {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        backgroundBitmap = BitmapFactory.decodeStream(inputStream)
        invalidate()
    }

    fun erase() {
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        invalidate()
    }

}

package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.min
import android.graphics.BlurMaskFilter
// hinh tron
class CircleCropView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var imageBitmap: Bitmap? = null
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 5f
    }
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 180 // Độ mờ (0-255)
    }
    private val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        maskFilter = BlurMaskFilter(50f, BlurMaskFilter.Blur.OUTER) // Hiệu ứng blur
    }
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
        isDither = true
    }
    private val imageMatrix = Matrix()

    private var scaleFactor = 1.0f
    private var posX = 0f
    private var posY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f) // Giới hạn zoom
            invalidate()
            return true
        }
    })

    fun setImageBitmap(bitmap: Bitmap) {
        this.imageBitmap = bitmap

        // Tính tỷ lệ co cho chiều rộng và chiều cao
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        val scale = min(viewWidth / bitmapWidth, viewHeight / bitmapHeight)

        // Tính toán dịch chuyển để căn giữa
        val dx = (viewWidth - bitmapWidth * scale) / 2
        val dy = (viewHeight - bitmapHeight * scale) / 2

        // Cập nhật matrix để hiển thị ảnh cân đối
        imageMatrix.reset()
        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(dx, dy)

        invalidate() // Vẽ lại View
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        imageBitmap?.let {
            canvas.save()
            canvas.translate(posX, posY)
            canvas.scale(scaleFactor, scaleFactor)
            canvas.drawBitmap(it, imageMatrix, imagePaint)
            canvas.restore()
        }

        val radius = min(width, height) / 2f
        val centerX = width / 2f
        val centerY = height / 2f

        // Vẽ lớp phủ mờ bên ngoài hình tròn
        canvas.save()
        val overlayPath = Path()
        overlayPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CCW)
        overlayPath.addCircle(centerX, centerY, radius, Path.Direction.CW)
        overlayPath.fillType = Path.FillType.EVEN_ODD
        canvas.clipPath(overlayPath)
        canvas.drawPaint(overlayPaint) // Vẽ lớp phủ
        canvas.restore()

        // Vẽ hình tròn viền trắng
        canvas.drawCircle(centerX, centerY, radius, circlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                if (event.pointerCount == 1) { // Chỉ di chuyển nếu có 1 ngón tay
                    posX += x - lastTouchX
                    posY += y - lastTouchY
                }
                lastTouchX = x
                lastTouchY = y
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }
        return true
    }

    fun getCroppedBitmap(): Bitmap {
        val radius = min(width, height) / 2
        val croppedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(croppedBitmap)

        imageBitmap?.let {
            canvas.save()
            canvas.translate(posX, posY)
            canvas.scale(scaleFactor, scaleFactor)
            canvas.drawBitmap(it, imageMatrix, imagePaint)
            canvas.restore()
        }

        val output = Bitmap.createBitmap(2 * radius, 2 * radius, Bitmap.Config.ARGB_8888)
        val outputCanvas = Canvas(output)
        val circlePath = Path()
        circlePath.addCircle(radius.toFloat(), radius.toFloat(), radius.toFloat(), Path.Direction.CCW)

        outputCanvas.clipPath(circlePath)
        outputCanvas.drawBitmap(croppedBitmap, -width / 2f + radius, -height / 2f + radius, null)

        return output
    }
}

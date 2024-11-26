package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.min

class CustomCropVuong @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var imageBitmap: Bitmap? = null
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 5f
    }
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 180
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
            scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)
            invalidate()
            return true
        }
    })

    fun setImageBitmap(bitmap: Bitmap) {
        this.imageBitmap = bitmap

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        val scale = min(viewWidth / bitmapWidth, viewHeight / bitmapHeight)

        val dx = (viewWidth - bitmapWidth * scale) / 2
        val dy = (viewHeight - bitmapHeight * scale) / 2

        imageMatrix.reset()
        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(dx, dy)

        invalidate()
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

        val cropWidth = width // Chiều ngang hình chữ nhật (80% chiều rộng View)
        val cropHeight = width * 0.8f // Chiều dọc hình chữ nhật (60% chiều cao View)
        val left = (width - cropWidth) / 2
        val top = (height - cropHeight) / 2
        val right = left + cropWidth
        val bottom = top + cropHeight

        canvas.save()
        val overlayPath = Path()
        overlayPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CCW)
        overlayPath.addRect(left.toFloat(), top, right.toFloat(), bottom, Path.Direction.CW)
        overlayPath.fillType = Path.FillType.EVEN_ODD
        canvas.clipPath(overlayPath)
        canvas.drawPaint(overlayPaint)
        canvas.restore()

        canvas.drawRect(left.toFloat(), top, right.toFloat(), bottom, borderPaint)
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
                if (event.pointerCount == 1) {
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
        val cropWidth = (width * 0.8f).toInt()
        val cropHeight = (height * 0.6f).toInt()
        val left = ((width - cropWidth) / 2).toInt()
        val top = ((height - cropHeight) / 2).toInt()

        val croppedBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(croppedBitmap)

        imageBitmap?.let {
            canvas.save()
            canvas.translate(posX - left, posY - top)
            canvas.scale(scaleFactor, scaleFactor)
            canvas.drawBitmap(it, imageMatrix, imagePaint)
            canvas.restore()
        }

        return croppedBitmap
    }
}

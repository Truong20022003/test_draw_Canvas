package com.example.myapplication

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class CustomView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var drawPath: Path = Path()
    var drawPaint: Paint = Paint()
    private var canvasPaint: Paint = Paint(Paint.DITHER_FLAG)
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null

    private var scaleFactor = 1.0f
    private var matrix = Matrix()
    private var savedMatrix = Matrix()
    private var scaleDetector: ScaleGestureDetector

    private var mode = NONE
    private var start = PointF()
    private var mid = PointF()
    private var oldDist = 1f

    val paths = mutableListOf<Path>()
    val undoPaths = mutableListOf<Path>()
    val undoPaints = mutableListOf<Paint>()

    val paints = mutableListOf<Paint>()

    var isDrawingMode = true

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    init {
        setupDrawing()
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // Lấy chiều rộng từ widthMeasureSpec (match_parent)
        val width = MeasureSpec.getSize(widthMeasureSpec)

        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        val desiredHeight = (screenHeight * 2) / 3

        // Đặt kích thước cho view
        setMeasuredDimension(width, desiredHeight)
    }



    private fun setupDrawing() {
        drawPaint.color = Color.BLACK
        drawPaint.isAntiAlias = true
        drawPaint.strokeWidth = 10f
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
    }

    fun setBrushSize(newSize: Float) {
        drawPaint.strokeWidth = newSize
    }

    fun setBrushColor(newColor: Int) {
        drawPaint.color = newColor
    }

    fun setBackgroundImage(image: Bitmap) {
        val viewWidth = width
        val viewHeight = height
        val bitmapWidth = image.width
        val bitmapHeight = image.height

        // Tính toán tỷ lệ để đảm bảo ảnh vừa với view
        val scale: Float
        val scaledWidth: Int
        val scaledHeight: Int

        val widthRatio = viewWidth.toFloat() / bitmapWidth
        val heightRatio = viewHeight.toFloat() / bitmapHeight

        scale = minOf(widthRatio, heightRatio)
        scaledWidth = (bitmapWidth * scale).toInt()
        scaledHeight = (bitmapHeight * scale).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(image, scaledWidth, scaledHeight, true)

        // Tạo bitmap mới để chứa ảnh đã thay đổi kích thước
        val resultBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Vẽ ảnh đã thay đổi kích thước lên canvas
        val left = (viewWidth - scaledWidth) / 2
        val top = (viewHeight - scaledHeight) / 2
        canvas.drawBitmap(scaledBitmap, left.toFloat(), top.toFloat(), null)

        backgroundBitmap = resultBitmap
        invalidate()
    }


    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.concat(matrix)

        backgroundBitmap?.let {
            canvas.drawBitmap(it, null, Rect(0, 0, width, height), null)
        }

        canvasBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, canvasPaint)
        }
        val length = paths.size
        val paintLength = paints.size
        for (i in 0 until length) {
            if (i < paintLength) {
                canvas.drawPath(paths[i], paints[i])
            }
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isDrawingMode) {
            val transformedEvent = transformTouchEvent(event)

            when (transformedEvent.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    drawPath = Path()
                    drawPath.moveTo(transformedEvent.x, transformedEvent.y)
                }

                MotionEvent.ACTION_MOVE -> {
                    drawPath.lineTo(transformedEvent.x, transformedEvent.y)
                    drawCanvas?.drawPath(drawPath, drawPaint)
                }

                MotionEvent.ACTION_UP -> {
                    paths.add(drawPath)
                    paints.add(Paint(drawPaint))
                    canvasBitmap?.recycle()
                    canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    drawCanvas = Canvas(canvasBitmap!!)

                    backgroundBitmap?.let {
                        drawCanvas?.drawBitmap(it, null, Rect(0, 0, width, height), null)
                    }
                    for (i in paths.indices) {
                        drawCanvas?.drawPath(paths[i], paints[i])
                    }
                }

            }
            invalidate()
            return true
        } else {
            // Nếu không trong chế độ vẽ, cho phép zoom
            scaleDetector.onTouchEvent(event)
            val pointerCount = event.pointerCount
            val touchX = event.x
            val touchY = event.y

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    start.set(touchX, touchY)
                    mode = DRAG
                }

                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG && pointerCount == 1) {
                        matrix.set(savedMatrix)
                        matrix.postTranslate(touchX - start.x, touchY - start.y)
                    } else if (mode == ZOOM) {
                        val newDist = spacing(event)
                        if (newDist > 10f) {
                            matrix.set(savedMatrix)
                            val scale = newDist / oldDist
                            matrix.postScale(scale, scale, mid.x, mid.y)
                        }
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    oldDist = spacing(event)
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix)
                        midPoint(mid, event)
                        mode = ZOOM
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE
                }
            }

            invalidate()
            return true
        }
    }


    private fun transformTouchEvent(event: MotionEvent): MotionEvent {
        val inverse = Matrix()
        matrix.invert(inverse)

        val touchPoint = floatArrayOf(event.x, event.y)
        inverse.mapPoints(touchPoint)

        val transformedEvent = MotionEvent.obtain(
            event.downTime,
            event.eventTime,
            event.action,
            touchPoint[0],
            touchPoint[1],
            event.metaState
        )
        return transformedEvent
    }


    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f)) // Giới hạn tỉ lệ zoom
            matrix.setScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            invalidate()
            return true
        }
    }

    fun zoomIn() {
        scaleFactor *= 1.1f // Tăng tỉ lệ zoom
        scaleFactor = Math.min(scaleFactor, 10.0f)
        matrix.setScale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        invalidate() // Cập nhật lại View
    }

    fun zoomOut() {
        scaleFactor /= 1.1f
        scaleFactor = Math.max(scaleFactor, 0.1f)
        matrix.setScale(scaleFactor, scaleFactor, width / 2f, height / 2f)
        invalidate()
    }

    fun translateImage(dx: Float, dy: Float) {
        matrix.postTranslate(dx, dy)
        invalidate()
    }

    fun undo() {
        drawPath
        if (paths.isNotEmpty()) {
            val lastPath = paths.removeAt(paths.size - 1)
            val lastPaint = paints.removeAt(paints.size - 1)
            undoPaths.add(lastPath)
            undoPaints.add(lastPaint)


            canvasBitmap?.recycle()
            canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            drawCanvas = Canvas(canvasBitmap!!)

            backgroundBitmap?.let {
                drawCanvas?.drawBitmap(it, null, Rect(0, 0, width, height), null)
            }
            for (i in paths.indices) {
                drawCanvas?.drawPath(paths[i], paints[i])
            }

            invalidate()
        }
    }
    fun clearCanvas() {
        paths.clear()
        paints.clear()
        undoPaths.clear()
        undoPaints.clear()
        canvasBitmap?.recycle()
        canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)

        invalidate()
    }
    fun saveBitmapToGallery(context: Context) {
        // Tạo bitmap từ canvas hiện tại
        val bitmap = getBitmapFromCanvas()

        // Lưu bitmap vào gallery
        val savedImageURL = MediaStore.Images.Media.insertImage(
            context.contentResolver,
            bitmap,
            "My Drawing",
            "Image drawn using custom view"
        )

        if (savedImageURL != null) {
            Log.d("CustomView", "Image saved to gallery: $savedImageURL")
        } else {
            Log.e("CustomView", "Failed to save image.")
        }
    }

    // Hàm để lấy bitmap từ canvas hiện tại
    fun getBitmapFromCanvas(): Bitmap {
        return canvasBitmap ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    fun erase() {
        drawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        invalidate()
    }

}






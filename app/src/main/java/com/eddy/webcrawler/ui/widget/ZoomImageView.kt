package com.eddy.webcrawler.ui.widget

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max
import kotlin.math.min

class ZoomImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var matrixValue = Matrix()
    private val matrixValues = FloatArray(9)
    private var mode = NONE

    private val last = PointF()
    private val start = PointF()
    private var minScale = 1f
    private var maxScale = 4f
    private var saveScale = 1f

    private var viewWidth = 0
    private var viewHeight = 0
    private var origWidth = 0f
    private var origHeight = 0f

    private var scaleDetector: ScaleGestureDetector

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    init {
        super.setClickable(true)
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        imageMatrix = matrixValue
        scaleType = ScaleType.MATRIX
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= scaleFactor
            if (saveScale > maxScale) {
                saveScale = maxScale
                scaleFactor = maxScale / origScale
            } else if (saveScale < minScale) {
                saveScale = minScale
                scaleFactor = minScale / origScale
            }

            if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
                matrixValue.postScale(scaleFactor, scaleFactor, viewWidth / 2f, viewHeight / 2f)
            } else {
                matrixValue.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            }
            fixTranslation()
            return true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        setupMatrix()
    }

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        setupMatrix()
    }

    private fun setupMatrix() {
        if (viewWidth == 0 || viewHeight == 0 || drawable == null) return
        if (saveScale == 1f) {
            val drawableWidth = drawable.intrinsicWidth
            val drawableHeight = drawable.intrinsicHeight
            if (drawableWidth == 0 || drawableHeight == 0) return

            val scaleX = viewWidth.toFloat() / drawableWidth.toFloat()
            val scaleY = viewHeight.toFloat() / drawableHeight.toFloat()
            val scale = min(scaleX, scaleY)
            matrixValue.setScale(scale, scale)

            var redundantYSpace = viewHeight.toFloat() - scale * drawableHeight.toFloat()
            var redundantXSpace = viewWidth.toFloat() - scale * drawableWidth.toFloat()
            redundantYSpace /= 2f
            redundantXSpace /= 2f

            matrixValue.postTranslate(redundantXSpace, redundantYSpace)
            origWidth = viewWidth - 2 * redundantXSpace
            origHeight = viewHeight - 2 * redundantYSpace
            imageMatrix = matrixValue
        }
        fixTranslation()
    }

    var onRegionClick: ((Float, Float) -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val currentPoint = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                last.set(currentPoint)
                start.set(last)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                val deltaX = currentPoint.x - last.x
                val deltaY = currentPoint.y - last.y
                val fixTransX = getFixDragTrans(deltaX, viewWidth.toFloat(), origWidth * saveScale)
                val fixTransY = getFixDragTrans(deltaY, viewHeight.toFloat(), origHeight * saveScale)
                matrixValue.postTranslate(fixTransX, fixTransY)
                fixTranslation()
                last.set(currentPoint.x, currentPoint.y)
                
                // Allow ViewPager2 to intercept if we can't scroll further horizontally
                if (saveScale > 1f) {
                    val atLeftEdge = fixTransX == 0f && deltaX > 0
                    val atRightEdge = fixTransX == 0f && deltaX < 0
                    if (atLeftEdge || atRightEdge) {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    } else {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                val xDiff = Math.abs(currentPoint.x - start.x)
                val yDiff = Math.abs(currentPoint.y - start.y)
                if (xDiff < 3 && yDiff < 3) {
                    onRegionClick?.invoke(event.x, viewWidth.toFloat())
                    performClick()
                }
            }
        }
        imageMatrix = matrixValue
        invalidate()
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun fixTranslation() {
        matrixValue.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        val fixTransX = getFixTrans(transX, viewWidth.toFloat(), origWidth * saveScale)
        val fixTransY = getFixTrans(transY, viewHeight.toFloat(), origHeight * saveScale)
        if (fixTransX != 0f || fixTransY != 0f) {
            matrixValue.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        if (trans < minTrans) return -trans + minTrans
        if (trans > maxTrans) return -trans + maxTrans
        return 0f
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0f
        } else {
            delta
        }
    }
}

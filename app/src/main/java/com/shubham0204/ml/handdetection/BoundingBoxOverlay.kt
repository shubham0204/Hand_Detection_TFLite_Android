package com.shubham0204.ml.handdetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.toRectF

class BoundingBoxOverlay(context : Context, attributeSet : AttributeSet)
    : SurfaceView( context , attributeSet ) , SurfaceHolder.Callback {

    // Variables used to compute output2overlay transformation matrix
    // These are assigned in FrameAnalyser.kt
    var areDimsInit = false
    var frameHeight = 0
    var frameWidth = 0

    // This var is assigned in FrameAnalyser.kt
    var handBoundingBoxes: List<Prediction>? = null

    // This var is assigned in MainActivity.kt
    var isFrontCameraOn = false

    private var output2OverlayTransform: Matrix = Matrix()
    private val boxPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 16f
    }
    private val textPaint = Paint().apply {
        strokeWidth = 2.0f
        textSize = 32f
        color = Color.YELLOW
    }


    override fun surfaceCreated(p0: SurfaceHolder) {
    }


    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
    }


    override fun surfaceDestroyed(p0: SurfaceHolder) {
    }


    override fun onDraw(canvas: Canvas?) {
        if ( handBoundingBoxes == null ) {
            return
        }
        if (!areDimsInit) {
            val viewWidth = canvas!!.width.toFloat()
            val viewHeight = canvas.height.toFloat()
            val xFactor: Float = viewWidth / frameWidth.toFloat()
            val yFactor: Float = viewHeight / frameHeight.toFloat()
            // Scale and mirror the coordinates ( required for front lens )
            output2OverlayTransform.preScale(xFactor, yFactor)
            if ( isFrontCameraOn ) {
                output2OverlayTransform.postScale(-1f, 1f, viewWidth / 2f, viewHeight / 2f)
                Logger.logInfo( "Transformation matrix configured for front camera." )
            }
            else {
                Logger.logInfo( "Transformation matrix configured for rear camera." )
            }
            areDimsInit = true
        }
        else {
            for ( prediction in handBoundingBoxes!! ) {
                val rect = prediction.boundingBox.toRectF()
                Logger.logInfo( "Rect before ${rect.toShortString()}")
                output2OverlayTransform.mapRect( rect )
                Logger.logInfo( "Rect before ${rect.toShortString()}")
                canvas?.drawRoundRect( rect , 16f, 16f, boxPaint )
                canvas?.drawText(
                    prediction.confidence.toString(),
                    rect.centerX(),
                    rect.centerY(),
                    textPaint
                )
            }
        }
    }




}
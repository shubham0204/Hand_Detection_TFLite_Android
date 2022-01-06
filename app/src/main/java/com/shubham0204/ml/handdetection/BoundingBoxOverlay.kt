/*
 * Copyright 2021 Shubham Panchal
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shubham0204.ml.handdetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.util.DisplayMetrics
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

    private val displayMetrics = DisplayMetrics()


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
            // Scale the coordinates
            output2OverlayTransform.preScale(xFactor, yFactor)
            if ( isFrontCameraOn ) {
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
                output2OverlayTransform.mapRect( rect )
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
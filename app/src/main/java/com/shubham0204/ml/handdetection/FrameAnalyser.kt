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
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Image Analyser for performing hand detection on camera frames.
class FrameAnalyser(
    private val context: Context ,
    private val handDetectionModel: HandDetectionModel ,
    private val boundingBoxOverlay: BoundingBoxOverlay ) : ImageAnalysis.Analyzer {

    private var frameBitmap : Bitmap? = null
    private var isFrameProcessing = false


    override fun analyze(image: ImageProxy) {
        // If a frame is being processed, drop the current frame.
        if ( isFrameProcessing ) {
            image.close()
            return
        }
        isFrameProcessing = true

        // Get the `Bitmap` of the current frame ( with corrected rotation ).
        frameBitmap = BitmapUtils.imageToBitmap( image.image!! , image.imageInfo.rotationDegrees , boundingBoxOverlay.isFrontCameraOn )
        image.close()

        // Configure frameHeight and frameWidth for output2overlay transformation matrix.
        if ( !boundingBoxOverlay.areDimsInit ) {
            Logger.logInfo( "Passing dims to overlay..." )
            boundingBoxOverlay.frameHeight = frameBitmap!!.height
            boundingBoxOverlay.frameWidth = frameBitmap!!.width
        }

        CoroutineScope( Dispatchers.Default ).launch {
            runModel( frameBitmap!! )
        }
    }


    private suspend fun runModel( inputImage : Bitmap ) = withContext( Dispatchers.Default ) {
        // Compute bounding boxes for the given frame,
        val predictions = handDetectionModel.flow( inputImage )
        withContext( Dispatchers.Main ) {
            // Notify that the current frame is processed and the pipeline is
            // ready for the next frame.
            isFrameProcessing = false
            // Submit the predictions to the BoundingBoxOverlay and update it.
            // Note, calling `drawingOverlay.invalidate()` here will call `onDraw()` in BoundingBoxOverlay.kt.
            boundingBoxOverlay.handBoundingBoxes = predictions
            boundingBoxOverlay.invalidate()
        }
    }

}
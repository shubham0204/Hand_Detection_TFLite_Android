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
import android.graphics.Rect
import android.os.Build
import android.view.ViewConfiguration
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlin.math.max
import kotlin.math.min

// Helper class for Hand detection TFLite model
class HandDetectionModel( context: Context ) {

    // I/O details for the hand detection model.
    // Refer to the comments of this script ->
    // https://github.com/tensorflow/models/blob/master/research/object_detection/export_tflite_ssd_graph.py
    // For quantization, use the tflite_convert utility as described in the conversion notebook ( README ).
    private val modelInputImageDim = 300
    private val isQuantized = false
    private val maxDetections = 10
    private val boundingBoxesTensorShape = intArrayOf( 1 , maxDetections , 4 ) // [ 1 , 10 , 4 ]
    private val confidenceScoresTensorShape = intArrayOf( 1 , maxDetections ) // [ 1 , 10 ]
    private val classesTensorShape = intArrayOf( 1 , maxDetections ) // [ 1 , 10 ]
    private val numBoxesTensorShape = intArrayOf( 1 ) // [ 1 , ]
    // Input tensor processor for quantized and non-quantized versions of the model.
    private val inputImageProcessorQuantized = ImageProcessor.Builder()
        .add( ResizeOp( modelInputImageDim , modelInputImageDim , ResizeOp.ResizeMethod.BILINEAR ) )
        .add( CastOp( DataType.FLOAT32 ) )
        .build()
    private val inputImageProcessorNonQuantized = ImageProcessor.Builder()
        .add( ResizeOp( modelInputImageDim , modelInputImageDim , ResizeOp.ResizeMethod.BILINEAR ) )
        .add( NormalizeOp( 128.5f , 128.5f ) )
        .build()

    // See app/src/main/assets for the TFLite model.
    private val modelName = "model.tflite"
    private val numThreads = 4
    private var interpreter : Interpreter
    // Confidence threshold for NMS
    private val outputConfidenceThreshold = 0.9f

    private var inputFrameWidth = 0
    private var inputFrameHeight = 0



    init {
        // Initialize TFLite Interpreter
        val interpreterOptions = Interpreter.Options().apply {
            setNumThreads( numThreads )
            Logger.logInfo( " kfjskjdf ${ViewConfiguration.get( context ).hasPermanentMenuKey()}")

            // Add the NNApiDelegate if supported.
            // See -> https://www.tensorflow.org/lite/performance/nnapi#initializing_the_nnapi_delegate
            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ) {
                Logger.logInfo( "NNAPI is supported on this device." )
                addDelegate( NnApiDelegate() )
            }
        }
        interpreter = Interpreter(FileUtil.loadMappedFile( context, modelName ) , interpreterOptions )
        Logger.logInfo( "TFLite interpreter created." )
    }


    fun flow( cameraFrameBitmap : Bitmap  ) : List<Prediction> {
        return run( cameraFrameBitmap )
    }


    private fun run( inputImage : Bitmap ) : List<Prediction> {
        // Store the width and height of the input frames as they will be used for future transformations.
        inputFrameWidth = inputImage.width
        inputFrameHeight = inputImage.height

        var tensorImage = TensorImage.fromBitmap( inputImage )
        tensorImage = if ( isQuantized ) {
            inputImageProcessorQuantized.process( tensorImage )
        }
        else {
            inputImageProcessorNonQuantized.process( tensorImage )
        }

        val confidenceScores = TensorBuffer.createFixedSize( confidenceScoresTensorShape , DataType.FLOAT32 )
        val boundingBoxes = TensorBuffer.createFixedSize( boundingBoxesTensorShape , DataType.FLOAT32 )
        val classes = TensorBuffer.createFixedSize( classesTensorShape , DataType.FLOAT32 )
        val numBoxes = TensorBuffer.createFixedSize( numBoxesTensorShape , DataType.FLOAT32 )
        val outputs = mapOf(
            0 to boundingBoxes.buffer ,
            1 to classes.buffer ,
            2 to confidenceScores.buffer ,
            3 to numBoxes.buffer
        )

        val t1 = System.currentTimeMillis()
        interpreter.runForMultipleInputsOutputs( arrayOf(tensorImage.buffer), outputs )
        Logger.logInfo( "Model inference time -> ${System.currentTimeMillis() - t1} ms." )

        return processOutputs( confidenceScores , boundingBoxes )
    }


    private fun processOutputs( scores : TensorBuffer ,
                                boundingBoxes : TensorBuffer ) : List<Prediction> {
        // Flattened version of array of shape [ 1 , maxDetections ] ( size = maxDetections )
        val scoresFloatArray = scores.floatArray
        // Flattened version of array of shape [ 1 , maxDetections , 4 ] ( size = maxDetections * 4 )
        val boxesFloatArray = boundingBoxes.floatArray
        val predictions = ArrayList<Prediction>()
        for ( i in boxesFloatArray.indices step 4 ) {
            // Store predictions which have a confidence > threshold
            if ( scoresFloatArray[ i / 4 ] >= outputConfidenceThreshold ) {
                predictions.add(
                    Prediction(
                        getRect( boxesFloatArray.sliceArray( i..i+3 )) ,
                        scoresFloatArray[ i / 4 ]
                    )
                )
            }
        }
        return predictions.toList()
    }


    // Transform the normalized bounding box coordinates relative to the input frames.
    private fun getRect( coordinates : FloatArray ) : Rect {
        return Rect(
            max( (coordinates[ 1 ] * inputFrameWidth).toInt() , 1 ),
            max( (coordinates[ 0 ] * inputFrameHeight).toInt() , 1 ),
            min( (coordinates[ 3 ] * inputFrameWidth).toInt() , inputFrameWidth ),
            min( (coordinates[ 2 ] * inputFrameHeight).toInt() , inputFrameHeight )
        )
    }

}
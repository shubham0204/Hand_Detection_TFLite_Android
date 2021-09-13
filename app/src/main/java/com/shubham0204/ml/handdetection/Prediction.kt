package com.shubham0204.ml.handdetection

import android.graphics.Rect
import android.graphics.RectF

data class Prediction(val boundingBox : Rect, val confidence : Float ) {
}
package com.volvo.tflite

import android.widget.Toast

fun debugToast(text: String) {
    @Suppress("ConstantConditionIf")
    if (BuildConfig.BUILD_TYPE == "debug") {
        Toast.makeText(App.context, text, Toast.LENGTH_SHORT).show()
    }
}
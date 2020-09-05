package com.epicqueststudios.beamtaxi.presentation.utils

import android.content.Context
import android.widget.Toast


fun Any.toast(context: Context, duration: Int = Toast.LENGTH_SHORT): Toast {
    return Toast.makeText(context, this.toString(), duration).apply { show() }
}

fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT): Toast {
    return Toast.makeText(this, text, duration).apply { show() }
}

fun Context.toast(resId: Int, duration: Int = Toast.LENGTH_SHORT): Toast {
    return toast(getString(resId), duration)
}


package com.hwb.wifidebughelper

import android.util.Log

object ALog {
    private const val TAG = "ALog"

    fun log(msg: String) {
//        if (BuildConfig.DEBUG) {
            Log.e(TAG, msg)
//        }
    }
}
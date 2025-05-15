package com.hwb.wifidebughelper

import android.app.Application
import com.hjq.toast.Toaster
import com.tencent.mmkv.MMKV

class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Toaster.init(this)
        val rootDir = MMKV.initialize(this)
    }
}
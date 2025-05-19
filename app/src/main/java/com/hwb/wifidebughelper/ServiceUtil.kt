package com.hwb.wifidebughelper

import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

object ServiceUtil {
    private var serviceBinder: MyService.MyBinder? = null
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBinder = service as MyService.MyBinder
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
        }
    }

    fun bindService(activity: MainActivity2) {
        val it = Intent(activity, MyService::class.java)
        activity.bindService(it, conn, BIND_AUTO_CREATE)
    }

    fun startService(address: String?, port: String?) {
        if (!address.isNullOrEmpty() && !port.isNullOrEmpty()) {
            serviceBinder?.saveServerAddress(address)
            serviceBinder?.saveAdbTcpipPort(port)
            serviceBinder?.connect()
        }
    }

}
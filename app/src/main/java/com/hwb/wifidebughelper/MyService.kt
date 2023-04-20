package com.hwb.wifidebughelper

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.wifidebughelper.R
import com.hwb.wifidebughelper.MainActivity.Companion.DEF_TPICP_PORT
import com.hwb.wifidebughelper.MainActivity.Companion.SP_KEY_ADB_TCOIP_PORT
import com.hwb.wifidebughelper.MainActivity.Companion.SP_KEY_SERVER_ADDRESS
import java.net.Inet4Address

class MyService : Service() {

    private val CHANNEL_DEFAULT_IMPORTANCE = "default"
    val binder by lazy {
        MyBinder(this)
    }

    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val queue by lazy {
        Volley.newRequestQueue(this)
    }

    //上次的ip地址
    var lastIPTemp = ""

    private var networkCallback: ConnectivityManager.NetworkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties)
                val find = linkProperties.linkAddresses.find { it.address is Inet4Address }
                find?.address?.hostAddress?.let {
                    //ip 发生变化，重新上报
                    if (it != lastIPTemp) {
                        reportIp(it)
                        lastIPTemp = it
                    }
                }
            }
        }

    private fun reportIp(ip: String) {
        val serverAddress = getSharedPreferences().getString(SP_KEY_SERVER_ADDRESS, null)
        if (serverAddress == null) {
            toast("请先设置服务器地址")
            return
        } else {
            val port = getSharedPreferences().getString(SP_KEY_ADB_TCOIP_PORT, DEF_TPICP_PORT)
            val url = "http://$serverAddress/string?address=$ip:$port"
            val stringRequest = StringRequest(url, {
                // Display the first 500 characters of the response string.
                toast("上报成功：$it")
            }, {
                toast("上报失败：${it.message}")
            })
            queue.add(stringRequest)
        }
    }

    private fun toast(s: String) {
        Toast.makeText(this, "${getString(R.string.app_name)}:$s", Toast.LENGTH_SHORT).show()
        Log.d("toast", s)
    }

    private fun getSharedPreferences(): SharedPreferences {
        return super.getSharedPreferences(packageName, MODE_PRIVATE)
    }


    override fun onCreate() {
        super.onCreate()
        //注册网络变化监听
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        createNotificationChannel()
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        val notification: Notification = Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
            .setContentTitle("ip 监听中")
            .setContentText("ip 监听中")
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val nm: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        //然后再重新创建渠道
        //新建channel group ，只管理该group 里面的channel
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DEFAULT_IMPORTANCE,
                "默认",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    class MyBinder(private val service: MyService) : Binder() {

        fun saveServerAddress(address: String) {
            service.getSharedPreferences().edit().putString(SP_KEY_SERVER_ADDRESS, address).apply()
            service.reportIp(service.lastIPTemp)
        }

        fun saveAdbTcpipPort(port: String) {
            service.getSharedPreferences().edit().putString(SP_KEY_ADB_TCOIP_PORT, port).apply()
            service.reportIp(service.lastIPTemp)
        }

        //链接
        fun connect() {
            service.reportIp(service.lastIPTemp)
        }
    }
}
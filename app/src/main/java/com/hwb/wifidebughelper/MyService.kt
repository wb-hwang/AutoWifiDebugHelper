package com.hwb.wifidebughelper

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ServiceCompat
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.wifidebughelper.R
import com.hwb.wifidebughelper.MainActivity.Companion.DEF_TPICP_PORT
import com.hwb.wifidebughelper.MainActivity.Companion.SP_KEY_ADB_TCOIP_PORT
import com.hwb.wifidebughelper.MainActivity.Companion.SP_KEY_SERVER_ADDRESS
import java.net.Inet4Address
import androidx.core.content.edit
import com.hwb.wifidebughelper.ConnectList.getConnectSwitch

class MyService : Service() {

    private val channelDefaultImportance = "default"
    private val binder by lazy {
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
                if (getConnectSwitch()) {
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
        }

    private fun reportIp(ip: String) {
        val serverAddress = getSharedPreferences().getString(SP_KEY_SERVER_ADDRESS, null)
        if (serverAddress == null) {
            toast("请先设置服务器地址")
            return
        } else {
            val port = getSharedPreferences().getString(SP_KEY_ADB_TCOIP_PORT, DEF_TPICP_PORT)
            val url = "http://$serverAddress/string?address=$ip:$port"
            Log.d("NetworkRequest", "尝试连接URL: $url")

            val stringRequest = object : StringRequest(
                Method.GET,
                url,
                { response ->
                    try {
                        Log.d("NetworkRequest", "请求成功: $response")
                        toast("上报成功：$response")
                    } catch (e: Exception) {
                        Log.e("NetworkRequest", "解析响应时出错", e)
                        toast("解析响应时出错: ${e.message}")
                    }
                },
                { error ->
                    val errorMsg = if (error.message != null) error.message else "连接失败"
                    val errorClass = error.javaClass.simpleName
                    val errorDetails = "错误类型: $errorClass, 错误信息: $errorMsg"

                    Log.e("NetworkRequest", "请求失败: $errorDetails", error)

                    // 处理服务器错误，尝试获取服务器返回的错误信息
                    if (errorClass.contains("ServerError") && error.networkResponse != null) {
                        try {
                            val serverErrorData = String(error.networkResponse.data)
                            Log.e("NetworkRequest", "服务器返回错误: $serverErrorData")
                            toast("服务器错误: $serverErrorData")
                        } catch (e: Exception) {
                            // 如果无法解析服务器错误信息，则显示一般错误
                            if (errorClass.contains("UnknownHostException")) {
                                toast("上报失败: 找不到主机，请确认服务器地址正确且PC服务器正在运行")
                            } else if (errorClass.contains("TimeoutError")) {
                                toast("上报失败: 连接超时，请检查网络连接")
                            } else if (errorClass.contains("ConnectionError") || errorClass.contains("NetworkError")) {
                                toast("上报失败: 网络连接错误，请确保手机和PC在同一网络")
                            } else if (errorClass.contains("ServerError")) {
                                toast("上报失败: 服务器内部错误，请检查PC端日志")
                            } else {
                                toast("上报失败: $errorMsg ($errorClass)")
                            }
                        }
                    } else {
                        // 其他类型的错误
                        if (errorClass.contains("UnknownHostException")) {
                            toast("上报失败: 找不到主机，请确认服务器地址正确且PC服务器正在运行")
                        } else if (errorClass.contains("TimeoutError")) {
                            toast("上报失败: 连接超时，请检查网络连接")
                        } else if (errorClass.contains("ConnectionError") || errorClass.contains("NetworkError")) {
                            toast("上报失败: 网络连接错误，请确保手机和PC在同一网络")
                        } else {
                            toast("上报失败: $errorMsg ($errorClass)")
                        }
                    }
                }
            ) {
                override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
                    // 记录原始响应
                    if (response != null) {
                        val responseData = String(response.data)
                        Log.d("NetworkRequest", "原始响应: $responseData")
                    }
                    return super.parseNetworkResponse(response)
                }
            }

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

        val notification: Notification = Notification.Builder(this, channelDefaultImportance)
            .setContentTitle("ip 监听中")
            .setContentText("ip 监听中")
            .setContentIntent(pendingIntent)
            .build()

        // 使用ServiceCompat替代直接调用startForeground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceCompat.startForeground(
                this,
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(1, notification)
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val nm: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        //然后再重新创建渠道
        //新建channel group ，只管理该group 里面的channel
        nm.createNotificationChannel(
            NotificationChannel(
                channelDefaultImportance,
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
            service.getSharedPreferences().edit { putString(SP_KEY_SERVER_ADDRESS, address) }
            service.reportIp(service.lastIPTemp)
        }

        fun saveAdbTcpipPort(port: String) {
            service.getSharedPreferences().edit { putString(SP_KEY_ADB_TCOIP_PORT, port) }
            service.reportIp(service.lastIPTemp)
        }

        //链接
        fun connect() {
            service.reportIp(service.lastIPTemp)
        }
    }
}
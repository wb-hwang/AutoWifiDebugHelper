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
import com.hwb.wifidebughelper.AppApplication.Companion.DEF_TPICP_PORT
import com.hwb.wifidebughelper.AppApplication.Companion.SP_KEY_ADB_TCOIP_PORT
import com.hwb.wifidebughelper.AppApplication.Companion.SP_KEY_SERVER_ADDRESS
import java.net.Inet4Address
import androidx.core.content.edit

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
        // 使用 ConnectList 获取选中的连接数据
        val selectedData = ConnectList.getSelectId()
        if (selectedData?.serverIp == null) {
            toast("请先选择或添加一个服务器配置")
            return
        } else {
            val serverAddress = selectedData.serverIp
            val serverPort = selectedData.serverPort ?: "5000"
            val tcpPort = selectedData.tcpIp ?: DEF_TPICP_PORT
            val url = "http://$serverAddress:$serverPort/string?address=$ip:$tcpPort"
            ALog.debug("尝试连接URL: $url")
            
            val stringRequest = object : StringRequest(
                Method.GET,
                url,
                { response ->
                    try {
                        ALog.info("请求成功: $response")
                        toast("上报成功：$response")
                    } catch (e: Exception) {
                        ALog.error("解析响应时出错", e)
                        toast("解析响应时出错: ${e.message}")
                    }
                },
                { error ->
                    val errorMsg = if (error.message != null) error.message else "连接失败"
                    val errorClass = error.javaClass.simpleName
                    val errorDetails = "错误类型: $errorClass, 错误信息: $errorMsg"
                    
                    ALog.error("请求失败: $errorDetails")
                    
                    // 处理服务器错误，尝试获取服务器返回的错误信息
                    if (errorClass.contains("ServerError") && error.networkResponse != null) {
                        try {
                            val serverErrorData = String(error.networkResponse.data)
                            ALog.error("服务器返回错误: $serverErrorData")
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
                        ALog.debug("原始响应: $responseData")
                    }
                    return super.parseNetworkResponse(response)
                }
            }
            
            queue.add(stringRequest)
        }
    }

    private fun toast(s: String) {
        Toast.makeText(this, "${getString(R.string.app_name)}:$s", Toast.LENGTH_SHORT).show()
        ALog.info(s)
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
            Intent(this, MainActivity2::class.java).let { notificationIntent ->
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
        
        ALog.info("IP监听服务已启动")
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
        ALog.debug("服务已绑定")
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        ALog.info("IP监听服务已销毁")
    }

    class MyBinder(private val service: MyService) : Binder() {

        // 记录上次配置修改的时间戳，用于防止短时间内重复调用
        private var lastConfigChangeTime = 0L
        private val DEBOUNCE_TIME = 500 // 防抖时间，毫秒

        fun saveServerAddress(address: String) {
            // 检查是否已存在此服务器地址的配置
            val existingData = ConnectList.getList().find { it?.serverIp == address }
            if (existingData != null) {
                // 设置为选中状态
                ConnectList.setSelectId(existingData.id)
            } else {
                // 添加新数据并设置为选中
                ConnectList.add(serverIp = address) { resp ->
                    if (resp.code == ConnectList.ErrorCode.SUCCESS) {
                        ConnectList.setSelectId(resp.data?.id)
                    }
                }
            }
            // 移除：尝试使用新选择的服务器配置进行上报
            // service.reportIp(service.lastIPTemp)
        }

        fun saveAdbTcpipPort(port: String) {
            // 更新当前选中的配置
            val selectedData = ConnectList.getSelectId()
            if (selectedData != null) {
                selectedData.tcpIp = port
                ConnectList.update(selectedData)
                // 移除：尝试使用更新的配置进行上报
                // service.reportIp(service.lastIPTemp)
            } else {
                // 如果没有选中的配置，提示用户
                service.toast("请先选择或添加一个服务器配置")
            }
        }

        //链接
        fun connect() {
            // 添加防抖逻辑
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastConfigChangeTime > DEBOUNCE_TIME) {
                lastConfigChangeTime = currentTime
                
                ALog.info("执行连接请求")
                service.reportIp(service.lastIPTemp)
            } else {
                ALog.info("连接请求被防抖忽略")
            }
        }
    }
}
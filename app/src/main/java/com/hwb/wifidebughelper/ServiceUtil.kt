package com.hwb.wifidebughelper

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hjq.toast.Toaster
import com.tencent.mmkv.MMKV

object ServiceUtil {
    private var serviceBinder: MyService.MyBinder? = null
    private var serviceConnection: ServiceConnection? = null
    private var isBinding = false
    
    // 添加ViewModel引用
    private var viewModel: ConnectListVM? = null
    
    // 用于存储待启动的服务请求
    private var pendingServiceStart: ((Context) -> Unit)? = null
    
    // 设置ViewModel引用
    fun setViewModel(vm: ConnectListVM) {
        viewModel = vm
        ALog.debug("设置ViewModel引用")
    }
    
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            ALog.info("服务连接成功")
            serviceBinder = service as MyService.MyBinder
            
            // 连接成功后，检查是否有当前选中的连接配置，并尝试启动服务
            val selectedData = ConnectList.getSelectId()
            if (selectedData?.serverIp != null && selectedData.tcpIp != null) {
                ALog.info("自动启动：发现有效连接配置 ${selectedData.serverIp}:${selectedData.tcpIp}")
                
                // 更新VM状态为已连接
                updateViewModelState(true)
                
                // 使用优化后的一次性配置并连接的方法
                setupAndConnect(selectedData.serverIp, selectedData.tcpIp)
            } else {
                ALog.info("自动启动：未找到有效连接配置")
            }
            
            isBinding = false
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            ALog.info("服务连接断开")
            serviceBinder = null
            serviceConnection = null
            isBinding = false
            
            // 更新VM状态为未连接
            updateViewModelState(false)
        }
    }

    // 自动启动设置的键名
    const val KEY_AUTO_START = "key_auto_start_service"
    // 默认不自动启动
    private const val DEFAULT_AUTO_START = false

    fun bindService(activity: MainActivity2) {
        ALog.debug("绑定服务到MainActivity2")
        val it = Intent(activity, MyService::class.java)
        activity.bindService(it, conn, BIND_AUTO_CREATE)
        serviceConnection = conn
    }

    // 检查通知权限
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    // 实际启动前台服务的方法
    private fun actuallyStartForegroundService(context: Context) {
        ALog.info("正在启动前台服务")
        val serviceIntent = Intent(context, MyService::class.java)
        
        // 对于Android 8.0及以上版本，使用startForegroundService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    // 从任何Context启动前台服务
    fun startForegroundService(context: Context) {
        // 如果不是Activity，我们无法请求权限，只能尝试启动
        if (context !is Activity) {
            if (hasNotificationPermission(context)) {
                actuallyStartForegroundService(context)
            } else {
                ALog.warning("非Activity上下文，无法请求通知权限")
                Toaster.show("请在应用设置中授予通知权限")
            }
            return
        }
        
        // 检查通知权限
        if (hasNotificationPermission(context)) {
            actuallyStartForegroundService(context)
        } else {
            // 直接请求权限，而不是通过回调
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    // 检查是否应该显示请求权限的理由
                    if (ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.POST_NOTIFICATIONS)) {
                        Toaster.show("需要通知权限以便在后台运行服务")
                    }
                    
                    // 请求权限
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        100
                    )
                    
                    // 保存待执行的操作，在MainActivity2的onRequestPermissionsResult中处理
                    pendingServiceStart = { ctx ->
                        if (hasNotificationPermission(ctx)) {
                            actuallyStartForegroundService(ctx)
                        } else {
                            Toaster.show("通知权限被拒绝，无法启动前台服务")
                        }
                    }
                } catch (e: Exception) {
                    ALog.error("请求权限时出错", e)
                }
            } else {
                // Android 13以下不需要通知权限
                actuallyStartForegroundService(context)
            }
        }
    }

    // 先启动服务再绑定服务
    fun bindAndStartService(context: Context) {
        if (isBinding) {
            ALog.info("服务绑定已在进行中，跳过")
            return
        }
        
        isBinding = true
        
        // 检查并请求权限，然后启动前台服务
        if (context is Activity) {
            if (hasNotificationPermission(context)) {
                actuallyStartForegroundService(context)
                bindServiceInternal(context)
            } else {
                requestNotificationPermission(context) { granted ->
                    if (granted) {
                        actuallyStartForegroundService(context)
                        bindServiceInternal(context)
                    } else {
                        isBinding = false
                    }
                }
            }
        } else {
            // 非Activity上下文，尝试直接启动
            if (hasNotificationPermission(context)) {
                actuallyStartForegroundService(context)
                bindServiceInternal(context)
            } else {
                Toaster.show("请在应用设置中授予通知权限")
                isBinding = false
            }
        }
    }
    
    // 内部绑定服务方法
    private fun bindServiceInternal(context: Context) {
        val it = Intent(context, MyService::class.java)
        val bindResult = context.bindService(it, conn, BIND_AUTO_CREATE)
        serviceConnection = conn
        
        ALog.info("服务绑定结果: $bindResult")
    }

    // 更新ViewModel状态
    private fun updateViewModelState(isConnected: Boolean) {
        viewModel?.let {
            ALog.info("更新ViewModel连接状态: $isConnected")
            it.isConnect.value = isConnected
        }
    }
    
    // 优化后的一次性设置和连接方法
    fun setupAndConnect(address: String?, port: String?) {
        if (!address.isNullOrEmpty() && !port.isNullOrEmpty()) {
            ALog.info("一次性设置并启动连接 地址:$address 端口:$port")
            
            // 先保存所有配置
            var currentData: ConnectData? = ConnectList.getSelectId()
            
            // 检查是否已存在此服务器地址的配置
            val existingData = ConnectList.getList().find { it?.serverIp == address }
            if (existingData != null) {
                // 设置为选中状态
                ConnectList.setSelectId(existingData.id)
                currentData = existingData
            } else {
                // 添加新数据并设置为选中
                ConnectList.add(serverIp = address, tcpIp = port) { resp ->
                    if (resp.code == ConnectList.ErrorCode.SUCCESS) {
                        ConnectList.setSelectId(resp.data?.id)
                        currentData = resp.data
                    }
                }
            }
            
            // 确保端口设置正确
            val dataToUpdate = currentData // 创建一个不可变的引用
            if (dataToUpdate != null && dataToUpdate.tcpIp != port) {
                dataToUpdate.tcpIp = port
                ConnectList.update(dataToUpdate)
            }
            
            // 更新VM状态为已连接
            updateViewModelState(true)
            
            // 最后一次性连接
            serviceBinder?.connect()
        } else {
            ALog.warning("尝试启动服务但地址或端口为空")
        }
    }
    
    // 保留旧方法以兼容现有代码，但改为调用优化后的方法
    fun startService(address: String?, port: String?) {
        if (!address.isNullOrEmpty() && !port.isNullOrEmpty()) {
            ALog.info("启动服务 地址:$address 端口:$port")
            
            // 更新VM状态为已连接
            updateViewModelState(true)
            
            // 调用优化后的方法
            setupAndConnect(address, port)
        } else {
            ALog.warning("尝试启动服务但地址或端口为空")
        }
    }

    fun connect() {
        ALog.info("手动连接请求")
        
        // 更新VM状态为已连接
        updateViewModelState(true)
        
        serviceBinder?.connect()
    }

    // 设置是否在应用启动时自动启动服务
    fun setAutoStartEnabled(enabled: Boolean) {
        ALog.info("设置自动启动: $enabled")
        MMKV.defaultMMKV()?.encode(KEY_AUTO_START, enabled)
    }

    // 检查是否设置了自动启动
    fun isAutoStartEnabled(): Boolean {
        val isEnabled = MMKV.defaultMMKV()?.decodeBool(KEY_AUTO_START, DEFAULT_AUTO_START) ?: DEFAULT_AUTO_START
        ALog.debug("检查自动启动设置: $isEnabled")
        return isEnabled
    }
    
    // 解除服务绑定
    fun unbindService(context: Context) {
        try {
            serviceConnection?.let {
                ALog.info("解除服务绑定")
                context.unbindService(it)
                serviceConnection = null
                isBinding = false
            }
        } catch (e: Exception) {
            ALog.error("解除服务绑定失败", e)
        }
    }

    // 请求通知权限
    private fun requestNotificationPermission(activity: Activity, onPermissionResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 检查权限状态
            val hasPermission = hasNotificationPermission(activity)
            
            if (hasPermission) {
                // 已有权限，直接回调
                onPermissionResult(true)
                return
            }
            
            try {
                // 检查是否应该显示请求权限的理由
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                    Toaster.show("需要通知权限以便在后台运行服务")
                }
                
                // 保存待执行的回调
                pendingServiceStart = { ctx ->
                    onPermissionResult(hasNotificationPermission(ctx))
                }
                
                // 发起权限请求
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            } catch (e: Exception) {
                ALog.error("请求权限时出错", e)
                onPermissionResult(false)
            }
        } else {
            // Android 13以下不需要请求POST_NOTIFICATIONS权限
            onPermissionResult(true)
        }
    }

    // 处理权限请求的结果
    fun handlePermissionResult(granted: Boolean, context: Context) {
        if (granted) {
            pendingServiceStart?.invoke(context)
        } else {
            Toaster.show("通知权限被拒绝，无法启动前台服务")
        }
        pendingServiceStart = null
    }
}
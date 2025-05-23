package com.hwb.wifidebughelper

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.tencent.mmkv.MMKV

object ServiceUtil {
    private var serviceBinder: MyService.MyBinder? = null
    private var serviceConnection: ServiceConnection? = null
    private var isBinding = false
    
    // 添加ViewModel引用
    private var viewModel: ConnectListVM? = null
    
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

    // 从任何Context启动前台服务
    fun startForegroundService(context: Context) {
        ALog.info("正在启动前台服务")
        val serviceIntent = Intent(context, MyService::class.java)
        
        // 对于Android 8.0及以上版本，使用startForegroundService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        ALog.info("前台服务启动命令已发送")
    }

    // 先启动服务再绑定服务
    fun bindAndStartService(context: Context) {
        if (isBinding) {
            ALog.info("服务绑定已在进行中，跳过")
            return
        }
        
        ALog.info("开始绑定和启动服务")
        isBinding = true
        
        // 首先启动前台服务
        startForegroundService(context)
        
        // 然后绑定服务
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
}
package com.hwb.wifidebughelper

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.hjq.toast.Toaster
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Toaster.init(this)
        val rootDir = MMKV.initialize(this)
        
        // 初始化日志系统
        ALog.init(this)
        // 清理一周前的日志
        ALog.cleanupOldLogs(7)
        
        ALog.info("应用启动 - MMKV根目录: $rootDir")
        
        migrateOldDataIfNeeded()
        
        // 检查是否启用了自动启动服务
        if (ServiceUtil.isAutoStartEnabled()) {
            ALog.info("应用启动 - 检测到自动启动设置已开启")
            
            // 使用协程延迟启动服务，以确保应用初始化完成
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    ALog.info("应用启动 - 准备自动启动服务")
                    
                    // 优先启动前台服务，然后再绑定
                    ServiceUtil.startForegroundService(this@AppApplication)
                    
                    // 延迟一点时间再绑定服务，确保服务已启动
                    delay(500)
                    
                    // 绑定服务
                    ServiceUtil.bindAndStartService(this@AppApplication)
                    ALog.info("服务自动启动完成")
                } catch (e: Exception) {
                    ALog.error("自动启动服务失败", e)
                    Toaster.show("自动启动服务失败: ${e.message}")
                }
            }
        } else {
            ALog.info("应用启动 - 自动启动服务未开启")
        }
    }
    
    /**
     * 迁移旧的 SharedPreferences 数据到 ConnectList (MMKV)
     * 只会在首次使用新版本时执行一次
     */
    private fun migrateOldDataIfNeeded() {
        try {
            val sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
            val migrationDone = sharedPref.getBoolean("migration_done", false)
            
            if (!migrationDone) {
                val serverAddress = sharedPref.getString(SP_KEY_SERVER_ADDRESS, null)
                val tcpipPort = sharedPref.getString(SP_KEY_ADB_TCOIP_PORT, DEF_TPICP_PORT)
                
                // 如果有旧数据，则迁移
                if (!serverAddress.isNullOrEmpty()) {
                    ConnectList.add(
                        serverIp = serverAddress,
                        tcpIp = tcpipPort,
                        name = "迁移的配置"
                    ) { resp ->
                        if (resp.code == ConnectList.ErrorCode.SUCCESS) {
                            // 将迁移的配置设为当前选中
                            ConnectList.setSelectId(resp.data?.id)
                            ALog.info("成功迁移旧配置: $serverAddress:$tcpipPort")
                        }
                    }
                }
                
                // 标记迁移已完成
                sharedPref.edit().putBoolean("migration_done", true).apply()
                ALog.info("数据迁移标记为已完成")
            }
        } catch (e: Exception) {
            ALog.error("迁移数据时发生错误", e)
        }
    }

    companion object {
        private const val TAG = "AppApplication"
        const val SP_KEY_SERVER_ADDRESS = "server_address"
        const val SP_KEY_ADB_TCOIP_PORT = "adb_tcpip_port"
        const val DEF_TPICP_PORT = "5555"
    }
}
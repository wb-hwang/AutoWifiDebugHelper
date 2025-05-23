package com.hwb.wifidebughelper

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ALog {
    private const val TAG = "ALog"
    private const val MAX_LOG_FILES = 30 // 最多保留30个日志文件
    
    // 日志级别
    enum class Level {
        DEBUG, INFO, WARNING, ERROR
    }
    
    private var appContext: Context? = null
    private var logDir: File? = null
    private var currentLogFile: File? = null
    private var executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * 初始化日志系统
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        logDir = File(context.getExternalFilesDir(null), "logs")
        if (!logDir!!.exists()) {
            logDir!!.mkdirs()
        }
        
        // 创建当天的日志文件
        val today = dateFormat.format(Date())
        currentLogFile = File(logDir, "log_${today}.txt")
        
        // 记录应用启动日志
        log(Level.INFO, "应用启动")
    }
    
    /**
     * 清理旧日志文件
     * @param daysToKeep 保留多少天内的日志
     */
    fun cleanupOldLogs(daysToKeep: Int = 7) {
        executorService.submit {
            try {
                val now = System.currentTimeMillis()
                val cutoffTime = now - (daysToKeep * 24 * 60 * 60 * 1000L)
                
                val logFiles = logDir?.listFiles { file ->
                    file.isFile && file.name.startsWith("log_") && file.name.endsWith(".txt")
                }
                
                logFiles?.forEach { file ->
                    try {
                        // 从文件名解析日期
                        val dateStr = file.name.substring(4, 14) // 提取"yyyy-MM-dd"部分
                        val fileDate = dateFormat.parse(dateStr)
                        
                        if (fileDate != null && fileDate.time < cutoffTime) {
                            if (file.delete()) {
                                Log.d(TAG, "已删除旧日志文件: ${file.name}")
                            } else {
                                Log.e(TAG, "无法删除日志文件: ${file.name}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "处理日志文件时出错: ${file.name}", e)
                    }
                }
                
                // 如果日志文件数量超过上限，删除最旧的文件
                val remainingFiles = logDir?.listFiles { file ->
                    file.isFile && file.name.startsWith("log_") && file.name.endsWith(".txt")
                }?.sortedBy { it.lastModified() }
                
                if (remainingFiles != null && remainingFiles.size > MAX_LOG_FILES) {
                    val filesToDelete = remainingFiles.take(remainingFiles.size - MAX_LOG_FILES)
                    filesToDelete.forEach { file ->
                        if (file.delete()) {
                            Log.d(TAG, "已删除超额日志文件: ${file.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "清理日志时出错", e)
            }
        }
    }
    
    /**
     * 获取所有日志文件
     */
    fun getLogFiles(): List<File> {
        return logDir?.listFiles { file ->
            file.isFile && file.name.startsWith("log_") && file.name.endsWith(".txt")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    /**
     * 读取日志文件内容
     */
    fun readLogFile(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            "无法读取日志文件: ${e.message}"
        }
    }
    
    /**
     * 删除所有日志文件
     */
    fun clearAllLogs() {
        executorService.submit {
            try {
                logDir?.listFiles { file ->
                    file.isFile && file.name.startsWith("log_") && file.name.endsWith(".txt")
                }?.forEach { file ->
                    file.delete()
                }
                
                // 创建新的当天日志文件
                val today = dateFormat.format(Date())
                currentLogFile = File(logDir, "log_${today}.txt")
                
                Log.d(TAG, "已清除所有日志文件")
            } catch (e: Exception) {
                Log.e(TAG, "清除日志时出错", e)
            }
        }
    }
    
    /**
     * 记录日志
     */
    fun debug(msg: String) {
        log(Level.DEBUG, msg)
    }
    
    fun info(msg: String) {
        log(Level.INFO, msg)
    }
    
    fun warning(msg: String) {
        log(Level.WARNING, msg)
    }
    
    fun error(msg: String) {
        log(Level.ERROR, msg)
    }
    
    fun error(msg: String, throwable: Throwable) {
        log(Level.ERROR, "$msg: ${throwable.message}")
        Log.e(TAG, msg, throwable)
    }
    
    // 兼容旧的调用方式
    fun log(msg: String) {
        log(Level.INFO, msg)
    }
    
    /**
     * 记录日志，带级别
     */
    fun log(level: Level, msg: String) {
        // 打印到LogCat
        when (level) {
            Level.DEBUG -> Log.d(TAG, msg)
            Level.INFO -> Log.i(TAG, msg)
            Level.WARNING -> Log.w(TAG, msg)
            Level.ERROR -> Log.e(TAG, msg)
        }
        
        // 写入文件
        writeToFile(level, msg)
    }
    
    /**
     * 将日志写入文件
     */
    private fun writeToFile(level: Level, msg: String) {
        if (appContext == null || logDir == null) {
            Log.e(TAG, "日志系统未初始化")
            return
        }
        
        executorService.submit {
            try {
                // 确保使用当天的日志文件
                val today = dateFormat.format(Date())
                val todayFile = File(logDir, "log_${today}.txt")
                if (!todayFile.exists() || currentLogFile?.absolutePath != todayFile.absolutePath) {
                    currentLogFile = todayFile
                }
                
                val timestamp = timeFormat.format(Date())
                val logEntry = "[$timestamp][${level.name}] $msg\n"
                
                FileWriter(currentLogFile, true).use { fileWriter ->
                    PrintWriter(fileWriter).use { printWriter ->
                        printWriter.write(logEntry)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "写入日志文件时出错", e)
            }
        }
    }
}
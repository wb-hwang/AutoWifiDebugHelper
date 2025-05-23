package com.hwb.wifidebughelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hjq.toast.Toaster
import com.hwb.wifidebughelper.ui.theme.Background
import com.hwb.wifidebughelper.ui.theme.Blue40
import com.hwb.wifidebughelper.ui.theme.CardBackground
import com.hwb.wifidebughelper.ui.theme.OnBackground
import com.hwb.wifidebughelper.ui.theme.WifiDebugHelperTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WifiDebugHelperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    LogScreen(
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onBackPressed: () -> Unit) {
    val logFiles = remember { mutableStateListOf<File>() }
    var showLogContentDialog by remember { mutableStateOf(false) }
    var showClearLogsConfirmDialog by remember { mutableStateOf(false) }
    var selectedLogFile by remember { mutableStateOf<File?>(null) }
    var selectedLogContent by remember { mutableStateOf("") }
    
    // 加载日志文件
    LaunchedEffect(key1 = Unit) {
        refreshLogFiles(logFiles)
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TopAppBar(
                title = { Text("日志查看", color = Color.White) },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier
                            .padding(12.dp)
                            .clickable { onBackPressed() },
                        tint = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Blue40
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showClearLogsConfirmDialog = true },
                containerColor = Blue40,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "清除日志"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Background)
        ) {
            if (logFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "没有日志文件",
                        color = OnBackground,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(logFiles) { file ->
                        LogFileItem(file) {
                            selectedLogFile = file
                            selectedLogContent = ALog.readLogFile(file)
                            showLogContentDialog = true
                        }
                    }
                }
            }
            
            // 日志内容对话框
            if (showLogContentDialog && selectedLogFile != null) {
                AlertDialog(
                    onDismissRequest = { showLogContentDialog = false },
                    title = {
                        val dateStr = try {
                            selectedLogFile?.name?.substring(4, 14) ?: "日志"
                        } catch (e: Exception) {
                            "日志"
                        }
                        Text("$dateStr 日志")
                    },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                        ) {
                            LazyColumn {
                                val lines = selectedLogContent.split("\n")
                                items(lines) { line ->
                                    Text(
                                        text = line,
                                        fontSize = 14.sp,
                                        color = when {
                                            line.contains("[ERROR]") -> Color.Red
                                            line.contains("[WARNING]") -> Color(0xFFFF9800)
                                            line.contains("[DEBUG]") -> Color(0xFF4CAF50)
                                            else -> OnBackground
                                        }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLogContentDialog = false }) {
                            Text("关闭")
                        }
                    }
                )
            }
            
            // 清除日志确认对话框
            if (showClearLogsConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showClearLogsConfirmDialog = false },
                    title = { Text("清除日志") },
                    text = { Text("确定要清除所有日志文件吗？") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                ALog.clearAllLogs()
                                Toaster.show("已清除所有日志")
                                refreshLogFiles(logFiles)
                                showClearLogsConfirmDialog = false
                            }
                        ) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showClearLogsConfirmDialog = false }
                        ) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LogFileItem(file: File, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    val dateStr = try {
        file.name.substring(4, 14) // 提取"yyyy-MM-dd"部分
    } catch (e: Exception) {
        file.name
    }
    
    val date = try {
        dateFormat.parse(dateStr)
    } catch (e: Exception) {
        Date(file.lastModified())
    }
    
    val formattedDate = dateFormat.format(date ?: Date(file.lastModified()))
    val formattedTime = timeFormat.format(Date(file.lastModified()))
    val fileSize = String.format("%.2f KB", file.length() / 1024.0)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "日期: $formattedDate",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = OnBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(
                    text = "修改时间: $formattedTime",
                    fontSize = 14.sp,
                    color = OnBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "大小: $fileSize",
                    fontSize = 14.sp,
                    color = OnBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun refreshLogFiles(logFiles: MutableList<File>) {
    logFiles.clear()
    logFiles.addAll(ALog.getLogFiles())
} 
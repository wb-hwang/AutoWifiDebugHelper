package com.hwb.wifidebughelper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {


                Text(text = "使用说明", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "1.将手机用数据线连接到pc", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "2.pc 执行命令：adb tcpip {端口号默认：5555}", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "3.pc 运行对应python 脚本", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "4.拔掉数据线，app 设置服务器地址", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))

                Text(text = "5.后续app 会自动监听并链接", modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))

            }
        }
    }
}

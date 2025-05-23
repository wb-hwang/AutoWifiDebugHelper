package com.hwb.wifidebughelper

import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.hjq.toast.Toaster
import androidx.compose.foundation.BorderStroke
import com.hwb.wifidebughelper.ui.theme.Background
import com.hwb.wifidebughelper.ui.theme.Blue40
import com.hwb.wifidebughelper.ui.theme.CardBackground
import com.hwb.wifidebughelper.ui.theme.ItemDefault
import com.hwb.wifidebughelper.ui.theme.OnBackground
import com.hwb.wifidebughelper.ui.theme.SelectedItem
import com.hwb.wifidebughelper.ui.theme.Success
import com.hwb.wifidebughelper.ui.theme.Error as ErrorColor
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

class MainListActivity : ComponentActivity() {
    // 空类实现，实际UI在Compose函数中
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainListActivity(navController: NavHostController) {
    val viewModel = viewModel<ConnectListVM>(LocalContext.current as ComponentActivity)
    val context = LocalContext.current
    var showDialog: Boolean by remember { mutableStateOf(false) }
    var changeData: ConnectData? by remember { mutableStateOf(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<String?>(null) }
    
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(Background)
            .padding(16.dp)
    ) {
        val (title, add, list, tipText) = createRefs()

        Text(
            "WiFi调试助手",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Blue40,
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(parent.top, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .clickable {
                    navController.popBackStack()
                }
        )

        Button(
            onClick = {
                changeData = null
                showDialog = true
            },
            modifier = Modifier.constrainAs(add) {
                top.linkTo(title.top)
                bottom.linkTo(title.bottom)
                end.linkTo(parent.end, margin = 16.dp)
            },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Blue40
            )
        ) {
            Text("添加")
        }

        CustomDialog(showDialog, changeData, {
            showDialog = false
        })

        if (showDeleteConfirmDialog) {
            ConfirmDialog(
                title = "确认删除",
                message = "确定要删除这个服务器配置吗？",
                onConfirm = {
                    itemToDelete?.let { id ->
                        viewModel.removeItem(id)
                    }
                    showDeleteConfirmDialog = false
                    itemToDelete = null
                },
                onDismiss = {
                    showDeleteConfirmDialog = false
                    itemToDelete = null
                }
            )
        }

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .constrainAs(list) {
                    top.linkTo(title.bottom, margin = 24.dp)
                    bottom.linkTo(tipText.top)
                    height = Dimension.fillToConstraints
                }, verticalArrangement = Arrangement.Top
        ) {
            itemsIndexed(
                viewModel._items,
                key = { index, item -> item?.serverIp!! }) { index, item ->
                ConnectItem(
                    connectData = item,
                    onEdit = {
                        changeData = item
                        showDialog = true
                    },
                    onDelete = {
                        itemToDelete = item?.id
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }
        
        Text(
            text = "提示：长按条目可编辑或删除",
            color = OnBackground.copy(alpha = 0.6f),
            fontSize = 14.sp,
            modifier = Modifier
                .constrainAs(tipText) {
                    bottom.linkTo(parent.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectItem(connectData: ConnectData?, onEdit: () -> Unit, onDelete: () -> Unit) {
    val viewModel = viewModel<ConnectListVM>(LocalContext.current as ComponentActivity)
    val isSelected = viewModel._item.value?.id == connectData?.id
    val backgroundColor = animateColorAsState(
        targetValue = if (isSelected) SelectedItem else Color(0xFF455A64),
        label = "backgroundColor"
    )
    
    // 添加菜单展开状态
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                // 使用combinedClickable添加长按功能
                .combinedClickable(
                    onClick = {
                        viewModel._item.value = connectData
                        ConnectList.setSelectId(connectData?.id)
                        viewModel.isConnect.value = true
                    },
                    onLongClick = {
                        showMenu = true
                    }
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = backgroundColor.value
            ),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (connectData?.name.isNullOrEmpty()) 
                        "服务器：${connectData?.serverIp}" 
                    else 
                        "服务器：${connectData?.name}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!connectData?.name.isNullOrEmpty()) {
                    Text(
                        text = "地址：${connectData?.serverIp}:${connectData?.serverPort}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "tcp端口：${connectData?.tcpIp}",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        
        // 下拉菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(androidx.compose.material3.CardDefaults.cardColors().containerColor)
                .padding(vertical = 4.dp)
        ) {
            DropdownMenuItem(
                text = { Text("编辑") },
                onClick = {
                    showMenu = false
                    onEdit()
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
            )
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            )
        }
    }
}

@Composable
fun CustomDialog(showDialog: Boolean, data: ConnectData?, onDismiss: () -> Unit) {
    val viewModel = viewModel<ConnectListVM>(LocalContext.current as ComponentActivity)
    if (showDialog) {
        var serverName by remember { mutableStateOf(if (data == null) "" else data.name ?: "") }
        var serverLink by remember { mutableStateOf(if (data == null) "" else data.serverIp ?: "") }
        var serverPort by remember { mutableStateOf(if (data == null) "5000" else data.serverPort ?: "5000") }
        var tcpIpLink by remember { mutableStateOf(if (data == null) "5555" else data.tcpIp ?: "5555") }
        Dialog(onDismissRequest = onDismiss) {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(16.dp),
                color = com.hwb.wifidebughelper.ui.theme.Surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (data == null) "添加连接" else "编辑连接",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue40
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "名称（可选）",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnBackground
                    )
                    TextField(
                        value = serverName,
                        onValueChange = {
                            serverName = it
                        },
                        placeholder = { Text("服务器名称") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Background,
                            focusedContainerColor = Background,
                            cursorColor = Blue40,
                            focusedIndicatorColor = Blue40,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "服务器地址",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnBackground
                    )
                    TextField(
                        value = serverLink,
                        onValueChange = {
                            serverLink = it
                        },
                        placeholder = { Text("192.168.10.10") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Background,
                            focusedContainerColor = Background,
                            cursorColor = Blue40,
                            focusedIndicatorColor = Blue40,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "服务器端口（可选）",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnBackground
                    )
                    TextField(
                        value = serverPort,
                        onValueChange = {
                            serverPort = it
                        },
                        placeholder = { Text("5000") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Background,
                            focusedContainerColor = Background,
                            cursorColor = Blue40,
                            focusedIndicatorColor = Blue40,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ADB TCPIP端口",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnBackground
                    )
                    TextField(
                        value = tcpIpLink,
                        onValueChange = {
                            tcpIpLink = it
                        },
                        placeholder = { Text("5555") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Background,
                            focusedContainerColor = Background,
                            cursorColor = Blue40,
                            focusedIndicatorColor = Blue40,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onDismiss,
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = Blue40
                            ),
                            border = BorderStroke(1.dp, Blue40)
                        ) {
                            Text("取消")
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        androidx.compose.material3.Button(
                            onClick = {
                                if (serverLink.isNullOrBlank()) {
                                    Toaster.show("请填写服务器地址")
                                    return@Button
                                }
                                if (data == null) {
                                    viewModel.addItem(serverLink, tcpIpLink, serverName.ifEmpty { null }, serverPort)
                                    Toaster.show("添加成功")
                                } else {
                                    data.serverIp = serverLink
                                    data.tcpIp = tcpIpLink
                                    data.name = serverName.ifEmpty { null }
                                    data.serverPort = serverPort
                                    viewModel.updateItem(data)
                                    Toaster.show("修改成功")
                                }
                                onDismiss()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Blue40
                            )
                        ) {
                            Text(if (data == null) "添加" else "保存")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    // 预览暂未实现
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(16.dp),
            color = com.hwb.wifidebughelper.ui.theme.Surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Blue40
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = OnBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Blue40
                        ),
                        border = BorderStroke(1.dp, Blue40)
                    ) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    androidx.compose.material3.Button(
                        onClick = onConfirm,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Blue40
                        )
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
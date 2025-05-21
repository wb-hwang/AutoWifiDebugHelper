package com.hwb.wifidebughelper

import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.hjq.toast.Toaster

class MainListActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            WifiDebugHelperTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    ConstraintLayoutContent(
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
//        ServiceUtil.bindService(this)
//    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainListActivity(navController: NavHostController) {
    val viewModel = viewModel<ConnectListVM>(LocalContext.current as ComponentActivity)
    val context = LocalContext.current
    var showDialog: Boolean by remember { mutableStateOf(false) }
    var changeData: ConnectData? by remember { mutableStateOf(null) }
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        // Create references for the composables to constrain
        val (title, add, list) = createRefs()

        Text(
            "WiFiDebugHelper",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(parent.top, margin = 50.dp)
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
            }
        ) {
            Text("添加")
        }

        CustomDialog(showDialog, changeData, {
            showDialog = false
        })

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .constrainAs(list) {
                    top.linkTo(title.bottom, margin = 20.dp)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.fillToConstraints
                }, verticalArrangement = Arrangement.Top
        ) {
//            items(viewModel._items) { item ->
            //items务必添加key，否则会造成显示错乱
            itemsIndexed(viewModel._items, key = { index, item -> item?.serverIp!! }) { index, item ->
                SwipeToDismiss(
                    //添加移除时的动画
                    modifier = Modifier.animateItemPlacement(),
                    content = {
                        ConnectItem(item)
                    },
                    onDelete = {
//                        data.remove(data.find { it.id == item.id })
                        viewModel.removeItem(item?.id)
                    },
                    onChange = {
                        changeData = item
                        showDialog = true
//                        Toaster.show("onChange")
//                        data[data.indexOf(data.find { it.id == item.id })] = item.copy(title = "Item has change: ${item.id}")
                    }
                )
//            }


            }
        }

    }
}

//使用material3自带的SwipeToDismissBox，滑动后放手松开立即执行
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismiss(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
    onDelete: () -> Unit,
    onChange: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { //滑动后放手会执行
                onDelete()
                return@rememberSwipeToDismissBoxState true
            }
            if (it == SwipeToDismissBoxValue.StartToEnd) { //滑动后放手会执行
                onChange()
            }
            return@rememberSwipeToDismissBoxState false
        }, positionalThreshold = { //滑动到什么位置会改变状态，滑动阈值
            it / 4
        })
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
//            .padding(4.dp)
            .fillMaxWidth(),
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> Color.Green
                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                    else -> Color.LightGray
                }, label = ""
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "",
                        modifier = Modifier
                    )
                else
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "",
                        modifier = Modifier
                    )
            }
        },
        content = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center,
                content = content
            )
        })
}

@Composable
fun ConnectItem(connectData: ConnectData?) {
    val viewModel = viewModel<ConnectListVM>(LocalContext.current as ComponentActivity)
    val dismissState = rememberSwipeToDismissBoxState()
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                viewModel._item.value = connectData
                ConnectList.setSelectId(connectData?.id)
                if (viewModel.isConnect.value) {
                    ServiceUtil.startService(connectData?.serverIp, connectData?.tcpIp)
                }
            })
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .background(
                if (viewModel._item.value?.id == connectData?.id) Color(0xFF082259) else Color(0xFF656565),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(start = 26.dp, end = 26.dp, top = 20.dp, bottom = 20.dp)
    ) {
        val (server, port) = createRefs()
        Text(
            text = "服务器：${connectData?.serverIp}",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(server) {
                top.linkTo(parent.top)
            }
        )
        Text(
            text = "tcp端口：${connectData?.tcpIp}",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(port) {
                top.linkTo(server.bottom)
            }
        )
    }
//    ConstraintLayout(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable(onClick = {
//                viewModel._item.value = connectData?.serverIp
//            })
//            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
//            .background(
//                if (viewModel._item.value == connectData?.serverIp) Color(0xFF082259) else Color(0xFF656565), shape = RoundedCornerShape(6.dp)
//            )
//            .padding(26.dp)
//    ) {
//        val (server, port, list) = createRefs()
//        Text(
//            text = "服务器：" + connectData?.serverIp,
//            color = Color.White,
//            fontSize = 16.sp,
//            modifier = Modifier.constrainAs(server) {
//                top.linkTo(parent.top)
//            }
//        )
//        Text(
//            "tcp端口：" + connectData?.serverIp,
//            color = Color.White,
//            fontSize = 16.sp,
//            modifier = Modifier.constrainAs(port) {
//                top.linkTo(server.bottom)
//            })
//    }
}

@Composable
fun CustomDialog(showDialog: Boolean, data: ConnectData?, onDismiss: () -> Unit) {
    val viewModel = viewModel<ConnectListVM>(LocalContext.current as ComponentActivity)
    if (showDialog) {
        var serverLink by remember { mutableStateOf(if (data == null) "" else data.serverIp) }
        var tcpIpLink by remember { mutableStateOf(if (data == null) "" else data.tcpIp) }
        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "添加连接", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "设置服务器地址", modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))

                    TextField(value = serverLink ?: "", onValueChange = {
                        serverLink = it
                    }, placeholder = { Text(text = "192.168.10.10:5000") })

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "设置adb tcpip对应端口", modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(value = tcpIpLink ?: "", onValueChange = {
                        tcpIpLink = it
                    }, placeholder = { Text(text = "5555") })
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(onClick = {
                            if (serverLink.isNullOrBlank() || tcpIpLink.isNullOrBlank()) {
                                Toaster.show("请填写完整")
                                return@Button
                            }
                            if (data == null) {
                                // 添加
                                Toaster.show("添加成功")
                                viewModel.addItem(serverLink, tcpIpLink)
                            } else {
                                // 修改
                                Toaster.show("修改成功")
                                data.serverIp = serverLink
                                data.tcpIp = tcpIpLink
                                viewModel.updateItem(data)
                            }
                            onDismiss()
                        }) {
                            Text("添加")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onDismiss) {
                            Text("关闭")
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
//    WifiDebugHelperTheme {
//        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//            ConstraintLayoutContent(
//                name = "Android",
//                modifier = Modifier.padding(innerPadding)
//            )
//        }
//    }
}
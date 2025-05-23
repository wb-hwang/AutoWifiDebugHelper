package com.hwb.wifidebughelper

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.wifidebughelper.R
import com.hwb.wifidebughelper.ui.theme.*
import com.hjq.toast.Toaster

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "connect") {
        navigation(startDestination = "Main2Activity", route = "connect") {
            composable("Main2Activity") {
                Main2Activity(navController)
            }
            composable("MainListActivity") {
                MainListActivity(navController)
            }
        }
    }
}

@SuppressLint("SetTextI18n")
class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WifiDebugHelperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    AppNavigation()
                }
            }
        }
        
        // 先绑定服务
        ServiceUtil.bindService(this)
        
        // 获取ViewModel并设置到ServiceUtil
        val viewModel = ViewModelProvider(this).get(ConnectListVM::class.java)
        ServiceUtil.setViewModel(viewModel)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 解绑服务，避免内存泄漏
        ServiceUtil.unbindService(this)
    }
}

@Composable
fun Main2Activity(navController: NavHostController) {
    val viewModel = viewModel<ConnectListVM>(LocalContext.current as ComponentActivity)
    var showHelpDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // 获取当前自动启动设置状态
    var autoStartEnabled by remember { mutableStateOf(ServiceUtil.isAutoStartEnabled()) }
    
    // 确保ServiceUtil可以访问到viewModel
    LaunchedEffect(viewModel) {
        ServiceUtil.setViewModel(viewModel)
        
        // 如果启用了自动启动，默认设置isConnect为true
        if (ServiceUtil.isAutoStartEnabled() && ConnectList.getSelectId() != null) {
            viewModel.isConnect.value = true
        }
    }
    
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(Background)
    ) {
        val (logo, title, connectCard, configCard, itemLog, itemSet, itemHelp, itemAbout, backupConnectButton, itemAutoStart) = createRefs()
        Image(
            painterResource(R.mipmap.logo),
            modifier = Modifier
                .constrainAs(logo) {
                    top.linkTo(parent.top, 20.dp)
                    start.linkTo(parent.start, 20.dp)
                }
                .clip(RoundedCornerShape(8.dp))
                .width(50.dp)
                .height(50.dp),
            contentDescription = "",
        )
        Text("Pair Helper", modifier = Modifier.constrainAs(title) {
            top.linkTo(logo.top)
            bottom.linkTo(logo.bottom)
            start.linkTo(logo.end, 20.dp)
        }, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Blue40)

        Card(
            modifier = Modifier
                .constrainAs(connectCard) {
                    top.linkTo(logo.bottom, 20.dp)
                    start.linkTo(parent.start, 16.dp)
                    if (viewModel.isConnect.value) {
                        end.linkTo(backupConnectButton.start, 8.dp)
                    } else {
                        end.linkTo(parent.end, 16.dp)
                    }
                    width = Dimension.fillToConstraints
                }
                .clickable(onClick = {
                    viewModel.isConnect.value = !viewModel.isConnect.value
                    if (viewModel.isConnect.value) {
                        ServiceUtil.startService(viewModel._item.value?.serverIp, viewModel._item.value?.tcpIp)
                    }
                }),
            colors = CardDefaults.cardColors(
                containerColor = if (viewModel.isConnect.value) SelectedItem else Color(0xFF455A64)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            ConstraintLayout(
                modifier = Modifier.padding(20.dp)
            ) {
                val (icon, server, port) = createRefs()
                Icon(
                    if (viewModel.isConnect.value) Icons.Default.Done else Icons.Default.Close,
                    contentDescription = "",
                    modifier = Modifier
                        .constrainAs(icon) {
                            top.linkTo(parent.top)
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                        },
                    tint = Color.White,
                )
                Text(
                    text = if (viewModel.isConnect.value) "运行中" else "已停止",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.constrainAs(server) {
                        top.linkTo(parent.top)
                        start.linkTo(icon.end, margin = 10.dp)
                    }
                )
                Text(
                    text = if (viewModel.isConnect.value) "点击停止" else "点击启动",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.constrainAs(port) {
                        top.linkTo(server.bottom)
                        start.linkTo(server.start)
                    }
                )
            }
        }

        // 添加备用连接按钮在启动按钮右侧
        if (viewModel.isConnect.value) {
            Card(
                modifier = Modifier
                    .constrainAs(backupConnectButton) {
                        top.linkTo(logo.bottom, 20.dp)
                        end.linkTo(parent.end, 16.dp)
                        bottom.linkTo(connectCard.bottom)
                        height = Dimension.fillToConstraints
                        width = Dimension.value(80.dp)
                    }
                    .clickable {
                        ServiceUtil.connect()
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Blue40
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "连接",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .constrainAs(configCard) {
                    top.linkTo(connectCard.bottom, 16.dp)
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    width = Dimension.fillToConstraints
                }
                .clickable(onClick = {
                    navController.navigate("MainListActivity")
                }),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            ConnectConfig(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
        }

        MainItem(
            "帮助", Icons.Default.Info,
            modifier = Modifier
                .constrainAs(itemHelp) {
                    top.linkTo(configCard.bottom, 16.dp)
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    width = Dimension.fillToConstraints
                }
                .clickable {
                    showHelpDialog = true
                }
                .padding(horizontal = 26.dp, vertical = 15.dp),
            tint = Blue40
        )

        MainItem(
            "日志", Icons.Default.DateRange,
            modifier = Modifier
                .constrainAs(itemLog) {
                    top.linkTo(itemHelp.bottom, 8.dp)
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    width = Dimension.fillToConstraints
                }
                .clickable {
                    // 跳转到日志查看界面
                    val intent = Intent(context, LogActivity::class.java)
                    context.startActivity(intent)
                }
                .padding(horizontal = 26.dp, vertical = 15.dp),
            tint = Blue40
        )
        
        if (showHelpDialog) {
            HelpDialog({ showHelpDialog = false })
        }
    }
}

@Composable
fun MainItem(itemName: String, icon: ImageVector, modifier: Modifier, tint: Color = OnBackground) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = "",
            modifier = Modifier
                .padding(start = 10.dp, end = 16.dp)
                .size(28.dp),
            tint = tint,
        )
        Text(itemName, color = OnBackground, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ConnectConfig(modifier: Modifier) {
    val viewModel = viewModel<ConnectListVM>(LocalContext.current as ComponentActivity)
    ConstraintLayout(modifier = modifier) {
        val (icon, server, port) = createRefs()
        Icon(
            Icons.Default.Menu,
            contentDescription = "",
            modifier = Modifier
                .constrainAs(icon) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                },
            tint = Blue40
        )
        
        // 优先显示名称，如果有的话
        val serverText = when {
            !viewModel._item.value?.name.isNullOrEmpty() -> 
                "服务器：${viewModel._item.value?.name}"
            !viewModel._item.value?.serverIp.isNullOrEmpty() -> 
                "服务器：${viewModel._item.value?.serverIp}"
            else -> 
                "未选择服务器配置"
        }
        
        // 如果有名称，同时显示IP地址
        val addressText = if (!viewModel._item.value?.name.isNullOrEmpty() && !viewModel._item.value?.serverIp.isNullOrEmpty()) {
            "${viewModel._item.value?.serverIp}:${viewModel._item.value?.serverPort}"
        } else {
            ""
        }
            
        val portText = if (viewModel._item.value?.tcpIp.isNullOrEmpty()) 
            "点击进行配置" 
        else 
            "tcp端口：${viewModel._item.value?.tcpIp}"
            
        Text(
            text = serverText,
            color = OnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.constrainAs(server) {
                top.linkTo(parent.top)
                start.linkTo(icon.end, margin = 10.dp)
            }
        )
        
        // 如果有地址信息，显示在第二行，否则显示端口信息
        Text(
            text = if (addressText.isNotEmpty()) addressText else portText,
            color = OnBackground,
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(port) {
                top.linkTo(server.bottom, 4.dp)
                start.linkTo(server.start)
            }
        )
        
        // 如果有地址信息，在第三行显示端口信息
        if (addressText.isNotEmpty()) {
            Text(
                text = portText,
                color = OnBackground,
                fontSize = 16.sp,
                modifier = Modifier
                    .constrainAs(createRef()) {
                        top.linkTo(port.bottom, 4.dp)
                        start.linkTo(server.start)
                    }
            )
        }
    }
}
package com.hwb.wifidebughelper

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.example.wifidebughelper.R
import com.hwb.wifidebughelper.ui.theme.WifiDebugHelperTheme

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
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
        ServiceUtil.bindService(this)
    }
}

@Composable
fun Main2Activity(navController: NavHostController) {
    val viewModel = viewModel<ConnectListVM>(LocalContext.current as ComponentActivity)
    var showHelpDialog by remember { mutableStateOf(false) }
    ConstraintLayout(modifier = Modifier) {
        val (logo, title, connectCard, configCard, itemLog, itemSet, itemHelp, itemAbout) = createRefs()
        Image(
            painterResource(R.mipmap.logo),
            modifier = Modifier
                .constrainAs(logo) {
                    top.linkTo(parent.top, 20.dp)
                    start.linkTo(parent.start, 20.dp)
                }
                .clip(RoundedCornerShape(6.dp))
                .width(50.dp)
                .height(50.dp),
            contentDescription = "",
        )
        Text("Pair Helper", modifier = Modifier.constrainAs(title) {
            top.linkTo(logo.top)
            bottom.linkTo(logo.bottom)
            start.linkTo(logo.end, 20.dp)
        }, fontSize = 20.sp, color = Color.Black)

        ConstraintLayout(
            modifier = Modifier
                .constrainAs(connectCard) {
                    top.linkTo(logo.bottom, 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth()
                .clickable(onClick = {
                    viewModel.isConnect.value = !viewModel.isConnect.value
                    if (viewModel.isConnect.value) {
                        ServiceUtil.startService(viewModel._item.value?.serverIp, viewModel._item.value?.tcpIp)
                    }
                    // viewModel._item.value = connectData
                    // ConnectList.setSelectId(connectData?.id)
                    // ServiceUtil.startService(connectData?.serverIp, connectData?.tcpIp)
                })
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .background(
                    if (viewModel.isConnect.value) Color(0xFF082259) else Color(0xFF656565),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(start = 26.dp, end = 26.dp, top = 20.dp, bottom = 20.dp)
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
//                                    .border(1.dp, Color.White, RoundedCornerShape(10.dp)),

            )
            Text(
                text = if (viewModel.isConnect.value) "运行中" else "已停止",
                color = Color.White,
                fontSize = 18.sp,
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
        ConnectConfig(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                }
                .constrainAs(configCard) {
                    top.linkTo(connectCard.bottom, 12.dp)
                    start.linkTo(parent.start, 16.dp)
                    end.linkTo(parent.end, 16.dp)
                    width = Dimension.fillToConstraints
                }
                .clickable(onClick = {
                    navController.navigate("MainListActivity")
                    //startActivity(Intent(this@MainActivity2, MainListActivity::class.java))

                })
                .background(
                    Color(0xFFDEDEDE),
                    shape = RoundedCornerShape(10.dp)
                )
                .shadow(4.dp, RoundedCornerShape(10.dp), clip = true, Color(0xFFDEDEDE), Color(0xFFDEDEDE))
                .padding(start = 26.dp, end = 26.dp, top = 20.dp, bottom = 20.dp)

        )

        MainItem(
            "帮助", Icons.Default.Info,
            modifier = Modifier
                .constrainAs(itemHelp) {
                    top.linkTo(configCard.bottom, 12.dp)
                }
                .fillMaxWidth()
                .clickable {
                    showHelpDialog = true
                }
                .padding(start = 26.dp, end = 26.dp, top = 15.dp, bottom = 15.dp),
        )

        MainItem(
            "日志", Icons.Default.DateRange,
            modifier = Modifier
                .constrainAs(itemLog) {
                    top.linkTo(itemHelp.bottom)
                }
                .fillMaxWidth()
                .clickable {

                }
                .padding(start = 26.dp, end = 26.dp, top = 15.dp, bottom = 15.dp),
        )
        if (showHelpDialog) {
            HelpDialog({ showHelpDialog = false })
        }

    }
}

@Composable
fun MainItem(itemName: String, icon: ImageVector, modifier: Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = "",
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp)
                .size(32.dp),
            tint = Color(0xFF656565),
        )
        Text(itemName, color = Color(0xFF656565), fontSize = 20.sp)
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
                }
//                .border(1.dp, Color.White, RoundedCornerShape(10.dp))
            , tint = Color(0xFF656565)
        )
        Text(
            text = "配置",
            color = Color(0xFF656565),
            fontSize = 18.sp,
            modifier = Modifier.constrainAs(server) {
                top.linkTo(parent.top)
                start.linkTo(icon.end, margin = 10.dp)
            }
        )
        Text(
            text = viewModel._item.value?.serverIp ?: "未选择",
            color = Color(0xFF656565),
            fontSize = 18.sp,
            modifier = Modifier.constrainAs(port) {
                top.linkTo(server.bottom)
                start.linkTo(server.start)
            }
        )
    }
}
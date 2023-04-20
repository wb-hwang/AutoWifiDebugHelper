package com.hwb.wifidebughelper

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wifidebughelper.databinding.ActivityMainBinding
import com.qmuiteam.qmui.widget.dialog.QMUIDialog

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var serviceBinder: MyService.MyBinder? = null
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBinder = service as MyService.MyBinder
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
        }
    }

    private fun getSharedPreferences(): SharedPreferences {
        return super.getSharedPreferences(packageName, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val it = Intent(this, MyService::class.java)
        bindService(it, conn, BIND_AUTO_CREATE)

        initListener()
    }

    private fun initListener() {
        binding.reportIpAndPort.setOnClickListener {
            val serverAddress =
                getSharedPreferences().getString(SP_KEY_SERVER_ADDRESS, null)
            val builder = QMUIDialog.EditTextDialogBuilder(this)
                .setTitle("设置服务器地址")
                .setPlaceholder("192.168.10.10:5000")
                //输入类型为ip端口
                .setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .setDefaultText(serverAddress)
                .addAction("取消") { dialog, _ ->
                    dialog.dismiss()
                }
            builder.addAction("确定") { dialog, _ ->
                val text = builder.editText.text.trim().toString()
                if (text.isNotEmpty()) {
                    dialog.dismiss()
                    toast("设置成功")
                    serviceBinder?.saveServerAddress(text)
                } else {
                    toast("请输入服务器地址")
                }
            }
                .create()
                .show()
        }

        binding.reportAdbTcpipPort.setOnClickListener {
            val tcpipPort =
                getSharedPreferences().getString(SP_KEY_ADB_TCOIP_PORT, DEF_TPICP_PORT)
            val builder = QMUIDialog.EditTextDialogBuilder(this)
                .setTitle("设置adb tcpip对应端口")
                .setPlaceholder("设置adb tcpip对应端口")
                //输入类型为ip端口
                .setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .setDefaultText(tcpipPort)
                .addAction("取消") { dialog, _ ->
                    dialog.dismiss()
                }
            builder.addAction("确定") { dialog, _ ->
                val text = builder.editText.text.trim().toString()
                if (text.isNotEmpty()) {
                    dialog.dismiss()
                    toast("设置成功")
                    serviceBinder?.saveAdbTcpipPort(text)
                } else {
                    toast("请输入adb tcpip对应端口")
                }
            }
                .create()
                .show()
        }
        binding.btnConnect.setOnClickListener {
            serviceBinder?.connect()
        }
    }

    private fun toast(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
        Log.d("toast", s)
    }

    companion object {
        const val SP_KEY_SERVER_ADDRESS = "server_address"
        const val SP_KEY_ADB_TCOIP_PORT = "adb_tcpip_port"
        const val DEF_TPICP_PORT = "5555"
    }

}
package com.hwb.wifidebughelper

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

class ConnectData(
    id: String? = UUID.randomUUID().toString(),
    serverIp: String?,
    tcpIp: String?
) : Parcelable {
    var id by mutableStateOf(id)
    var serverIp by mutableStateOf(serverIp)
    var tcpIp by mutableStateOf(tcpIp)

    // 通常返回 0，表示没有特殊标志
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id) // 写入 id
        dest.writeString(serverIp) // 写入 serverIp
        dest.writeString(tcpIp) // 写入 tcpIp
    }

    companion object CREATOR : Parcelable.Creator<ConnectData> {
        override fun createFromParcel(parcel: Parcel): ConnectData {
            return ConnectData(
                id = parcel.readString() ?: UUID.randomUUID().toString(), // 读取 id
                serverIp = parcel.readString(), // 读取 serverIp
                tcpIp = parcel.readString() // 读取 tcpIp
            )
        }

        override fun newArray(size: Int): Array<ConnectData?> {
            return arrayOfNulls(size) // 创建指定大小的数组
        }
    }
}

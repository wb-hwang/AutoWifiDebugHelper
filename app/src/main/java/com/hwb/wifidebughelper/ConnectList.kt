package com.hwb.wifidebughelper

import com.tencent.mmkv.MMKV

object ConnectList {
    const val KEY_CONNECT_LIST = "connect_list"
    const val KEY_CONNECT_SELECT = "connect_select"


    enum class ErrorCode {
        SUCCESS,
        KEY_EXIST,
        KEY_NOT_EXIST
    }

    fun getSelectId(): ConnectData? {
        val mmkv = MMKV.mmkvWithID(KEY_CONNECT_SELECT)
        val selectedId = mmkv.decodeString("selectedId", "")
        if (selectedId.isNullOrEmpty()) {
            return null
        }
        val mmkv2 = MMKV.mmkvWithID(KEY_CONNECT_LIST)
        return mmkv2.decodeParcelable(selectedId,  ConnectData::class.java)
    }

    fun setSelectId(id: String?) {
        val mmkv = MMKV.mmkvWithID(KEY_CONNECT_SELECT)
        mmkv.encode("selectedId", id)
    }

    fun add(serverIp: String?, tcpIp: String? = "5555", name: String? = null, serverPort: String? = "5000", onRes: (BaseResp) -> Unit) {
        if (checkListForValue(serverIp)) {
            onRes(BaseResp(ErrorCode.KEY_EXIST, null))
        } else {
            val mmkv = MMKV.mmkvWithID(KEY_CONNECT_LIST)
            val newData = ConnectData(serverIp = serverIp, tcpIp = tcpIp, name = name, serverPort = serverPort)
            mmkv.encode(newData.id, newData)
            onRes(BaseResp(ErrorCode.SUCCESS, newData))
        }
    }

    fun remove(id: String) {
        val mmkv = MMKV.mmkvWithID(KEY_CONNECT_LIST)
        mmkv.removeValueForKey(id)
    }

    fun update(data: ConnectData) {
        val mmkv = MMKV.mmkvWithID(KEY_CONNECT_LIST)
        mmkv.encode(data.id, data)
    }

    fun getList(): MutableList<ConnectData?> {
        val mmkv = MMKV.mmkvWithID(KEY_CONNECT_LIST)
        val list = arrayListOf<ConnectData?>()
        mmkv.allKeys()?.forEach {
            try {
                list.add(mmkv.decodeParcelable(it, ConnectData::class.java))
            } catch (_: Exception) {
            }
        }
        return list
    }

    fun checkListForValue(serverIp: String?): Boolean {
        getList().forEach {
            if (it?.serverIp == serverIp) {
                return true
            }
        }
        return false
    }


}
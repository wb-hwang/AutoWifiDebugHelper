package com.hwb.wifidebughelper

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.hjq.toast.Toaster
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectListVM : ViewModel() {

    val _items = mutableStateListOf<ConnectData?>()

    val _item = mutableStateOf<ConnectData?>(null)

    init {
        _items.addAll(ConnectList.getList())
        _item.value = ConnectList.getSelectId()
    }
    //val items = _items.asStateFlow()

    fun removeItem(id: String?) {
        id?.let {
            ConnectList.remove(id)
            _items.removeIf { it?.id == id }
        }
    }

    fun updateItem(data: ConnectData) {
        // 更新数据源
        ConnectList.update(data)
        val index = _items.indexOfFirst { it?.id == data.id } // 找到需要更新的元素索引
        if (index != -1) {
            // 更新ui元素
            _items[index] = data
        }
    }

    fun addItem(serverIp: String?, tcpIp: String?) {
        ConnectList.add(serverIp, tcpIp) {
            when (it.code) {
                ConnectList.ErrorCode.SUCCESS -> {
                    _items.add(it.data)
                }

                ConnectList.ErrorCode.KEY_EXIST -> {
                    // 可以优化为弹窗选择，去修改or直接替换
                    Toaster.show("服务器ip已存在")
                }

                else -> {}
            }
        }
    }

    fun addItems(newItems: MutableList<ConnectData?>) {
        // _items.value = newItems
    }
}
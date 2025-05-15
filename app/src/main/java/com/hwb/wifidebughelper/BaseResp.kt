package com.hwb.wifidebughelper

import android.os.Parcel
import android.os.Parcelable
import com.hwb.wifidebughelper.ConnectList.ErrorCode

data class BaseResp(
    var code: ErrorCode = ErrorCode.SUCCESS,
    var data: ConnectData?
)


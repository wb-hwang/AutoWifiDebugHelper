Android Studio 自带的无线调试不稳定，经常断开，这个工具可以自动链接，尤其是手机ip 经常会变的时候。

### 原理
进入tcpip 模式链接，然后监听ip 变化，监听到时上报到PC 端，用python 脚本自动链接

### 使用说明
1. 将手机用数据线连接到pc
2. pc 执行命令开启tcpip 模式：adb tcpip {端口号，默认：5555}
3. pc 运行对应python 脚本（/python/reciveAdbAddressServer.py）
4. 拔掉数据线，app 设置服务器地址
5. 后续app 会自动监听并链接

### 注意事项
* 设置***ADB_HOME*** ，确保控制台可以直接执行adb 命令，或者在python 修改adb 路径
* 如果链接不是检查pc 和手机是否在同一个局域网
* 链接不上尝试更换端口，5555、5556、5557 等
* 如果打开APP 后没有看到常驻通知，请给予通知权限
* 如果没有python 环境，可以使用可执行程序（/python/reciveAdbAddressServer.exe）
from flask import Flask, request
import subprocess

app = Flask(__name__)

@app.route('/string', methods=['GET'])
def receive_string():
    address = request.args.get("address")
    # string = data.decode('utf-8') # 将接收到的二进制数据解码为字符串
    print(f"Received string: {address}")
    ret = subprocess.run(["powershell", "-command",
                                      F"adb connect {address}"],
                                     capture_output=True).stdout.decode().strip()
    isSuccess = ret.__contains__("connected")
    print(ret)
    return {"code": 200,"msg":ret,"data":isSuccess}

# 如果报错没有安装flask 就跑个命令：pip install Flask
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)

# 比鼬文件传输

本应用为Android文件传输应用。实现功能：

1. 文件选择
	- 将各类文件分类
	- 提供文件操作的基本功能
2. 文件传输
	- Android与Android设备互传文件
	- Android与PC端互传文件（服务器端程序，见[BiuWeb](https://github.com/YieldNull/BiuWeb)）
	- Android与IOS等其他设备互传文件
3. 传输历史
	- 管理传输历史记录：实现打开，删除，发送，分享等操作。
4. 文件分享
	- 接收从其他应用分享的文件，用以发送
	- 将已传输的文件分享给其他应用

分为[界面模块](app)以及[httpd](httpd)模块（根据RFC2388解析`multipart/form-data`）

查看[使用帮助](help.md)，查看[演示](http://yieldnull.github.io/Biu/index.html)

## 作者

- [YieldNull](https://github.com/YieldNull)
- [kmyfoer](https://github.com/kmyfoer)
- [WaterDemo](https://github.com/WaterDemo)


## LICENSE

GNU General Public License v2.0

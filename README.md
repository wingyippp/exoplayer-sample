# exoplayer-sample
a sample project of the exoplayer

# 环境搭建步骤

1. 下载并安装[Android Studio](https://developer.android.com/studio)
2. 克隆Github工程到本地 `git clone git@github.com:wingyippp/exoplayer-sample.git`
3. 启动Android Studio，选择打开步骤2的工程根目录
4. Android Studio会自动开始下载Android SDK
5. 下载CMake版本：打开Android Studio -> Setting -> Languages and frameworks -> Android SDK -> SDK Tools -> CMake，选择`3.22.1`版本下载安装
6. 直接点击Android Studio中的运行按钮
7. 其中，C/C++代码在[/app/src/main/cpp/audio_processor.cpp](https://github.com/wingyippp/exoplayer-sample/blob/main/app/src/main/cpp/audio_processor.cpp)里面
8. 音频文件保存在`app/src/main/assets/samples/music.mp3`，在保持文件名字不变的情况下，可以随意替换音频文件

![image](https://raw.githubusercontent.com/wingyippp/exoplayer-sample/refs/heads/main/demo_screenshot.png)
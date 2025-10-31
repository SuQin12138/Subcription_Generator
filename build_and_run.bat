@echo off
chcp 65001 >nul
echo.
echo ========================================
echo    Android地区切换应用 - 构建脚本
echo ========================================
echo.

echo [1/4] 检查环境...
if not exist gradlew.bat (
    echo 错误: 找不到 gradlew.bat 文件
    pause
    exit /b 1
)

if not exist local.properties (
    echo 警告: 找不到 local.properties 文件
    echo 请复制 local.properties.template 并配置 Android SDK 路径
    echo.
    set /p choice="是否继续构建? (y/n): "
    if /i not "%choice%"=="y" (
        echo 构建已取消
        pause
        exit /b 1
    )
)

echo [2/4] 清理项目...
call gradlew.bat clean
if errorlevel 1 (
    echo 错误: 清理项目失败
    pause
    exit /b 1
)

echo [3/4] 构建APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo 错误: 构建失败
    echo 请检查:
    echo 1. Android SDK 是否正确安装
    echo 2. local.properties 中的 sdk.dir 路径是否正确
    echo 3. 网络连接是否正常 (下载依赖)
    pause
    exit /b 1
)

echo [4/4] 构建完成!
echo.
echo APK文件位置: app\build\outputs\apk\debug\app-debug.apk
echo.

set /p choice="是否安装到连接的设备? (y/n): "
if /i "%choice%"=="y" (
    echo.
    echo 正在检查连接的设备...
    adb devices
    echo.
    echo 正在安装...
    call gradlew.bat installDebug
    if errorlevel 1 (
        echo 错误: 安装失败
        echo 请检查:
        echo 1. 设备是否已连接并启用USB调试
        echo 2. 设备是否信任此计算机
        echo 3. ADB驱动是否正确安装
    ) else (
        echo 安装成功!
        echo 可以在设备上找到 "地区切换器" 应用
    )
)

echo.
echo ========================================
echo              构建信息
echo ========================================
echo 应用名称: 地区切换器
echo 包名: com.example.regionswitcher
echo 版本: 1.0 (Debug)
echo 最低SDK: Android 7.0 (API 24)
echo 目标SDK: Android 14 (API 34)
echo.
echo 🎨 UI主题: Matrix 赛博朋克风格
echo 🏗️ 架构: MVVM + Hilt + Room + DataStore
echo 📱 功能: 地区切换 + 配置管理 + 订阅生成
echo.
echo 构建脚本执行完成
pause
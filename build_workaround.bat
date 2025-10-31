@echo off
setlocal

echo ===============================================
echo   Android 地区切换应用 - 兼容性构建脚本
echo ===============================================

echo [1/5] 检测Java版本...
java -version 2>&1 | findstr "version"
if errorlevel 1 (
    echo ❌ Java未安装或配置错误
    pause
    exit /b 1
)

echo.
echo [2/5] 设置Java 17环境变量 (临时)...
rem 尝试查找Java 17安装路径
set "JAVA17_PATH="
if exist "C:\Program Files\Java\jdk-17" (
    set "JAVA17_PATH=C:\Program Files\Java\jdk-17"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot" (
    set "JAVA17_PATH=C:\Program Files\Eclipse Adoptium\jdk-17.0.12.7-hotspot"
) else if exist "C:\Program Files\Microsoft\jdk-17.0.12.7-hotspot" (
    set "JAVA17_PATH=C:\Program Files\Microsoft\jdk-17.0.12.7-hotspot"
)

if defined JAVA17_PATH (
    echo ✅ 找到Java 17: %JAVA17_PATH%
    set "JAVA_HOME=%JAVA17_PATH%"
    set "PATH=%JAVA17_PATH%\bin;%PATH%"
) else (
    echo ⚠️ 未找到Java 17，使用当前Java版本尝试构建...
)

echo.
echo [3/5] 清理项目...
call gradlew.bat clean --no-daemon --quiet

echo.
echo [4/5] 构建调试版APK...
call gradlew.bat assembleDebug --no-daemon --quiet

if errorlevel 1 (
    echo.
    echo ❌ 构建失败，尝试下载Java 17...
    echo.
    echo 请访问以下链接下载Java 17:
    echo https://adoptium.net/temurin/releases/?version=17
    echo.
    echo 或者运行以下命令安装 (需要管理员权限):
    echo winget install EclipseAdoptium.Temurin.17.JDK
    echo.
    pause
    exit /b 1
)

echo.
echo [5/5] 构建完成！
echo.
echo 📱 APK文件位置:
echo %cd%\app\build\outputs\apk\debug\app-debug.apk
echo.
echo 🚀 安装到设备 (确保USB调试已开启):
echo adb install app\build\outputs\apk\debug\app-debug.apk
echo.

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ✅ APK构建成功！
    
    echo.
    echo 是否现在安装到连接的Android设备? (Y/N)
    set /p "install_choice=请选择: "
    
    if /i "%install_choice%"=="Y" (
        echo.
        echo 正在安装APK到设备...
        adb install -r "app\build\outputs\apk\debug\app-debug.apk"
        if errorlevel 1 (
            echo ❌ 安装失败，请检查设备连接和USB调试设置
        ) else (
            echo ✅ 安装成功！
        )
    )
) else (
    echo ❌ APK文件未找到
)

echo.
pause

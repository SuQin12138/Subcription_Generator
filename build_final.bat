@echo off
setlocal EnableDelayedExpansion

echo ===============================================
echo   Android 地区切换应用 - 自动构建脚本
echo ===============================================

echo [1/6] 检查Java环境...
java -version 2>&1 | findstr "version" | findstr "17\|11\|1.8"
if errorlevel 1 (
    echo ❌ 当前Java版本不兼容，需要Java 17、11或8
    echo 📥 正在下载便携版Java 17...
    
    if not exist "java17" (
        mkdir java17
        echo 请手动下载Java 17并解压到java17文件夹，或安装Java 17
        echo 下载链接: https://adoptium.net/temurin/releases/?version=17
        pause
        exit /b 1
    )
    
    set "JAVA_HOME=%cd%\java17"
    set "PATH=%cd%\java17\bin;%PATH%"
    echo ✅ 使用便携版Java 17
) else (
    echo ✅ Java版本兼容
)

echo.
echo [2/6] 清理Gradle缓存...
if exist "%USERPROFILE%\.gradle\caches" (
    echo 清理旧缓存...
    rmdir /s /q "%USERPROFILE%\.gradle\caches" 2>nul
)

echo.
echo [3/6] 设置Gradle配置...
echo org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 > gradle.properties
echo org.gradle.parallel=true >> gradle.properties
echo org.gradle.daemon=false >> gradle.properties
echo android.useAndroidX=true >> gradle.properties
echo android.enableJetifier=true >> gradle.properties

echo.
echo [4/6] 清理项目...
call gradlew.bat clean --no-daemon --quiet 2>nul

echo.
echo [5/6] 构建APK...
echo 这可能需要几分钟时间，请耐心等待...
call gradlew.bat assembleDebug --no-daemon --info

if errorlevel 1 (
    echo.
    echo ❌ 构建失败，尝试强制重新下载依赖...
    call gradlew.bat assembleDebug --no-daemon --refresh-dependencies
    
    if errorlevel 1 (
        echo.
        echo ❌ 构建仍然失败，请检查错误信息或手动安装Java 17
        pause
        exit /b 1
    )
)

echo.
echo [6/6] 构建完成！
echo.

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo ✅ APK构建成功！
    echo 📁 文件位置: %cd%\app\build\outputs\apk\debug\app-debug.apk
    echo.
    
    for %%I in ("app\build\outputs\apk\debug\app-debug.apk") do (
        echo 📊 文件大小: %%~zI 字节
    )
    
    echo.
    echo 🚀 安装选项:
    echo 1. USB调试安装: adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo 2. 直接传输APK到手机进行安装
    echo.
    
    echo 是否现在尝试安装到连接的Android设备? (Y/N)
    set /p "install_choice=请选择: "
    
    if /i "!install_choice!"=="Y" (
        echo.
        echo 正在检查ADB连接...
        adb devices
        echo.
        echo 正在安装APK...
        adb install -r "app\build\outputs\apk\debug\app-debug.apk"
        if errorlevel 1 (
            echo ❌ 安装失败，请检查：
            echo - USB调试是否开启
            echo - 设备是否正确连接
            echo - 是否允许USB调试授权
        ) else (
            echo ✅ 安装成功！应用已安装到设备
        )
    )
    
    echo.
    echo 📱 应用信息:
    echo - 应用名称: 地区切换器
    echo - 包名: com.example.regionswitcher
    echo - 版本: 1.0
    echo - 最小Android版本: 7.0 (API 24)
    
) else (
    echo ❌ APK文件未找到，构建可能失败
)

echo.
echo 构建脚本执行完成！
pause

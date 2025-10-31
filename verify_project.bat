@echo off
chcp 65001 >nul
echo.
echo ========================================
echo    Android地区切换应用 - 项目验证脚本
echo ========================================
echo.

set "ERROR_COUNT=0"

echo [1/8] 检查项目结构...

:: 检查关键目录
if not exist "app\src\main\java\com\example\regionswitcher" (
    echo ❌ 主源码目录缺失
    set /a ERROR_COUNT+=1
) else (
    echo ✅ 主源码目录存在
)

if not exist "app\src\main\res" (
    echo ❌ 资源目录缺失
    set /a ERROR_COUNT+=1
) else (
    echo ✅ 资源目录存在
)

echo.
echo [2/8] 检查核心文件...

:: 检查关键文件
set "CORE_FILES=app\build.gradle app\src\main\AndroidManifest.xml app\src\main\java\com\example\regionswitcher\MainActivity.kt"

for %%f in (%CORE_FILES%) do (
    if exist "%%f" (
        echo ✅ %%f
    ) else (
        echo ❌ %%f 缺失
        set /a ERROR_COUNT+=1
    )
)

echo.
echo [3/8] 检查数据层文件...

set "DATA_FILES=app\src\main\java\com\example\regionswitcher\data\model\DataModels.kt app\src\main\java\com\example\regionswitcher\data\database\AppDatabase.kt app\src\main\java\com\example\regionswitcher\data\repository\RegionRepository.kt"

for %%f in (%DATA_FILES%) do (
    if exist "%%f" (
        echo ✅ %%f
    ) else (
        echo ❌ %%f 缺失
        set /a ERROR_COUNT+=1
    )
)

echo.
echo [4/8] 检查UI层文件...

set "UI_FILES=app\src\main\java\com\example\regionswitcher\ui\viewmodel\MainViewModel.kt app\src\main\java\com\example\regionswitcher\ui\config\ConfigActivity.kt app\src\main\java\com\example\regionswitcher\ui\subscription\SubscriptionActivity.kt"

for %%f in (%UI_FILES%) do (
    if exist "%%f" (
        echo ✅ %%f
    ) else (
        echo ❌ %%f 缺失
        set /a ERROR_COUNT+=1
    )
)

echo.
echo [5/8] 检查布局文件...

set "LAYOUT_FILES=app\src\main\res\layout\activity_main.xml app\src\main\res\layout\activity_config.xml app\src\main\res\layout\activity_subscription.xml"

for %%f in (%LAYOUT_FILES%) do (
    if exist "%%f" (
        echo ✅ %%f
    ) else (
        echo ❌ %%f 缺失
        set /a ERROR_COUNT+=1
    )
)

echo.
echo [6/8] 检查资源文件...

set "RESOURCE_FILES=app\src\main\res\values\strings.xml app\src\main\res\values\colors.xml app\src\main\res\values\styles.xml"

for %%f in (%RESOURCE_FILES%) do (
    if exist "%%f" (
        echo ✅ %%f
    ) else (
        echo ❌ %%f 缺失
        set /a ERROR_COUNT+=1
    )
)

echo.
echo [7/8] 检查配置文件...

set "CONFIG_FILES=gradle.properties settings.gradle build.gradle"

for %%f in (%CONFIG_FILES%) do (
    if exist "%%f" (
        echo ✅ %%f
    ) else (
        echo ❌ %%f 缺失
        set /a ERROR_COUNT+=1
    )
)

echo.
echo [8/8] 检查Gradle Wrapper...

if exist "gradlew.bat" (
    echo ✅ gradlew.bat
) else (
    echo ❌ gradlew.bat 缺失
    set /a ERROR_COUNT+=1
)

if exist "gradle\wrapper\gradle-wrapper.properties" (
    echo ✅ gradle-wrapper.properties
) else (
    echo ❌ gradle-wrapper.properties 缺失
    set /a ERROR_COUNT+=1
)

echo.
echo ========================================
echo              验证结果
echo ========================================

if %ERROR_COUNT% == 0 (
    echo ✅ 项目验证通过！所有必需文件都存在。
    echo.
    echo 🚀 项目已准备就绪，可以开始构建：
    echo    1. 运行 'gradlew.bat clean' 清理项目
    echo    2. 运行 'gradlew.bat assembleDebug' 构建APK
    echo    3. 运行 'gradlew.bat installDebug' 安装到设备
    echo.
    echo 📁 构建产物将在以下位置：
    echo    app\build\outputs\apk\debug\app-debug.apk
) else (
    echo ❌ 项目验证失败！发现 %ERROR_COUNT% 个问题。
    echo.
    echo 🔧 请检查并补全缺失的文件，然后重新运行验证。
)

echo.
echo ========================================
echo              项目统计
echo ========================================

:: 统计代码行数
set "KOTLIN_COUNT=0"
set "XML_COUNT=0"

for /r "app\src\main\java" %%f in (*.kt) do (
    set /a KOTLIN_COUNT+=1
)

for /r "app\src\main\res" %%f in (*.xml) do (
    set /a XML_COUNT+=1
)

echo 📊 Kotlin 文件: %KOTLIN_COUNT% 个
echo 📊 XML 文件: %XML_COUNT% 个
echo 📊 总文件数: %KOTLIN_COUNT% + %XML_COUNT% 个

echo.
echo 🏗️ 项目架构:
echo    ├── MVVM 架构模式
echo    ├── Hilt 依赖注入
echo    ├── Room 数据库
echo    ├── DataStore 配置存储
echo    ├── Kotlin Coroutines + Flow
echo    └── Material Design 3 UI

echo.
echo 🎨 UI 主题:
echo    ├── Matrix 赛博朋克风格
echo    ├── 绿色调色板 (#00FF00)
echo    ├── 卡片式布局设计
echo    └── 响应式动画效果

echo.
pause

# 基于CF workers反代的Android 订阅生成应用
*readme由Claude Sonnet 4.5 编写

## 项目简介

这是一个基于[Joey](https://github.com/byJoey)大佬[原始 Cloudflare Worker 代理脚本](https://github.com/byJoey/cfnew.git)改编的 Android 应用，提供代理地区切换、配置管理和订阅生成功能。

## 功能特性

### 🌍 地区管理
- **智能地区检测**: 支持自动检测和手动选择
- **多地区支持**: 美国、新加坡、日本、香港、韩国、德国等10+地区
- **地区匹配算法**: 智能选择最优连接地区

### ⚙️ 配置管理
- **协议支持**: VLESS、Trojan、Xhttp 多协议
- **自定义配置**: ProxyIP、Worker地区、认证令牌
- **高级设置**: TLS限制、地区匹配、优选IP管理

### 📡 订阅管理
- **多客户端支持**: Clash、Surge、Sing-Box、Loon、Quantumult X、V2Ray
- **一键生成**: 自动生成适配不同客户端的订阅链接
- **实时统计**: 节点数量、地区覆盖、更新时间

### 🎨 Matrix UI设计
- **赛博朋克风格**: 绿色Matrix主题界面
- **动态效果**: 流畅的动画和视觉反馈
- **响应式布局**: 适配不同屏幕尺寸

## 技术栈

### 架构
- **MVVM架构**: ViewModel + LiveData/StateFlow
- **依赖注入**: Hilt (Dagger2)
- **响应式编程**: Kotlin Coroutines + Flow

### 核心库
- **UI框架**: Material Design 3 + ViewBinding
- **数据存储**: Room Database + DataStore
- **网络请求**: Retrofit2 + OkHttp3
- **导航组件**: Activity Navigation

### 开发工具
- **构建系统**: Gradle 8.0
- **最低SDK**: Android 7.0 (API 24)
- **目标SDK**: Android 14 (API 34)

## 快速开始

### 环境要求
- Android Studio Hedgehog+ (2023.1.1+)
- JDK 17+
- Android SDK 34+
- Gradle 8.0+

### 构建步骤

1. **克隆项目**
```bash
git clone https://github.com/SuQin12138/Subcription_Generator.git
cd Config_Subcribe
```

2. **配置环境**
```bash
# 复制local.properties模板并配置Android SDK路径
cp local.properties.template local.properties
# 编辑local.properties文件，设置正确的SDK路径
```

3. **构建项目**
```bash
# 清理项目
.\gradlew.bat clean

# 构建Debug版本
.\gradlew.bat assembleDebug
```

4. **安装到设备**
```bash
# 通过ADB安装
.\gradlew.bat installDebug
```

## 应用使用说明

### 基本操作
1. **启动应用**: 打开"地区切换器"应用
2. **选择地区**: 在主界面选择目标地区或启用自动检测
3. **配置设置**: 进入配置页面设置协议和高级选项
4. **生成订阅**: 在订阅页面选择客户端类型并生成订阅链接
5. **导入客户端**: 将生成的订阅链接导入到对应的代理客户端

### 配置说明

#### 系统配置
- **Worker地区**: 手动指定Worker运行地区
- **自定义ProxyIP**: 设置自定义代理IP地址
- **地区匹配**: 启用智能地区匹配算法
- **TLS限制**: 仅生成TLS加密节点
- **优选IP**: 管理优选IP列表

#### 协议配置
- **VLESS**: 轻量级代理协议 (默认启用)
- **Trojan**: 基于TLS的代理协议
- **Xhttp**: HTTP/2传输协议
- **认证设置**: UUID令牌和Trojan密码

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/regionswitcher/
│   │   ├── data/                    # 数据层
│   │   │   ├── model/               # 数据模型
│   │   │   ├── database/            # 数据库相关
│   │   │   └── repository/          # 数据仓库
│   │   ├── ui/                      # UI层
│   │   │   ├── viewmodel/           # ViewModel
│   │   │   ├── config/              # 配置页面
│   │   │   └── subscription/        # 订阅页面
│   │   ├── di/                      # 依赖注入
│   │   ├── utils/                   # 工具类
│   │   └── MainActivity.kt          # 主活动
│   └── res/                         # 资源文件
│       ├── layout/                  # 布局文件
│       ├── values/                  # 值资源
│       └── drawable/                # 图形资源
```

## 开发指南

### 添加新地区

1. 更新常量定义:
```kotlin
// Constants.kt
val REGION_MAPPING = mapOf(
    "XX" to listOf("🇽🇽 新地区", "XX", "New Region")
)
```

2. 添加数据库记录:
```kotlin
// 在DatabaseCallback中添加
INSERT INTO regions (code, nameZh, nameEn, flag, isCustom, isActive, priority) 
VALUES ('XX', '🇽🇽 新地区', 'New Region', '🇽🇽', 0, 0, 11)
```

### 自定义主题

修改颜色和样式资源:
```xml
<!-- colors.xml -->
<color name="matrix_green">#00FF00</color>
<color name="custom_accent">#FF6B00</color>

<!-- styles.xml -->
<style name="CustomButton" parent="MatrixButton">
    <item name="backgroundTint">@color/custom_accent</item>
</style>
```

## 故障排除

### 常见问题

**Q: 构建失败，提示找不到SDK**
A: 确保已正确安装Android SDK，并在local.properties中设置sdk.dir

**Q: 应用无法连接网络**
A: 检查网络权限，确保设备已连接互联网

**Q: 订阅链接无效**
A: 验证Worker域名和认证令牌是否正确配置

### 调试技巧

1. **查看日志**:
```bash
adb logcat -s RegionSwitcher
```

2. **网络调试**:
```kotlin
// 启用网络日志
OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor())
```

## 贡献指南

欢迎提交Issue和Pull Request！

### 开发流程
1. Fork项目并创建特性分支
2. 进行开发并编写测试
3. 确保代码符合项目规范
4. 提交Pull Request

### 代码规范
- 使用Kotlin编码规范
- 遵循MVVM架构模式
- 添加适当的注释和文档
- 编写单元测试

## 许可证

本项目基于 MIT 许可证开源。

---

**注意**: 本应用仅供学习和研究使用，请遵守当地法律法规。

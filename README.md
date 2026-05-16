# JetBrains Help

一个基于Spring Boot的JetBrains许可证激活服务项目。

## 功能特性

- 模拟JetBrains官方许可证服务器
- 生成产品和插件激活码
- 提供JRebel许可证服务
- 集成ja-netfilter代理工具下载
- 现代化Web界面

## 快速开始

### 环境要求

- Java 1.8+
- Maven 3.x

### 运行应用

```bash
# 克隆项目
git clone <repository-url>
cd Jetbrains-Help

# 编译项目
mvn clean compile

# 启动应用
mvn spring-boot:run
```

应用启动后访问：http://localhost:10768

## 使用说明

1. **下载代理工具**：访问 `/ja-netfilter` 下载并配置ja-netfilter
2. **激活码方式**：在Web界面生成产品或插件激活码
3. **服务器方式**：配置许可证服务器地址为应用根地址
4. **JRebel激活**：使用 `{服务器地址}/{uuid}` 格式

## 技术栈

- **后端**：Spring Boot 2.6.13, Java 1.8
- **前端**：Vue.js 3, TailwindCSS
- **构建**：Maven

## 许可证

本项目仅供学习交流使用。

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

### 代理工具方法1(推荐)：
  1. 在Web界面直接下载`ja-netfilter.zip`工具包
    <img width="1285" height="725" alt="image" src="https://github.com/user-attachments/assets/98640a06-7a61-44da-829e-ad60b8ed39f9" />
  2. 解压后放入**不包含中文路径**的任意目录
  3. 之前使用过代理工具
     - 先执行 `/scripts/uninstall-all-users.vbs` 等待`Done`弹窗
     - 再执行`/scripts/install-current-user.vbs` 等待`Done`弹窗
  4. 之前未使用过代理工具
     - 执行`/scripts/install-current-user.vbs` 等待`Done`弹窗
  5. **重启电脑(重要)**

### 代理工具方法2：
  1. 下载最新版原始工具[ja-netfilter](https://gitee.com/ja-netfilter/ja-netfilter/releases)
  2. 配置工具包\config\dns.conf
      ```
      [DNS]
      EQUAL,jetbrains.com
      EQUAL,dbeaver.com
      EQUAL,plugin.obroom.com
      ```
  3. 配置工具包\config\url.conf
      ```
      [URL]
      
      PREFIX,https://check-license.squaretest.com
      
      PREFIX,https://account.jetbrains.com/lservice/rpc/validateKey.action
      PREFIX,https://account.jetbrains.com/lservice/rpc/validateLicense.action
      PREFIX,https://account.jetbrains.com/lservice/rpc/obtainAgreement.action
      PREFIX,https://account.jetbrains.com/lservice/rpc/obtainLicense.action
      PREFIX,https://account.jetbrains.com/lservice/rpc/fetchData.action
      
      PREFIX,https://account.jetbrains.com.cn/lservice/rpc/validateKey.action
      PREFIX,https://account.jetbrains.com.cn/lservice/rpc/validateLicense.action
      PREFIX,https://account.jetbrains.com.cn/lservice/rpc/obtainAgreement.action
      PREFIX,https://account.jetbrains.com.cn/lservice/rpc/obtainLicense.action
      PREFIX,https://account.jetbrains.com.cn/lservice/rpc/fetchData.action
      ```
  4. 配置工具包\config\power.conf
     - 在Web界面获取power.conf的值
     <img width="1237" height="462" alt="image" src="https://github.com/user-attachments/assets/3716a979-cf58-4f25-9d16-97b79e411122" />
  5. 配置工具包\vmoptions
     -  \vmoptions目录下所有文件均在最后一行添加 `-javaagent:破解包里ja-netfilter.jar的绝对路径`
     - 例如：`-javaagent:D:\ja-netfilter\ja-netfilter.jar`
  6. 配置电脑【用户变量】
       - 如何查看应用程序对应的变量名称
         - 打开`应用程序安装目录\bin\应用程序名称.bat`,搜索`SET USER_VM_OPTIONS_FILE=`
       - 右键我的电脑 -> 属性 -> 高级系统设置 -> 环境变量 -> 用户变量 -> 新建

         | 变量名                         | 变量值                                        |
         |:----------------------------|:-------------------------------------------|
         | APPCODE_VM_OPTIONS          | 破解包路径\vmoptions\appcode.vmoptions          |
         | CLION_VM_OPTIONS            | 破解包路径\vmoptions\clion.vmoptions            |
         | DATAGRIP_VM_OPTIONS         | 破解包路径\vmoptions\datagrip.vmoptions         |
         | DATASPELL_VM_OPTIONS        | 破解包路径\vmoptions\dataspell.vmoptions        |
         | DEVECOSTUDIO_VM_OPTIONS     | 破解包路径\vmoptions\devecostudio.vmoptions     |
         | GATEWAY_VM_OPTIONS          | 破解包路径\vmoptions\gateway.vmoptions          |
         | GOLAND_VM_OPTIONS           | 破解包路径\vmoptions\goland.vmoptions           |
         | IDEA_VM_OPTIONS             | 破解包路径\vmoptions\idea.vmoptions             |
         | JETBRAINS_CLIENT_VM_OPTIONS | 破解包路径\vmoptions\jetbrains_client.vmoptions |
         | JETBRAINSCLIENT_VM_OPTIONS  | 破解包路径\vmoptions\jetbrainsclient.vmoptions  |
         | PHPSTORM_VM_OPTIONS         | 破解包路径\vmoptions\phpstorm.vmoptions         |
         | PYCHARM_VM_OPTIONS          | 破解包路径\vmoptions\pycharm.vmoptions          |
         | RIDER_VM_OPTIONS            | 破解包路径\vmoptions\rider.vmoptions            |
         | RUBYMINE_VM_OPTIONS         | 破解包路径\vmoptions\rubymine.vmoptions         |
         | STUDIO_VM_OPTIONS           | 破解包路径\vmoptions\studio.vmoptions           |
         | WEBIDE_VM_OPTIONS           | 破解包路径\vmoptions\webide.vmoptions           |
         | WEBSTORM_VM_OPTIONS         | 破解包路径\vmoptions\webstorm.vmoptions         |
  7. **重启电脑(重要)**
      
### 产品激活
1. **激活码方式**：在Web界面生成产品或插件激活码
2. **服务器方式**：配置许可证服务器地址为应用根地址
3. **JRebel激活**：使用 `{服务器地址}/{uuid}` 格式

## 技术栈

- **后端**：Spring Boot 2.6.13, Java 1.8
- **前端**：Vue.js 3, TailwindCSS
- **构建**：Maven

## 许可证

本项目仅供学习交流使用。

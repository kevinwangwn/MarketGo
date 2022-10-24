

&emsp;

&emsp;


<div align=center><img src="https://s1.ax1x.com/2022/10/18/xrKqgK.png" width="500"  align=center /></div>

&emsp;

&emsp;



## 1、 MarketGo介绍

MarketGo是基于企业微信的营销平台，

MarketGo在企业微信的开放能力之上，建立了连接客户、员工的通道。基于以上通道企业可以快速的构建自己行业、企业的营销场景。

MarketGo的特点：

- MarketGo是开源平台；
- MarketGo是一个开放的系统，可以对接企业的内容平台、CDP系统；

**MarKetGo发布了V0.0.1版本，这个版本的特点是构建了客户，客户群，客户朋友圈的通道。**

## 2、项目介绍

### 2.1、项目结构

前端项目结构如下：

```
├── babel.config.js       --按需引入配置
├── package-lock.json     --结合了逻辑树和物理树的一个快照
├── package.json          --安装树
├── public                --公共文件
│   ├── favicon.ico       --网页标题的左侧小图标
│   └── index.html        --入口页面
├── src                   --源码文件
│   ├── App.vue           --根组件
│   ├── api               --api配置
│   ├── assets            --资源文件
│   ├── components        --公共组件
│   ├── main.js           --入口文件
│   ├── mixins            --js配置+公共变量
│   ├── router            --路由
│   ├── store             --状态库
│   ├── utils             --公共方法
│   └── views             --视图界面
└── vue.config.js         --配置文件
```

后端项目结构如下：

```
├── marketgo.sql           --系统的sql脚本
├── mktgo-api              --dubbo的接口定义
├── mktgo-biz              --业务module
├── mktgo-common           --公共依赖
├── mktgo-core             --业务核心公共模块，
├── mktgo-gateway          --网关服务，请求企微的接口
├── mktgo-react            --接收外部请求的modul，可以根据自己的客户量级，做成服务
├── mktgo-test             --测试项目
├── mktgo-web              --web服务
└── pom.xml                --公共依赖
```

### 2.2、技术框架

- 核心框架：SpringBoot 2.7.0
- 日志管理：SLF4J 1.7
- 持久层框架：spring-data-jpa   2.7.0
- RPC框架：dubbo 2.7.13
- 项目管理框架: Maven 3.6.0
- 前端框架：Vue 2.6.11
- UI框架: element-ui 2.15.9

### 2.3、开发环境

- IDE: IntelliJ IDEA 2019.2+
- DB: Mysql 5.7.37
- JDK: JDK 1.8
- Maven: Maven 3.6.1
- Redis:7.0.2
- Nginx: 1.12.2

## 3、部署流程

[服务器配置](https://baidu.com)

[端口配置](https://baidu.com)

[环境准备](https://baidu.com)

[前端部署](https://baidu.com)

[后端部署](https://baidu.com)

## 4、演示环境

[演示环境](http://101.34.88.187)

用户名：admin

密码：123456

## 5、定制开发

需要基于MarketGo做定制业务需求，请将需求整理成文档和联系方式，发给邮箱：[kevinwangwn0209@gmail.com](kevinwangwn0209@gmail.com)

如果需要技术支持的，请支持299元，并附上你的微信号，按照捐赠的方式支付，我们会联系您并提供项目和安装的相关技术支持。

## 6、开源说明

MarketGo的源码100%开源，遵守GPL-3.0协议。

开发者可以基于MarketGo做二次开发，源码完全免费。

## 7、产品介绍

[演示环境](http://101.34.88.187)

## 8、捐赠

开源不易，坚持更难！如果您觉得MarketGo不错，可以捐赠一杯奶茶，在此表示感谢^_^。

当前我们接受来自于**微信**、**支付宝**的捐赠，请在捐赠时备注自己的昵称或附言。

您的捐赠将用于支付该项目的一些费用支出，并激励开发者们以便更好的推动项目的发展。



|                             微信                             |                            支付宝                            |
| :----------------------------------------------------------: | :----------------------------------------------------------: |
| <img src="https://github.com/kevinwangwn0209/MarketGo/blob/dev_0.0.1/WechatIMG5.jpeg" width="300"  align=center /> | <img src="https://github.com/kevinwangwn0209/MarketGo/blob/dev_0.0.1/WechatIMG6.jpeg" width="300"  align=center /> |


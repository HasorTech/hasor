# Hasor

[![Maven Central](https://img.shields.io/maven-central/v/net.hasor/hasor-core.svg)](https://central.sonatype.com/artifact/net.hasor/hasor-core)
[![Javadoc](https://javadoc.io/badge2/net.hasor/hasor-core/javadoc.svg)](https://javadoc.io/doc/net.hasor/hasor-core)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

Hasor 是一个面向 Java 17 的轻量级应用框架。它以依赖注入容器为基础，将声明式配置、Web MVC、应用启动和可执行 JAR 打包组合成一套模块化能力。应用只需引入实际使用的模块，也可以通过统一的 `Boot.run(...)` 入口启动普通 Java 或内嵌 Web 应用。

Hasor 自 2013 年持续维护。当前稳定版为 `5.3.0`。

## 特性

- **依赖注入**：支持 Hasor 注解与 JSR-330，提供构造器、字段、方法注入以及单例和自定义作用域。
- **模块装配**：通过 `Module` 与 `ApiBinder` 组织绑定关系，支持 AOP、事件通知和应用生命周期回调。
- **声明式配置**：使用 `@Configuration`、`@Bean` 声明对象，通过 `hasor.loadPackages` 统一控制自动发现范围。
- **Web MVC**：提供注解路由、参数绑定、请求校验、过滤器、拦截器、文件上传和返回值渲染。
- **Web 资源**：支持类路径静态资源、欢迎页、缓存控制以及显式配置的单页应用路由回退。
- **统一启动**：普通应用和 Web 应用都使用 `Boot.run(...)`，Boot SPI 负责接入运行环境并统一管理关闭过程。
- **内嵌容器**：支持 Tomcat、Jetty、Undertow，应用选择其中一种依赖即可使用相同的启动方式和 HTTP 配置。
- **运行保障**：Web Boot 默认提供健康检查，可通过业务 `HealthCheck` 扩展依赖状态。
- **可执行 JAR**：提供 Gradle `bootJar` 和 Maven `repackage`，应用及依赖可以打包后直接运行。

数据库访问不再内置于 Hasor，相关能力由同一生态中的 [dbVisitor](https://www.dbvisitor.net/) 提供。

## 快速开始

下面的示例启动一个内嵌 Tomcat，并通过 `GET /hello` 返回文本。

### 1. 引入依赖

```groovy
plugins {
    id 'application'
}

repositories {
    mavenCentral()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

dependencies {
    implementation 'net.hasor:hasor-boot-web-tomcat:5.3.0'
    runtimeOnly 'com.google.code.gson:gson:2.14.0'
}

application {
    mainClass = 'example.Application'
}
```

`gson` 为对象和内置健康检查提供 JSON 渲染。若使用 Jackson、Fastjson 或 Fastjson 2，可替换为对应运行时依赖。

### 2. 编写启动类和接口

```java
package example;

import net.hasor.boot.Boot;
import net.hasor.boot.BootApplication;

public class Application {
    public static void main(String[] args) throws Exception {
        try (BootApplication application = Boot.run(args, Application.class)) {
            application.join();
        }
    }
}
```

```java
package example;

import net.hasor.web.annotation.Get;
import net.hasor.web.annotation.MappingTo;

@MappingTo("/hello")
public class HelloAction {
    @Get
    public String hello() {
        return "Hello Hasor Boot";
    }
}
```

### 3. 配置扫描范围

在 `src/main/resources/hconfig.xml` 中明确业务包。Hasor 不根据启动类位置推导扫描范围。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="https://www.hasor.net/sechma/main">
    <hasor.loadPackages>example</hasor.loadPackages>
</config>
```

运行应用：

```bash
./gradlew run
curl http://localhost:8080/hello
curl http://localhost:8080/health
```

默认监听 `0.0.0.0:8080`。可以通过 `HASOR_HTTP_HOST`、`HASOR_HTTP_PORT` 覆盖监听地址，也可以在 `hconfig.xml` 中配置上下文路径、静态资源和健康检查。

## 模块

| 模块 | 职责 |
| --- | --- |
| [`hasor-core`](hasor-core/README.md) | 依赖注入、模块装配、AOP、事件、配置读取和应用生命周期 |
| [`hasor-config`](hasor-config/README.md) | `@Configuration`、`@Bean`、自动发现和 Web 默认装配 |
| [`hasor-web`](hasor-web/README.md) | Servlet Web MVC、请求处理、响应渲染和静态资源 |
| [`hasor-boot`](hasor-boot/README.md) | 统一启动入口、Boot SPI、内嵌容器和可执行 JAR 打包 |

`hasor-boot-web-tomcat`、`hasor-boot-web-jetty`、`hasor-boot-web-undertow` 是三种可替换的内嵌容器实现；应用只保留其中一个。

## 示例与文档

- [`demo-hasor-boot`](demo-hasor-boot/README.md)：Gradle、Maven 的普通应用和 Web 应用完整示例。
- [快速上手](document/docs/guides/getting-started/overview.md)：按 Core、Config、Web、Boot 选择能力。
- [工程配置与打包](document/docs/guides/deployment/project-config.md)：Maven、Gradle 依赖和可执行 JAR 配置。
- [项目文档](https://www.hasor.net)：完整中文文档。
- [AI 文档索引](https://www.hasor.net/llms.txt) · [English](https://www.hasor.net/en/llms.txt)

## 社区与许可证

- QQ 群：193943114
- License：[Apache License 2.0](LICENSE.txt)

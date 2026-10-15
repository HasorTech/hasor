# Hasor Boot 示例

这里提供两组可直接运行的 Hasor Boot 示例，并分别给出 Gradle 与 Maven 工程。两种构建方式使用相同的 Java 代码和运行行为。

| 场景 | Gradle | Maven | 展示能力 |
| --- | --- | --- | --- |
| 普通应用 | `gradle/demo-hasor-boot-basic` | `maven/demo-hasor-boot-basic` | `Boot.run`、Module、依赖注入、启动参数和生命周期 |
| Web 应用 | `gradle/demo-hasor-boot-web` | `maven/demo-hasor-boot-web` | 内嵌 Tomcat、Controller 自动发现、返回值渲染、静态资源和健康检查 |

示例使用 Java 17 和 Hasor `5.3.0`。Basic 与 Web 使用同一个 `Boot.run(...)` 入口；Web 能力由 `hasor-boot-web-tomcat` 的 Boot SPI 自动接入，不需要手工创建或启动 `WebServer`。

## 构建

在 `demo-hasor-boot` 目录执行 Gradle 构建：

```bash
cd gradle
../../gradlew clean bootJar
```

执行 Maven 构建：

```bash
cd maven
mvn clean package
```

两种构建都会生成包含应用和运行依赖的可执行 JAR。

## 运行 Basic

Gradle：

```bash
java -jar gradle/demo-hasor-boot-basic/build/libs/*-boot.jar demo
```

Maven：

```bash
java -jar maven/demo-hasor-boot-basic/target/*-boot.jar demo
```

应用在 `Module.onStart()` 中读取启动参数并执行命令；`BootApplication` 关闭时进入 `Module.onStop()`，可以看到对应的启动和关闭日志。

## 运行 Web

Gradle：

```bash
java -jar gradle/demo-hasor-boot-web/build/libs/*-boot.jar
```

Maven：

```bash
java -jar maven/demo-hasor-boot-web/target/*-boot.jar
```

启动后可以访问：

| 地址 | 内容 |
| --- | --- |
| `http://localhost:8080/` | `META-INF/resources/index.html` 静态欢迎页 |
| `http://localhost:8080/hello` | 自动发现的 Web MVC 接口 |
| `http://localhost:8080/health` | Boot 内置健康检查 |

`HelloWebAction` 直接返回 `String`，Hasor Web 负责输出正文和 `text/plain` 响应类型。静态首页由框架的资源处理能力提供，不需要编写资源 Controller。

通过环境变量可以覆盖默认端口：

```bash
HASOR_HTTP_PORT=18080 java -jar gradle/demo-hasor-boot-web/build/libs/*-boot.jar
```

Web 示例只保留一个内嵌容器依赖。将 `hasor-boot-web-tomcat` 替换为 `hasor-boot-web-jetty` 或 `hasor-boot-web-undertow`，启动代码无需改变。

## 配置与打包

- 每个示例都在 `src/main/resources/hconfig.xml` 中明确设置 `hasor.loadPackages`，避免使用过大的默认扫描范围。
- Gradle 使用 `bootJar` 和独立的 `bootLoader` 配置生成 `*-boot.jar`。
- Maven 使用 `hasor-boot-maven-plugin` 的 `repackage` 目标，并通过 `boot` classifier 保留可执行包。
- Web 示例提供 Gson 运行时依赖，用于对象返回值和 `/health` 的 JSON 渲染。

完整配置和扩展说明见 [`hasor-boot/README.md`](../hasor-boot/README.md)。

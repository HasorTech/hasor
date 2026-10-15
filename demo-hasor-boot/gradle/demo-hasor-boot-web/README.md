# Hasor Boot Web · Gradle

这个示例通过统一的 `Boot.run(...)` 入口启动内嵌 Tomcat。`hconfig.xml` 限定 Controller 扫描范围，应用不需要手工创建 WebServer 或注册 `HelloWebAction`。

从当前模块目录构建并运行：

```bash
../../../gradlew bootJar
java -jar build/libs/*-boot.jar
```

启动后访问：

- `http://localhost:8080/`：静态欢迎页。
- `http://localhost:8080/hello`：Web MVC 文本接口。
- `http://localhost:8080/health`：内置健康检查。

使用 `HASOR_HTTP_PORT=18080` 可以覆盖默认端口。替换唯一的容器依赖即可切换到 Jetty 或 Undertow，Java 启动代码不变。

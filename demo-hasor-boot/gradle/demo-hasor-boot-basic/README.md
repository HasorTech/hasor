# Hasor Boot Basic · Gradle

这个示例展示普通 Java 应用如何通过 `Boot.run(...)` 启动 Module，完成服务绑定、字段注入、启动参数读取和关闭回调。

从当前模块目录构建并运行：

```bash
../../../gradlew bootJar
java -jar build/libs/*-boot.jar demo
```

`Module.onStart()` 执行一次性命令；退出 try-with-resources 时关闭 `BootApplication`，随后执行 `Module.onStop()`。

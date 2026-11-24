---
id: annotationioc
sidebar_position: 10
title: 2.1.9 自定义注解注入
description: 通过 AnnotationInjector 和 bindAnnotationInjector 扩展 Hasor 的字段注入，并在 Bean 初始化前解析字段值。
---

# 2.1.9 自定义注解注入

实现 `AnnotationInjector<A>`，为自定义注解标记的字段提供注入值。

## 实现注入器

定义字段注解：

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Greeting {
    String value();
}
```

实现 `AnnotationInjector`，根据注解返回字段值：

```java
import java.lang.reflect.Field;
import net.hasor.core.AnnotationInjector;
import net.hasor.core.AppContext;

public class GreetingInjector implements AnnotationInjector<Greeting> {
    @Override
    public Object resolve(AppContext context, Field field, Greeting annotation) {
        return "Hello, " + annotation.value();
    }
}
```

## 注册注入器

在模块的 `loadModule` 中注册注解与注入器的对应关系：

```java
import net.hasor.core.ApiBinder;
import net.hasor.core.Module;

public class GreetingModule implements Module {
    @Override
    public void loadModule(ApiBinder binder) {
        binder.bindAnnotationInjector(Greeting.class, new GreetingInjector());
    }
}
```

Hasor 和 Hasor Boot 均在模块中注册。

## 使用注解

在容器管理的业务对象中标记字段，无需再添加 `@Inject`：

```java
public class Client {
    @Greeting("Meteor")
    private String greeting;

    public String message() {
        return this.greeting;
    }
}
```

容器注入后，`message()` 返回 `Hello, Meteor`。

## 注意事项

- `resolve` 在对象构造后、初始化方法前调用，返回值由容器赋给字段。
- 支持父类字段；不支持 `static`、`final` 字段。
- 一个注解类型只能注册一个注入器。同一字段混用多个注入注解，或叠加 `@Inject`、`@InjectSettings`，会报注入冲突。
- 返回值必须与字段类型兼容，基本类型不能返回 `null`。解析失败会终止注入，不再执行该 Bean 的初始化方法。

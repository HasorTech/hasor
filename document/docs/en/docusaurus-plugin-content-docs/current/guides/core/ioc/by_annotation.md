---
id: annotationioc
sidebar_position: 10
title: 2.1.9 Custom Annotation Injection
description: Register an AnnotationInjector to resolve annotated fields before Bean initialization in Hasor and Hasor Boot.
---

# 2.1.9 Custom Annotation Injection

Implement `AnnotationInjector<A>` to supply values for fields marked with a custom annotation.

## Implement the injector

Define a field annotation:

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

Implement `AnnotationInjector` and return the field value based on the annotation:

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

## Register the injector

Register the annotation and its injector in a module's `loadModule` method:

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

Hasor and Hasor Boot both register injectors in a module.

## Use the annotation

Annotate a field in a container-managed business object. No additional `@Inject` is needed:

```java
public class Client {
    @Greeting("Meteor")
    private String greeting;

    public String message() {
        return this.greeting;
    }
}
```

After injection, `message()` returns `Hello, Meteor`.

## Notes

- `resolve` runs after construction and before Bean initialization. The container assigns its returned value to the field.
- Inherited fields are supported; static and final fields are not.
- Register one injector per annotation type. Combining injection annotations, including `@Inject` or `@InjectSettings`, on the same field causes a conflict.
- Return a value compatible with the field type. Primitive fields cannot accept `null`. Resolution failures abort injection and prevent Bean initialization.

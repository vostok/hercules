# Use hercules mapper

To make serialization and deserialization more easy you can use `HerculesMapper`.

## Deserialization

If you want to deserialize a simple pojo for this schema:

```yaml
PojoClass:
  integerTag: Integer
  optionalStringTag?: String
```

you should add `@Tag` annotation the following way:

```java
class PojoClass {

    @Tag("integerTag")
    private int integerTag;

    @Tag(name = "optionalStringTag", optional = true)
    private Maybe<String> optionalStringTag;

    // Getters and setters
}
```

For immutable classes you must mark one constructor with `@HerculesMapperConstructor` annotation.

The same schema

```yaml
PojoClass:
  integerTag: Integer
  optionalStringTag?: String
```

will be described as:

```java
class ImmutablePojoClass {

    private final int integerTag;
    private final Maybe<String> optionalStringTag;

    @HerculesMapperConstructor
    public ImmutablePojoClass(
        @Tag("integerTag") int integerTag,
        @Tag(name = "optionalStringTag", optional = true) Maybe<String> optionalStringTag
    ) {
        this.integerTag = integerTag;
        this.optionalStringTag = optionalStringTag;
    }

    // Getters
}
```

### Expected tag type

If types parameter is not set its value will be defined regarding java type of field.
Following rules applies:

- `byte` -> `Byte`
- `Byte` -> `Byte, Null`
- `short` -> `Short`
- `Short` -> `Short, Null`
- `int` -> `Integer`
- `Integer` -> `Integer, Null`
- `long` -> `Long`
- `Long` -> `Long, Null`
- `float` -> `Float`
- `Float` -> `Float, Null`
- `double` -> `Double`
- `Double` -> `Double, Null`
- `boolean` -> `Flag`
- `Boolean` -> `Flag, Null`
- `String` -> `String, Null`
- `UUID` -> `UUID, Null`

So in class:

```java
class Pojo {

    @Tag("integerTag")
    private int value;

}
```

parser will expect tag of `Integer` type.

You can redefine default behavior by setting types value:

```java
class Pojo {

    @Tag(name = "shortTag" types={"Short"})
    private int value;
}
```

In this example parser will expect tag of `Short` type.

### Deserialization of tag with many possible types

```yaml
MultitypePojo:
  multitypeTag: Short, Integer, Long
```

```java
class MultitypePojo {

    @Tag(name="multitypeTag", types={"Short", "Integer", "Long"})
    private long multitypeTag;
}
```

All types in `types` annotation parameter must be convertable to java type of field or variable.
There is standart set of converters from Hercules types to java types:

Byte:

- `Byte` -> `Byte`
- `Byte` -> `byte`
- `Byte` -> `Short`
- `Byte` -> `short`
- `Byte` -> `Integer`
- `Byte` -> `int`
- `Byte` -> `Long`
- `Byte` -> `long`

Short:

- `Short` -> `Short`
- `Short` -> `short`
- `Short` -> `Integer`
- `Short` -> `int`
- `Short` -> `Long`
- `Short` -> `long`

Integer:

- `Integer` -> `Integer`
- `Integer` -> `int`
- `Integer` -> `Long`
- `Integer` -> `long`

Long:

- `Long` -> `Long`
- `Long` -> `long`

Float:

- `Float` -> `Float`
- `Float` -> `float`
- `Float` -> `Double`
- `Float` -> `double`

Double:

- `Double` -> `Double`
- `Double` -> `double`

Flag:

- `Flag` -> `Boolean`
- `Flag` -> `boolean`

String:

- `String` -> `String`

UUID:

- `UUID` -> `UUID`

Null:

- `Null` -> `Byte`
- `Null` -> `Short`
- `Null` -> `Integer`
- `Null` -> `Long`
- `Null` -> `Float`
- `Null` -> `Double`
- `Null` -> `Boolean`
- `Null` -> `String`
- `Null` -> `UUID`
- `Null` -> `Object`

### Deserialization of optional tag

In case of optional tag

```yaml
PojoWithOptionalTag:
  optionalString?: String
```

java type must be enclosed in Maybe generic type

```java
class PojoWithOptionalString {

    @Tag("optionalString", optional = true)
    private Maybe<String> optionalString;

    // Getters and setters
}
```

Unnecessary wrapping can be avoided by passing special parameter `missingAsNull = true` to `@Tag` annotation:

```java
class PojoWithOptionalString {

    @Tag("optionalString", optional = true, missingAsNull = true)
    private String optionalString;

    // Getters and setters
}
```

In case of optional tag with possible null-value such parameter cannot be true and using `Maybe` wrapper is obligatory.

### Missing required tags

In case of missing required tags during deserialization a `MissingRequiredTagsException` will be thrown.

### Deserialization of container

Nested object in hercules protocol are passed via nested containers.

```yaml
MainPojo:
  innerPojoTag: InnerPojo
InnerPojo:
  stringTag: String
```

Such schema can be described in java code as follows:

```java
class MainPojo {

    @Tag(name = "innerPojoTag")
    private InnerPojo innerPojo;
}

class InnerPojo {

    @Tag("stringTag")
    private String stringTag;
}
```

# Use hercules mapper

To make serialization and deserialization more easy you can use `HerculesMapper`.

## Primitive types

If you want to map a simple pojo for this schema:

```yaml
PojoClass:
  integerTag: Integer
```

you should add `@Tag` annotation the following way:

```java
class PojoClass {

    @Tag("integerTag")
    private int integerTag;

    // Getters and setters
}
```

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

So in example abowe on serizlization java field `integerTag` of type `int` will be converted into `Integer` hercules value.
On deserizlization mapper will expect tag with `Integer` value.
In case of type mismatch mapper will throw `TypeMismatchException`.

You can redefine default behavior by setting types value:

```java
class Pojo {

    @Tag(name = "shortTag" types={"Short"})
    private int value;
}
```

In this example on serialization mapper will convert `int` java value to `Short` hercules value
or throw `InvalidValueException` in case of overflow.
On deserialization mapper will expect tag of `Short` type and store its value in `int` field.

## Multiple possible types

In Hercules protocol you can set tag to value of different types.

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

## Immutable class

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

## Optional tags

In case of optional tag

```yaml
PojoWithOptionalTag:
  optionalString?: String
```

java type must be enclosed in Maybe generic type

```java
class PojoWithOptionalString {

    @Tag(name = "optionalString", optional = true)
    private Maybe<String> optionalString;

    // Getters and setters
}
```

Unnecessary wrapping can be avoided by passing special parameter `missingAsNull = true` to `@Tag` annotation:

```java
class PojoWithOptionalString {

    @Tag(name = "optionalString", optional = true, missingAsNull = true)
    private String optionalString;

    // Getters and setters
}
```

In case of optional tag with possible null-value such parameter cannot be true and using `Maybe` wrapper is obligatory.

## Missing required tags

In case of missing required tags during deserialization a `MissingRequiredTagsException` will be thrown.

## Containers

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

    @Tag("innerPojoTag")
    private InnerPojo innerPojo;
}

class InnerPojo {

    @Tag("stringTag")
    private String stringTag;
}
```

## Vector of primitives

Vector of primitive values can be mapped to array of primitive (vectorTag1) or boxed (vectorTag2) arrays,
ordered (vectorTag3) or unordered collections (vectorTag4) as follows:

```yaml
PojoWithVector:
  vectorTag1: Vector<Integer>
  vectorTag2: Vector<Integer>
  vectorTag3: Vector<Integer>
  vectorTag4: Vector<Integer>
```

```java
class PojoWithVector {

    @Tag("vectorTag1")
    private int[] vectorTag1;

    @Tag("vectorTag2")
    private Integer[] vectorTag2;

    @Tag("vectorTag3")
    private List<Integer> vectorTag3;

    @Tag("vectorTag4")
    private Set<Integer> vectorTag4;
}
```

## Vector of containers

For more complex structure there is no much difference

```yaml
Node:
  data: String
  childs: Vector<Node>
```

```java
class Node {

    @Tag("data")
    private String data;

    @Tag("childs")
    private Node[] childs;
}
```

## Vector of vectors and more nested structures

```yaml
Matrix:
  data: Vector<Vector<Integer>>
```

Vector of vectors can be mapped to two-dimensional array:

```java
class Matrix {

    @Tag("data")
    private int[][] data;
}
```
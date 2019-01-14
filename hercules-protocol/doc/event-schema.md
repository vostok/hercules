# Hercules event schema description

Each hercules event contains common data like version, id and timestamp.
Additional data can be passed via tags as described in [hercules protocol](../README.md).
Tag set is actualy a container and both can be described the same way.

The main purpose of schema description is to create a common language to describe event structure for different group of developers.
At this moment there is no instruments of event validation.

## Container

To describe container type you have to describe all possible tags.

```yaml
TypeName:
  tagName: Integer
  anotherTagName: Double
```

If tag can contain value of several types, all of that types must be enlisted in square brackets and separated by comma:

```yaml
AnotherTypeName:
  multyTypeTagName: [Integer, String]
```

In this example integer, string and null values will be valid.

### Optional tags

Tag can be optional, i.e. event with missing optional tag still valid.
Optional tags must be marked by `?` in the end of tag name:

```yaml
TypeWithOptionalTag:
  requiredTag: Integer
  optionalTag?: Integer
```

Remember that `Null` type is not the same as optional tag.
In the next example tag can be optional but cannot contain null value:

```yaml
SomeType:
  optionalTag?: [Integer, String]
```

## Primitive types

Part of hercules types are primitive:

- Byte
- Short
- Integer
- Long
- Flag
- Float
- Double
- String
- Uuid
- Null

Tags of such values described by type name:

```yaml
SomeType:
  byteTagName: Byte
  shortTagName: Short
  integerTagName: Integer
  longTagName: Long
  flagTagName: Flag
  floatTagName: Float
  doubleTagName: Double
  stringTagName: String
  uuidTagName: Uuid
  nullableTagName: [Integer, "Null"] # Tag with only Null type value is useless
```

Note that `Null` type is surrounded by quote marks `"`.
This is because of `Null` without quotes is yaml null-value.

## Vector

To describe vector type you have to specify its element type in angle brackets:

```yaml
Vector<Integer>
```

Vectors can be nested:

```yaml
Vector<Vector<Integer>>
```

Description of container with vector tag will be as follows:

```yaml
TypeWithVectors:
  integerVectorTag: Vector<Integer>
  uuidVectorTag: Vector<Uuid>
```

## Nested containers

Container can have nested containers. 
All tags for nested containers must be enlisted to:

```yaml
TypeWithNestedContainer:
  integerTag: Integer
  flagTag: Flag
  nestedContainerTag:
    nestedIntegerTag: Integer
    nestedFloatTag: Float
```

As alternate variant type of the nested container can be described separately:

```yaml
TypeWithNestedContainer:
  nestedIntegerTag: Integer
  nestedFloatTag: Float
SomeType:
  integerTag: Integer
  flagTag: Flag
  nestedContainerTag: NestedType
```

In some cases this is the only option to describe type, e.g. tree-like structures:

```yaml
TreeNode:
  value: String
  left?: TreeNode
  right?: TreeNode
```

## Aliases

You can create aliases for types simply by enumerated aliased value(s):

```yaml
Number: [Byte, Short, Integer, Long, Float, Double]
```

Aliases can be useful if some type set is common for many tags:

```yaml
OptionalInteger: [Integer, "Null"]
SomeType:
  integerTag: OptionalInteger
  anotherIntegerTag: OptionalInteger
  andAnotherTag: OptionalInteger
  andHereCanBeStringAlso: [OptionalInteger, String]
```

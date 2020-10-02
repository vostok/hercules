# HPath
**HPath** describes a hierarchy of tags.
Each parent tag must be a Container.
Leaf tag can be any of type.
Currently, HPath does not support individual elements of inner vector tag.

String representation of a HPath consists of tag names are joined by slash `/`.

## Match tag
HPath is used to match specific tag from a hierarchy of tags. See examples below.

Matching top-level tag with name `tagName`:
```plaintext
tagName
```

Matching inner tag with name `innerTagName` inside container tag with name `containerTagName`:
```plaintext
containerTagName/innerTagName
```

## Empty HPath
String representation of an empty HPath is an empty string. Also, it's possible to define an empty HPath from the string "/".

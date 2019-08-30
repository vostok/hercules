# HPath
**HPath** describes hierarchy of tags.
Each parent tag must be a Container.
Leaf tag can be any of type.
Currently, HPath does not support individual elements of inner vector tag.

String representation of HPath consists of tag names are joined by slash `/`.

## Match tag
HPath is used to match specific tag from hierarchy of tags. See examples below.

Matching top-level tag with name `tagName`:
```plaintext
tagName
```

Matching inner tag with name `innerTagName` inside container tag with name `containerTagName`:
```plaintext
containerTagName/innerTagName
```

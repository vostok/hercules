# H-path

H-path is similar to json path. It can be used to match tags in complex nested hierarchy of containers.

- `$` - root mark
- `.` - hierarchy separator
- `[i]` - array element matcher
- `[*]` - array any element matcher
- `*` - any tag matcher
- `**` - any tag sequence mathcher, match any nested container sequence or no container at all

## Match tag

To match specific tag use its name:

```plaintext
$.tagName
```

To match specific vector element use vector tag name and its index:

```plaintext
$.arrayTagName[0]
```

To match all vector elements use vector tag name and any element matcher

```plaintext
$.arrayTagName[*]
```

To match inner tag use its name and names of all parent tags separated by hierarchy separator `.`:

```plaintext
$.tagName.innerTagName
```

To match all child tags use name of parent tag and any tag matcher

```plaintext
$.tagName.*
```

To match inner tag with specific name in all parent tags use any tag matcher instead of specific name:

```plaintext
$.*.innerTagName
```

To match inner tag with specific name in all possible combination of parent hierarchy use any tag sequence matcher

```plaintext
$.**.innerTagName
```

# HPath

HPath is similar to XPath. It can be used to describe tag position in complex nested hierarchy of containers.
At this moment HPath does not support inner elements of vector.

## Match tag

All path must be started from root element `/`.

To match specific tag use its name:

```plaintext
/tagName
```

To match inner tag use its name and names of all parent tags separated by hierarchy separator `/`:

```plaintext
/tagName/innerTagName
```

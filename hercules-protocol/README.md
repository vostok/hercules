# Hercules Protocol

Minimal data unit of Hercules is **Hercules Event**. Binary protocol is used to store and transfer Hercules Events. Binary protocol is explained below.

Each event contains a set of Tags. Tag is pair of Name and Value, where Name is string with Tag's name, Value is value of Tag with one of several possible types.

## Types

List of types which are supported in Hercules.

| Type      | Description                                        | Size | C#         | Java    |
|-----------|----------------------------------------------------|------|------------|---------|
| Container | Collection of Tags                                 | *    | Dictionary | Map     |
| Byte      | Unsigned integer number from 0 to 255 inclusively  | 1    | byte       | byte    |
| Short     | Signed integer number                              | 2    | short      | short   |
| Integer   | Signed integer number                              | 4    | int        | int     |
| Long      | Signed integer number                              | 8    | long       | long    |
| Flag      | Unsigned integer number of 0 or 1                  | 1    | bool       | boolean |
| Float     | Single-precision floating-point number             | 4    | float      | float   |
| Double    | Double-precision floating-point number             | 8    | double     | double  |
| String    | UTF-8 encoded string                               | *    | string     | String  |
| UUID      | Universally unique identifier                      | 16   | Guid       | UUID    |
| Null      | Representation of null value                       | 0    | null       | null    |
| Vector    | Array of values with one of types above            | *    | Array      | Array   |
| DataType  | Data type                                          | 1    | enum       | enum    |

Here, 
- Column 'Size' means value size in bytes. Also, `*` means variable size
- Columns 'C#' and 'Java' contain language-specific analogues
- Floating-points numbers are in [IEEE 754](https://en.wikipedia.org/wiki/IEEE_754) format
- UUID format is defined in [RFC 4122](https://tools.ietf.org/html/rfc4122)

It is important to note that `byte` is signed integer value from `-128` to `127` in Java.

## Binary format of Hercules Event

Binary protocol is used to store and transfer Hercules Events.

### DataType

| Data type | Value |
|-----------|-------|
| Container | 0x01  |
| Byte      | 0x02  |
| Short     | 0x03  |
| Integer   | 0x04  |
| Long      | 0x05  |
| Flag      | 0x06  |
| Float     | 0x07  |
| Double    | 0x08  |
| String    | 0x09  |
| UUID      | 0x0A  |
| Null      | 0x0B  |
| Vector    | 0x80  |

### VarLen

VarLen is VLQ-encoded unsigned integer.
VLQ is Variable-length quantity format.

See [wikipedia](https://en.wikipedia.org/wiki/Variable-length_quantity) for details.

### Binary representation

<pre>
Event = Version Timestamp Random Payload
        ; Hercules Event

Version = <i>Byte</i>
        ; Protocol version is used

Timestamp = <i>Long</i>
        ; Event timestamp in 100-ns ticks from Gregorian Epoch (1582-10-15T00:00:00.000Z)

Random = <i>UUID</i>
        ; Event identifier is used to deduplicate events with the same timestamp

Payload = <i>Container</i>
        ; Event body 

Container = Count, Tag*
        ; Collection of Tags

Count = <i>VarLen</i>
        ; Total Tags count 

Tag = Key <i>DataType</i> Value
        ; Tag of Event

Key = <i>String</i>
        ; Name of Tag

String = Size <i>Byte</i>*
        ; String

Size = <i>VarLen</i>
        ; String size in bytes

Value = <i>Container</i> | <i>Byte</i> | <i>Short</i> | <i>Integer</i> | <i>Long</i> |
        <i>Flag</i> | <i>Float</i> | <i>Double</i> | <i>String</i> | <i>UUID</i> | <i>Null</i> | Vector
        ; Value

Vector = <i>DataType</i> Length Value*
        ; Vector

Length = <i>VarLen</i>
        ; Length of Vector
</pre>

Where,  
`X*` means "repeat `X` zero or more times"  
`X | Y` means "`X` or `Y`"  
`; commentary` is commentary.

# Hecules Partitioner 
Partitioner is used to distribute events over partitions.

There are two implemented partitioners:
- RandomPartitioner uses round robin algorithms
- HashPartitioner uses hash function

## HashPartitioner
Special ShardingKey is used to determine which tags of the Event should be used in hash function.
Note, it's possible to use nested Containers.
Thus, ShardingKey consists of a sequence of tag paths.
Those paths are defined in Stream or Timeline entities. Each path is a sequence of tags joined with a dot. See example below.

```text
topLevelTag.secondLevelTag.stringTag
```
Here, path consists of two nested containers (`topLevelTag` and `secondLevelTag`) and leaf tag `stringTag`.

### Hash function
Hash function is defined by Hasher's implementation. See NaiveHasher - the naive hash function implementation.

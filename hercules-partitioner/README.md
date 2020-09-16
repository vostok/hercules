# Hercules Partitioner 
Partitioner is used to distribute events over partitions.

Currently, there are two implemented partitioners:
- RandomPartitioner uses round robin algorithm,
- HashPartitioner uses hash function

## HashPartitioner
Special ShardingKey is used to determine which tags of the Event should be used in hash function.
Note, it's possible to use nested Containers.
Thus, ShardingKey consists of a sequence of tag paths in [HPath](../hercules-protocol/doc/h-path.md) form.
See example below:
```plaintext
topLevelTag/secondLevelTag/stringTag
```
Here, path consists of two nested containers (`topLevelTag` and `secondLevelTag`) and leaf tag `stringTag`.

Those paths are defined in Stream or Timeline entities.

### Hash function
Hash function is defined by Hasher's implementation. See NaiveHasher - the naive hash function implementation.

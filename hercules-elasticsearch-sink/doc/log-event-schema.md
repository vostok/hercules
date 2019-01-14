# LogEvent schema

Elasticsearch-sink does not apply any special requirements for LogEvent format except one:
tag `properties` should contain container with common tags from [common tags](../../hercules-protocol/doc/common-tags.md).

```yaml
LogEvent:
  properties: Properties
```

Also `Properties` can contain special tags for use in elasticsearch-sink:

```yaml
Properties:
  elk-index?: String
```

Where:

- `elk-index` - can be used to define special index name for LogEvent.

Index of LogEvent is defined by folowing rules

1. If `elk-index` tag exists form index as `${elk-index}-${date}`
  where
    `${elk-index}` is value of `elk-index` tag,
    `${date}` is UTC date from timestamp of event in `YYYY-MM-DD` format.
2. If `project` tag exists form index as `${project}-${service}-${environment}-${date}`:
  where
    `${project}` is value of `project` tag,
    `${service}` is value of `service` tag,
    `${environment}` is value of `environment` tag,
    `${date}` is UTC date from timestamp of event in `YYYY-MM-DD` format.
    If `service` or `environment` tags are missing theirs value and corresponding hyphen will be skipped.
3. If none of above tags exists ignore event.

# TraceSpan

Schema of TraceSpan described below:

```yaml
TraceSpan:
  traceId: Uuid
  spanId: Uuid
  parentSpanId?: Uuid
  beginTimestampUtc: Long # Expressed in 100-ns ticks from Unix epoch
  beginTimestampUtcOffset?: Long # Expressed in 100-ns ticks
  endTimestampUtc?: Long # Expressed in 100-ns ticks from Unix epoch
  endTimestampUtcOffset?: Long # Expressed in 100-ns ticks
  annotations?: Container # Key-value dictionary where values are primitives or string representation in case of object
```

## Services which can process TraceSpan

At this moment there is no implemented services which can process TraceSpan.
In future a group of such services will be created.

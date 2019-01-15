# LogEvent

Schema of LogEvent described below:

```yaml
LogEvent:
  utcOffset?: Long # Utc offset on 100ns ticks
  messageTemplate?: String # Message template
  renderedMessage?: String # Rendered message
  exception?: Exception # Exception
  properties?: Container # Key-value dictionary where values are primitives or string representation in case of object
Exception:
  type?: String # Exception runtime type
  message?: String # Exception message
  innerExceptions?: Vector<Exception> # Inner exceptions
  stackTrace?: Vector<StackFrame> # Exception stack frame
StackFrame:
  function?: String # Name of function
  type?: String # Type where function is declared
  file?: String # File name
  line?: Integer # Line number
  column?: Short # Column number
```

## Services which can process LogEvent

- elasticsearch-sink can process LogEvent as described in [its documentation](../../hercules-elasticsearch-sink/doc/log-event-schema.md)

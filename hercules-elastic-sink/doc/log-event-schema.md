# LogEvent schema

Elastic Sink does not apply any special requirements for [LogEvent](../../doc/event-schema/log-event-schema.md) format and event can be extended with any additional tags. 

Also, special tag `properties/elk-index` of type String can be present. See explanation below.

Thus, the name of index in Elastic is defined as follows:
1. If `elk-index` tag exists then the index name would be `${elk-index}-${date}`,  
  where  
    `${elk-index}` is the value of the `properties/elk-index` tag,  
    `${date}` is UTC date from timestamp of the event in `YYYY.MM.DD` format.
2. If `project` tag exists then the index name would be `${project}-${subproject}-${environment}-${date}`,  
  where  
    `${project}` is the value of the `properties/project` tag,  
    `${subproject}` is the value of the `properties/subproject` tag, if absent then used value of the `properties/application` tag,  
    `${environment}` is the value of the `properties/environment` tag,  
    `${date}` is UTC date from timestamp of event in `YYYY.MM.DD` format.  
    If `subproject/application` or `environment` tags are missing theirs value and corresponding hyphen will be skipped.
3. If none of above tags exists ignore event.

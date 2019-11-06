# Common tags

Common tags can be used in different application and store meta information about environment, project etc.

```yaml
CommonTags:
  project?: String
  subproject?: String
  environment?: String
```

## Hierarchy of projects

We use two level hierarchy (like GitHub, Sentry etc.).
Tag `project` represents top level hierarchy, it can be the name of your project,
group or just the name of your service, in case of monolith project.

Tag `subproject` represents part of your project, it can be front-end and back-end or some additional service,
e. g. mail service, background worker etc.

Tags `subproject` should be used only with tag project.

## Environment

Tag `environment` represents environment of running application, i.e. `production`, `testing`, `stress-testing` etc.

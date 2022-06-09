## `jar` target

Create JAR and POM files for the project

### Usage

```bash
clojure -T:project jar
```

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`jar` target instead of running on the project.

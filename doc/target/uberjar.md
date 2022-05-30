## `uberjar` target

Create standalone JAR for the project

### Usage

```bash
clojure -T:project uberjar
```

### Additional project behavior

- `:exoscale.project/uberjar?`: Only produce the uberjar when set to `true`. Defaults to `false`.

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`uberjar` target instead of running on the project.

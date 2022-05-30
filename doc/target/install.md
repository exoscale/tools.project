## `install` target

Perform a local install of jars

### Usage

```bash
clojure -T:project install
```

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`install` target instead of running on the project.

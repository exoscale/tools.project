## `clean` target

Removes target directories

### Usage

```bash
clojure -T:project clean
```

### Additional project configuration

- `:exoscale.project/extra-clean-targets`: An optional collection of directories to
  clean.

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`clean` target instead of running on the project.

## `check` target

Eagerly loads all project namespaces. Errors out if any namespaces
fail to load. Resources directories are excluded from the namespace
search.

### Usage

```bash
clojure -T:project check
```

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`check` target instead of running on the project.

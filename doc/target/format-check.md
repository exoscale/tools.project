## `format-check` target

Perform a format check with the help of [clj-fmt](https://github.com/weavejester/cljfmt)

### Usage

```bash
clojure -T:project format-check
```

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`format-check` target instead of running on the project.

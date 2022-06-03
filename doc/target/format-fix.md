## `format-fix` target

Perform a format check with the help of [clj-fmt](https://github.com/weavejester/cljfmt)

### Usage

```bash
clojure -T:project format-fix
```

### Configuration

The following options may be provided

- `:exoscale.project/source-path-exclusions`: A regex of paths to exclude, defaults to `#"(resources|^target|^classes)"`


### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`format-fix` target instead of running on the project.

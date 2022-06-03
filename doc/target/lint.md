## `lint` target

Perform a
Perform linting of source files through [clj-kondo](https://github.com/clj-kondo/clj-kondo).

### Usage

```bash
clojure -T:project lint
```

### Configuration

The following options may be provided

- `:exoscale.project/source-path-exclusions`: A regex of paths to exclude, defaults to `#"(resources|^target|^classes)"`

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`lint` task instead of running on the project.

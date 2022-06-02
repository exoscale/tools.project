## `prep` target

Run the [prep task](https://clojure.org/guides/deps_and_cli#prep_libs) of dependent libraries where applicable.
To avoid unnecessarily running in all sub modules, predicated
on `:exoscale.project/needs-prep?` in the deps edn file.

### Usage

```bash
clojure -T:project prep
```

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`prep` target instead of running on the project, only accounting
for those having `:exoscale.project/needs-prep?` set to true.

In standalone projects, the flag is not honored and `clojure -X:deps prep`
is ran regardless.

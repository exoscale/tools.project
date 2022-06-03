## `prep-self` target

Run the [prep task](https://clojure.org/guides/deps_and_cli#prep_libs) of the current
project where applicable.
To avoid unnecessarily running in all sub modules, predicated
on `:deps/prep-lib` in the deps edn file.

This is implicitly ran for `check`, `jar`, and `uberjar` and thus is only useful
as a debugging facility.

### Usage

```bash
clojure -T:project prep-self
```

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`prep-self` target instead of running on the project, only accounting
for those having `:deps/prep-lib` set to true.

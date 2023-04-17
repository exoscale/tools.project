## `uberjar` target

Create standalone JAR for the project

### Usage

```bash
clojure -T:project uberjar
```

### Additional project behavior

- `:exoscale.project/uberjar?`: Only produce the uberjar when set to `true`. Defaults to `false`.
- `:exoscale.project/compile-opts`: A map of options as for [`compile-clj`](https://clojure.github.io/tools.build/clojure.tools.build.api.html#var-compile-clj).

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`uberjar` target instead of running on the project.  
Additionally, a `resources/VERSION` file is added to the uberjar, containing
the specified project's version.

## `merge-aliases` target

Handle dependency merging throughout profile with [deps-modules](https://github.com/exoscale/deps-modules).
Modules to merge are inferred:

- When `:exoscale.deps/deps-files` is present, its value is used;
- Otherwise, when `:exoscale.project/modules` is present in configuration, deps-file is
  inferred from the list of modules;
- Otherwise, `:exoscale.deps/deps-files` is assumed to be `["deps.edn"]`

When inferring deps file configuration from the list of modules, it may be useful
to add additional files to the list of entries processed. This can be done with
the `:exoscale.deps/extra-deps-files` key.

### Usage

```bash
clojure -T:project merge-aliases
```

### Multi-module project behavior

No specific behavior is enforced for multi-module or standalone repositories.

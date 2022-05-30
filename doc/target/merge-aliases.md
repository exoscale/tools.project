## `merge-aliases` target

Handle dependency merging throughout profile with [deps-modules](https://github.com/exoscale/deps-modules).
Modules to merge are inferred:

- When `:exoscale.deps/deps-files` is present, its value is used;
- Otherwise, when `:exoscale.project/modules` is present in configuration, deps-file is
  inferred from the list of modules;
- Otherwise, `:exoscale.deps/deps-files` is assumed to be `["deps.edn"]`

### Usage

```bash
clojure -T:project merge-aliases
```

### Multi-module project behavior

No specific behavior is enforced for multi-module or standalone repositories.

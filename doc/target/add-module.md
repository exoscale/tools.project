## `add-module` target

Requires a `:name` argument on the command line

``` shell
clojure -T:project add-module :name com.example/my-module
```

Adds a new module to the project. This target will perform the
following actions:

- Create a new directory within the `modules/` directory with a skeleton
  for the module
- Add a reference to the module in the top level `deps.edn`

You will likely want to run the [`merge-deps`](merge-deps.md) and
[`merge-aliases`](merge-aliases.md) targets shortly after having ran
this target.

Note that once you add a first module, the top-level `deps.edn` file
will be assumed not to contain anything relevant. If you want the
top-level project to be part of the projects considered by targets,
you will need to add `"."` to the collection of modules stored
in `:exoscale.project/modules`

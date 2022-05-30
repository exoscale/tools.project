## `init` target

This target will initialize a new project. This is the only
target that is expected to be ran outside of a specific project.

Requires a `:name` argument on the command line

``` shell
clojure -T:project init :name com.example/my-project
```

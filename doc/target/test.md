## `test` target

This is a dummy target which calls `clojure -X:test` in the current
project. In multi-module projects, it runs through all configured
modules unless `:exoscale.project/testable?` is set to false in the
project file and calls `clojure -X:test`.

Note that it is still on each individual projects to set up the `:test`
alias correctly.

The default project templates wires kaocha as the runner for convenience.

All commands are chainable. They take a context map an return a potentially
updated one.

Most commands use built'ins from clojure/tools.deps and clojure/tools.build.

You can add preset project keys in a edn file that will be loaded for every
project command. By default we assume you're doing that in `deps.edn` but you
can specify an `:exoscale.project/file` at invocation time if you want to
separate them (ex in a `project.edn` file alongside of `deps.edn`, **at Exoscale
we tend to prefer to have everything in deps.edn**). You can also specify an
:exoscale.project/keypath with the location in the loaded edn file. It defaults
to root.

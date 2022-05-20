# exoscale tools.project

This is a small `clojure.tool` that allows to work with projects in a
streamlined way.

It provides an api to generate jars, uberjar, deploy, install, clean, release
etc...

All commands are chainable. They take a context map an return a potentially
updated one.

Most commands use built'ins from clojure/tools.deps and clojure/tools.build. The
only exception is `deploy` which uses slipset/deps-deploy (for now).  When
tools.deploy lands it will replace it. For that reason the context key for the
deploy tasks is now under `:slipset.deps-deploy/exec-args`.

You can add preset project keys in a edn file that will be loaded for every
project command. By default we assume you're doing that in `deps.edn` but you
can specify an `:exoscale.project/file` at invocation time if you want to
separate them (ex in a `project.edn` file alongside of `deps.edn`, **at exoscale
we tend to prefer to have everything in deps.edn**). You can also specify an
:exoscale.project/keypath with the location in the loaded edn file. It defaults
to root.

A typical deps.edn file could look like this:
``` clj
{:exoscale.project/lib exoscale/foo-orchestator
 :exoscale.project/version "1.0.0-SNAPSHOT"

 :deps { ... }
 :aliases { ... }
 :paths [ ... ]
 :mvn/repos { ... }}
```

## Installation

Add this to your deps.edn

``` clj
  :project {:deps {com.exoscale/tools.project {:git/sha "..." :git/url "git@github.com:exoscale/tools.project.git"}}
            :ns-default exoscale.tools.project.module}
```

Or install the tool locally.

From there you can run most commands from the cli:

* `clj -T:project install`
* `clj -T:project uberjar`
* `clj -T:project jar`
* `clj -T:project compile`
* `clj -T:project deploy`


Or use the api directly via `exoscale.tools.project.api`.  Commands merely
forward to api calls. API calls also return an upated context, so that can be
useful for chaining commands in an efficient way.

## deps.edn/project.edn

see [project.sample.edn](project.sample.edn)

All keys are static, it's purely declarative, this library will make no attempt
to modify its contents.

## Tasks

You have the possibility to define/create tasks for your projects, under the
`:exoscale.project/tasks` key.  Tasks essentially allow you to run
fns/shell-commands/other-tasks from clj directly and compose them.  That is how,
for instance most project level, complex, tasks are constructed (ex bump
version + commit + release).

Then in :exoscale.project/tasks we have tasks definitions, a map of task id ->
task def.

In your deps.edn
``` clj
{:exoscale.project/tasks
 {:foo [{:exoscale.project.task{:run :some.fn/f :args {}}}]
  :bar [{:exoscale.project.task{:run :some.fn/f
                                :args {}
                                :for-all [:exoscale.project/libs]}}
        {:exoscale.project.task{:ref :foo}}
        {:exoscale.project.task{:shell ["echo ho ho ho"]
                                :for-all [:exoscale.project/deployables]}}]}}
```


A Task is just a map `#:exoscale.project.task{...}`

We have 3 type of tasks for now:

* :run tasks `#:exoscale.project.task{:run some.ns/fn}` that will trigger an
  invocation of that task

* :shell tasks `#:exoscale.project.task{:shell [\"ping foo.com\" \"ping
  bar.com\"]}` that will trigger an invocation of the shell commands listed in
  order

* :ref tasks `#:exoscale.project.task{:ref :something}` that will invoke another task


Great, but why are we doing this?

Tasks can be repeated against a coll of values if you specify a :for-all key
`#:exoscale.project.task{:run some.ns/fn :for-all :exoscale.project/libs]}` the
task will then be run for all modules listed under that key in the deps.edn
file, in (execution) context, in a single process potentially. Tasks can also
refer each other. Essentially a declarative version of lein sub that supports
composability and is not spawning as many processes as we have tasks.

Some implementation notes:

* all commands run in the same process, with the exception of :shell commands of
  course, that means an equivalent of `lein sub install` runs quite fast (inside
  1 jvm process).
* we take care of setting the modules path via tools.deps & build for every
  command on separate modules.

It's a tool, so invocation is of the `-T` form: `clj -T:project task :id
:install`, meaning we potentially support run-time args as well, the classpath
is also isolated from the lib cp. But because it's a tool we have no access to
other aliases in context.


### Tasks & exoscale.tools.project.module

If your default-ns is set on `exoscale.tools.project.module` and not
`exoscale.tools.project` all commands invoked via -T will perform some magic for
multi-module projects.

If you have a :exoscale.project/libs and/or :exoscale.project/deployables key
set in your deps.edn and the command is run from the root of the project we will
assume the -T command should run on all modules.

That means `clj -T:project install` will run install on all modules when invoked
from the root and the same command from a module dir will just install that module.

That works for any task (release, uberjar, jar, compile, clean, etc...).

If that disturbs you you can simply set your default-ns to exoscale.tools or call tasks directly via
`clj -T:project task :id your-task-id`

## clj-new template

TODO


## UX Suggestion

We suggest you add the following aliases to your bash profile, all these plugins
should be available from any module.

``` bash
## common exoscale tools deps aliases
alias clj-test/repl='clj -A:test'
alias clj-test/unit='clj -X:test:test/unit'
alias clj-test/integration='clj -X:test:test/integration'
alias clj-test/all='clj -X:test:test/all'
alias clj-merge-deps='clj -T:deps-modules exoscale.deps-modules/merge-deps'
alias clj-fmt/lint='clj -M:cljfmt check'
alias clj-fmt/format='clj -M:cljfmt fix'
alias clj-antq='clj -M:antq outdated'
```

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
separate them (ex in a `project.edn` file alongside of `deps.edn`). You can also
specify an :exoscale.project/keypath with the location in the loaded edn
file. It defaults to root.

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
            :ns-default exoscale.tools.project}
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

## clj-new template 

TODO

## UX Suggestion 

We suggest you add the following aliases to your bash profile, all these plugins
should be available from any module.

``` bash
## common exoscsale tools deps aliases
alias clj-test/repl='clj -A:test'
alias clj-test/unit='clj -X:test:test/unit'
alias clj-test/integration='clj -X:test:test/integration'
alias clj-test/all='clj -X:test:test/all'
alias clj-merge-deps='clj -T:deps-modules exoscale.deps-modules/merge-deps'
alias clj-fmt/lint='clj -M:cljfmt check'
alias clj-fmt/format='clj -M:cljfmt fix'
alias clj-antq='clj -M:antq outdated'
```

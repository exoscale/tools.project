# exoscale tools.project

This is a small `clojure.tool` that allows to work with projects in a
streamlined way.

It provides an api to generate jars, uberjar, deploy, install, clean, release
etc...

All commands are chainable. They take a context map an return a potentially
updated one.

It's possible to also provide a project.edn (aero) file for a project that will
be used as a base for context for the commands mentioned before.  We merge the
default options with the read project contents and lastly with potential
arguments passed to the commands.

Most commands use built'ins from clojure/tools.deps and clojure/tools.build. The
only exception is `deploy` which uses slipset/deps-deploy (for now).  When
tools.deploy lands it will replace it. For that reason the context key for the
deploy tasks is now under `:slipset.deps-deploy/exec-args`.

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


## project.edn

see [project.sample.edn](project.sample.edn)

It's loaded as an aero file. All keys are static, it's purely declarative, this
library will make no attempt to modify its contents. You are expected to
namespace keys, if you don't they'll be assumed to be part of `:exoscale.project/*`

# exoscale tools.project

A `clojure.tool` that allows to work with projects in a streamlined way.
It provides an api for common project management tasks, inspired by
the conventions enforced by [leiningen](http://leiningen.org)

## Installation

Add this to your deps.edn

``` clj
  :project {:deps {com.exoscale/tools.project {:git/sha "..." :git/url "git@github.com:exoscale/tools.project.git"}}
            :ns-default exoscale.tools.project}
```

Or install the tool locally:

``` bash
clojure -Ttools install com.exoscale/tools.project {:git/sha "..." :git/url ""} :as project
```

## Usage

With the tool installed, a new project can be initialized with:

``` bash
clojure -Tproject init :name com.example/my-lib
```

Once installed, tools can called using standard tool call syntax, within the project
directory:

``` bash
clojure -T:project jar
```

A convenience `Makefile` is provided with projects bootstrapped with
`clojure -Tproject init`, which can assist in discovering the tool.

## Supported targets

- [add-module](doc/target/add-module.md): creates a new submodule
- [check](doc/target/check.md): validates that namespaces can be loaded
- [clean](doc/target/clean.md): cleans target directories
- [deploy](doc/target/deploy.md): deploys jars to remote repositories
- [format-check](doc/target/format-check.md): source code format check
- [format-fix](doc/target/format-fix.md): source code format fixing
- [init](doc/target/init.md): initialize new project
- [install](doc/target/install.md): local maven installation
- [jar](doc/target/jar.md): JAR and POM generation
- [lint](doc/target/lint.md): source code linting
- [merge-deps](doc/target/merge-deps.md): managed dependency management
- [merge-aliases](doc/target/merge-aliases.md): managed alias management
- [outdated](doc/target/outdated.md): outdated version check
- [prep](doc/target/prep.md): handle prep libs
- [release](doc/target/release.md): project release
- [task](doc/target/task.md): arbitrary task run
- [test](doc/target/test.md): run tests
- [uberjar](doc/target/uberjar.md): standalone JAR generation

## Configuration

A few keys control the configuration of the project. A minimum configuration
is shown below:

```clojure
{:exoscale.project/lib com.example/my-lib
 :exoscale.project/version-file "VERSION"
 :exoscale.project/uberjar? true

 :paths ["src" "resources"]
 :deps {org.clojure/clojure {:mvn/version "1.11.1"}}}
```

See [configuration options](doc/config-options.md) for a list of
all valid options.

## Multi-module projects

Some projects need to build several artifacts, or to expose different libraries.
This is what we refer to as multi-module projects.

**tools.project** supports multi-module projects out of the box, and provides the
following functionality:

- All targets adopt a default behavior that is dependent on whether they are called
  for a *multi-module*, or *standalone* project.
- Default facilities are present to share dependencies and aliases across modules (through [deps-modules](https://github.com/exoscale/deps-modules)

Let's assume the following module structure:

```
superproject
├── deps.edn          ;; com.example/superproject-common
└── modules
    ├── client
    │   └── deps.edn  ;; com.example/superproject-client
    └── server
        └── deps.edn  ;; com.example/superproject-server
```

The corresponding top-level `deps.edn` file would
add the following:

``` clojure
{:exoscale.project/lib com.example/superproject-common
 :exoscale.project/version-file "VERSION"
 :exoscale.project/modules ["." "modules/client" "modules/server"]

 :paths ["src" "resources"]
 :deps {org.clojure/clojure {:mvn/version "1.11.1"}}}
```

**tools.project** supports seamless transitions from standalone projects
to multi module ones. See [`add-module`](doc/target/add-module.md) for
help on how to add a first module.

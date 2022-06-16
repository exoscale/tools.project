## `release` target

Perform a release of the project. This is merely a [task](../task.md) run
for the `:release/single` task in standalone projects, and for the `:release/modules`
task in multi-module projects.

### Usage

```bash
clojure -T:project release
```

### Additional project configuration

Since `release` is a task, the default task configuration can be overridden
using standard task configuration:


``` clojure
:exoscale.project/tasks
{:release/single [{:shell ["echo noop"]}]
 ;; or
 :release/modules [{:shell ["echo module noop"]}]}
```

The default release task configuration is:

``` clojure
[{:run :exoscale.tools.project.standalone/version-remove-snapshot}
 {:run :exoscale.tools.project.standalone/deploy} ;; {:ref :deploy} is used for multi module projects
 {:run :exoscale.tools.project.standalone/git-commit-version}
 {:run :exoscale.tools.project.standalone/git-tag-version}
 {:run :exoscale.tools.project.standalone/version-bump-and-snapshot}
 {:run :exoscale.tools.project.standalone/git-commit-version}
 {:run :exoscale.tools.project.standalone/git-push}]
```

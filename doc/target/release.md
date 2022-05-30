## `release` target

Perform a release of the project. This is merely a [task](../task.md) run
for the `:release` task.

### Usage

```bash
clojure -T:project release
```

### Additional project configuration

Since `release` is a task, the default task configuration can be overriden
using standard task configuration:


``` clojure
:exoscale.project/tasks
{:release [#:exoscale.project.task {:shell ["echo noop"]}]}
```

The default release task configuration is:

``` clojure
[#:exoscale.project.task{:run :exoscale.tools.project.standalone/version-remove-snapshot}
 #:exoscale.project.task{:ref :deploy}  ;; as for clj -T:project deploy
 #:exoscale.project.task{:ref :uberjar} ;; as for clj -T:project uberjar
 #:exoscale.project.task{:run :exoscale.tools.project.standalone/git-commit-version}
 #:exoscale.project.task{:run :exoscale.tools.project.standalone/git-tag-version}
 #:exoscale.project.task{:run :exoscale.tools.project.standalone/version-bump-and-snapshot}
 #:exoscale.project.task{:run :exoscale.tools.project.standalone/git-commit-version}
 #:exoscale.project.task{:run :exoscale.tools.project.standalone/git-push}]
```

### Multi-module project behavior

No specific behavior is enforced for multi-module or standalone repositories.

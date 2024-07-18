## `release` target

Perform a release of the project. This is merely a [task](../task.md) run
for the `:release/single` task in standalone projects, and for the `:release/modules`
task in multi-module projects.

### Usage release+tag task

This is a simple release task that will release the artifact and not commit the
version file (if any provided), and then git tag the release simply. That can be
used with version-fn to create releases based on git-revs-count or timestamp
schemes (see: `exoscale.tools.project.api.version/git-count-revs` &
`exoscale.tools.project.api.version/epoch`).

```bash
clojure -T:project release+tag
```

The default release+tag task is:

``` clojure
[{:run :exoscale.tools.project.standalone/deploy}
 {:run :exoscale.tools.project.standalone/git-tag-version}
 {:run :exoscale.tools.project.standalone/git-push}]
```


### Usage release task

This task will trigger the update of the VERSION file provided and commit it, then trigger a release

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

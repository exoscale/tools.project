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
 {:foo [{:run :some.fn/f :args {}}]
  :bar [{:run :some.fn/f
         :args {}
         :for-all [:exoscale.project/libs]}
        {:ref :foo}
        {:shell ["echo ho ho ho"]
         :for-all [:exoscale.project/deployables]}]}}
```


A Task is just a map `{...}`

We have 3 type of tasks for now:

* :run tasks `{:run some.ns/fn}` that will trigger an
  invocation of that task

* :shell tasks `{:shell [\"ping foo.com\" \"ping bar.com\"]}` that will
  trigger an invocation of the shell commands listed in
  order

* :ref tasks `{:ref :something}` that will invoke another task


Great, but why are we doing this?

Tasks can be repeated against a coll of values if you specify a :for-all key
`{:run some.ns/fn :for-all :exoscale.project/libs]}` the
task will then be run for all modules listed under that key in the deps.edn
file, in (execution) context, in a single process potentially. Tasks can also
refer each other. Essentially a declarative version of lein sub that supports
composability and is not spawning as many processes as we have tasks.

Filtering on the list of target modules can be performed:

      {:run some.ns/fn :for-all [:exoscale.project/modules]
       :when :exoscale.project/should-run?}

Additionally, a default value can be provided:

      {:run some.ns/fn :for-all [:exoscale.project/modules]
       :when :exoscale.project/bypass?}

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

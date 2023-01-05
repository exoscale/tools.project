# Testing against a specific branch

To test this project within the context of another project, either use a different sha, or point to 
this library via local coordinates:

```clojure
{:extra-deps {io.github.exoscale/tools.project {:git/sha "my-branch-complete-sha"}}}
             
;; or
{:extra-deps {io.github.exoscale/tools.project {:local/root "../tools.project"}}}
```

Then you can run & optionally debug the process:

```shell
clj -J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -T:project test :kaocha.filter/focus '[:integration]'
;; connect remote debugger on port 5005
```
## Project configuration

### Sample config

### Standalone project

``` clojure
{:exoscale.project/lib com.example/my-lib
 :exoscale.project/version-file "VERSION"
 :exoscale.project/uberjar? true ;; false by default

 :paths ["src" "resources"]

 ;; exec-args as for slipset-deploy
 :slipset.deps-deploy/exec-args
 {:installer :remote :sign-releases? false :repository "clojars"}

 :deps {org.clojure/clojure {:mvn/version "1.11.1"}}

 :aliases
 {:test
  {:extra-deps {lambdaisland/kaocha {:exoscale.deps/inherit :all, :mvn/version "1.66.1034"}}

   :extra-paths ["test" "resources" "test/resources"]

   :exec-fn  kaocha.runner/exec-fn}
  :project
  {:extra-deps
    {com.exoscale/tools.project {:git/sha "faf6d89d64d8ab76a4ff5c8256488ca213aa1a38"
                                 :git/url "https://github.com/exoscale/project"}}
   :ns-default exoscale.tools.project}}}
```

### Multi-module project

``` clojure
{:exoscale.project/lib com.example/my-local-server
 :exoscale.project/version-file "VERSION"
 :exoscale.project/modules ["." "modules/client" "modules/server"]

 :exoscale.project/remote-deploy? false ;; true by default
 :exoscale.project/uberjar? true ;; false by default

 :paths ["src" "resources"]

  :deps {com.example/my-client {:local/root "modules/client", :exoscale.deps/inherit :all}
        com.example/my-server {:local/root "modules/server", :exoscale.deps/inherit :all}}

 :aliases
 {:test
  {:extra-deps {lambdaisland/kaocha {:exoscale.deps/inherit :all, :mvn/version "1.66.1034"}}

   :extra-paths ["test" "resources" "test/resources"]

   :exec-fn  kaocha.runner/exec-fn
   :exoscale.deps/inherit :all} ;;
  :project
  {:extra-deps
    {com.exoscale/tools.project {:git/sha "faf6d89d64d8ab76a4ff5c8256488ca213aa1a38"
                                 :git/url "https://github.com/exoscale/project"}}
   :exoscale.deps/inherit :all
   :ns-default exoscale.tools.project}}

 ;; Aliases that may need to be forwarded to downstream projects, only useful in
 ;; multi-module projects
 :exoscale.deps/managed-aliases
 {:project
  {:extra-deps
    {com.exoscale/tools.project {:git/sha "faf6d89d64d8ab76a4ff5c8256488ca213aa1a38"
                                 :git/url "https://github.com/exoscale/project"}}
   :ns-default exoscale.tools.project}
  :test
  {:extra-deps {lambdaisland/kaocha {:exoscale.deps/inherit :all, :mvn/version "1.66.1034"}}

   :extra-paths ["test" "resources" "test/resources"]

   :exec-fn  kaocha.runner/exec-fn
   :exoscale.deps/inherit :all}}

 ;; Dependencies that may need to be forwarded to downstream projects, only useful in
 ;; multi-module projects
 :exoscale.deps/managed-dependencies
 {org.clojure/clojure        #:mvn{:version "1.11.1"}
  org.clojure/tools.logging  #:mvn{:version "1.2.4"}

  com.example/my-client {:local/root "modules/client"}
  com.example/my-server {:local/root "modules/server"}}}
```

### Project module

``` clojure
{:exoscale.project/lib          com.example/my-client
 :exoscale.project/version-file "../../VERSION"

 :paths ["src" "resources"]

 :slipset.deps-deploy/exec-args
 {:installer :remote :sign-releases? false :repository "clojars"}

 :deps {org.clojure/clojure {:exoscale.deps/inherit :all, :mvn/version "1.11.1"}}

 :aliases
 {:test
   {:extra-deps {lambdaisland/kaocha {:exoscale.deps/inherit :all, :mvn/version "1.66.1034"}}
    :exec-fn kaocha.runner/exec-fn
    :exoscale.deps/inherit :all}
  :project
  {:extra-deps
    {com.exoscale/tools.project {:git/sha "faf6d89d64d8ab76a4ff5c8256488ca213aa1a38"
                                 :git/url "https://github.com/exoscale/project"}}
   :exoscale.deps/inherit :all
   :ns-default exoscale.tools.project}}}
```

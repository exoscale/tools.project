# exoscale tools.project

This is a small clojure.tool that allows to work with projects in a streamlined way. 

It provides an api to generate jars, uberjar, deploy, install, clean, release etc

It also allows to provide a project.edn (aero) file for a project that will be
used as a base for arguments for the commands mentioned before. 

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


Or use the api directly via `exoscale.tools.project.api`. 
Commands merely forward to api calls. API calls also return an upated context, so that can be useful for chaining commands in an efficient way. 


## project.edn  

TODO


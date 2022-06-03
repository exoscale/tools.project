## `deploy` target

Performs a remote deploy of the project's configured artifact
through [deps-deploy](https://github.com/slipset/deps-deploy).

### Usage

```bash
clojure -T:project deploy
```

### Additional project configuration

- `:exoscale.project/remote-deploy?`: Remote deploys are not attempted when set to `false`. Defaults to `true
- `:slipset.deps-deploy/exec-args`: Exec arguments for [deps-deploy](https://github.com/slipset/deps-deploy).

Note that by default if pushing to object storage, an Exoscale SOS bucket is assumed.

### Multi-module project behavior

When a `:exoscale.project/modules` key is present in the project's
configuration, runs through all configured module to call the
`deploy` target instead of running on the project.

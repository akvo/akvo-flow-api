
## Akvo FLOW API


## Building and testing

## Dependencies

* `make` (via Xcode on macOS)
* [Docker](https://docs.docker.com/engine/installation/)
* [curl](https://curl.haxx.se/)
* [jq](https://stedolan.github.io/jq/)

### Build

    $ ./ci/build.sh

### Start

	$ make start # assumes lumen-keycloak container is running
	$ # or #
	$ make start-full # wait few seconds to Keycloak starts and imports the Akvo realm

#### Starting API backend (WIP)

Assuming a `secrets` folder is in the current directory

    $ docker run --rm -p 3000:3000 --interactive --tty --volume "$PWD/secrets":/secrets akvo/flow-api-backend


### Test

    $ ./util/test-proxy.sh

## Other info

* Available users username/password:
  * demo1/akvo123
  * demo2/akvo123

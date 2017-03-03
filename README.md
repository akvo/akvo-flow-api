
## Akvo FLOW API


## Building and testing

## Dependencies

* `make` (via Xcode on macOS)
* [Docker](https://docs.docker.com/engine/installation/)
* [curl](https://curl.haxx.se/)
* [jq](https://stedolan.github.io/jq/)

### Build

    $ make buid

### Start

	$ make start
	$ # wait few seconds to Keycloak starts and imports the Akvo realm


### Test

    $ ./nginx/test-proxy.sh

## Other info

* Available users username/password:
  * demo1/akvo123
  * demo2/akvo123

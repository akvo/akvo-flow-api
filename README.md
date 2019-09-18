
## Akvo FLOW API

[![Build Status](https://travis-ci.org/akvo/akvo-flow-api.svg?branch=develop)](https://travis-ci.org/akvo/akvo-flow-api)

## Building and testing

## Dependencies

* [Docker compose](https://docs.docker.com/engine/installation/)

### Start dev env

	docker-compose up
	
REPL available in port 47480.
Backend available in port 3000. 

### Deployment to production

Run:

    ./ci/promote-test-to-prod.sh

And follow the instructions
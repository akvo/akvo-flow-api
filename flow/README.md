## Akvo Flow data access library


We can build the latest released Akvo Flow code and use the
the data access classes as dependency.


## Building

    ./build.sh

This will pull the image Docker image `akvo/flow-api-build`
and compile the code inside a running container

## Updating the image

    ./get-dependencies.sh
    docker build -t akvo/flow-api-build .
    docker push akvo/flow-api-build

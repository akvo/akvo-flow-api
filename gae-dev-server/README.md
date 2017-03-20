
# GAE development server

## Requirements

https://cloud.google.com/appengine/docs/standard/java/tools/maven

* JDK 8
* Maven 3.3.9+

More info at: 

## Test data

GAE development server uses a local file to simulate the Datastore.
This file is located in the the following path:
`./target/stub-server-1.0-SNAPSHOT/WEB-INF/appengine-generated/local_db.bin`

If you copy a file with test data it will persist the changes across restarts.

## Start/Stop the server

### Sync start

    mvn appengine:devserver

## Async start/stop

    mvn appengine:devserver_start
    mvn appengine:devserver_stop


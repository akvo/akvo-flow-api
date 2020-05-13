#!/usr/bin/env bash

docker run --rm -e SLACK_CLI_TOKEN -v ~/.config:/home/akvo/.config -v "$(pwd)":/app \
  -it akvo/akvo-devops:20200513.082032.37c61a6 \
  promote-test-to-prod.sh flow-api akvo-flow-api-version akvo-flow-api
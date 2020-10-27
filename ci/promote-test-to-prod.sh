#!/usr/bin/env bash

docker run --rm -e ZULIP_CLI_TOKEN -v ~/.config:/home/akvo/.config -v "$(pwd)":/app \
  -it akvo/akvo-devops:20201026.223019.4d53287 \
  promote-test-to-prod.sh flow-api akvo-flow-api-version akvo-flow-api zulip flumen-dev
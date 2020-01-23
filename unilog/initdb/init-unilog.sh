#!/usr/bin/env bash


set -eu

db_name="u_akvoflowsandbox"

psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER}" --dbname "${POSTGRES_DB}" <<-EOSQL
    CREATE USER unilog WITH PASSWORD 'unilog';
    CREATE DATABASE "${db_name}" WITH TEMPLATE template0 ENCODING 'UTF8';
    GRANT ALL PRIVILEGES ON DATABASE "${db_name}" TO unilog;
EOSQL


psql -v ON_ERROR_STOP=1 --username unilog --dbname "${db_name}" <<-EOSQL
CREATE TABLE IF NOT EXISTS event_log (
id bigserial PRIMARY KEY,
payload jsonb UNIQUE
);

CREATE INDEX timestamp_idx ON
       event_log(cast(payload->'context'->>'timestamp' AS numeric));

CREATE UNIQUE INDEX unique_payload ON event_log (md5(payload::text));
EOSQL

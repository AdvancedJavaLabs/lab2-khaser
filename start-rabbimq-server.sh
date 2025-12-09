#!/usr/bin/env bash

RABBITMQ_MNESIA_BASE=$(pwd)/rabbitmq-db \
RABBITMQ_LOGS=$(pwd)/rabbitmq-logs \
    rabbitmq-server

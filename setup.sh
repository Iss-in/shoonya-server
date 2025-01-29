#!/bin/bash
docker rm -f shoonya-server
docker compose build --no-cache
docker compose up -d

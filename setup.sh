#!/bin/bash
cd /home/kushy/Projects/shoonya_app/shoonya-server/
docker rm -f shoonya-server
docker compose build --no-cache
docker compose up -d

#!/bin/sh
curl -v -X POST http://localhost:8082/api/reviews \
  -H 'Content-Type: application/json' \
  -d '{"frameworkId":1,"comment":"Excellent language!","rating":5}'

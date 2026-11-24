#!/usr/bin/env bash
# Smoke test against a running app with seeded demo data (local: mvn jetty:run; CI: see tests.yml).
# Logs in as each role, renders every page, and checks role restrictions and core business rules.
set -euo pipefail
BASE=${BASE:-http://localhost:8080}
JAR=$(mktemp)
CSRF=""
fails=0

status() { # status METHOD PATH [curl args...]
    local method=$1 path=$2; shift 2
    curl -s -o /dev/null -w '%{http_code}' -X "$method" -b "$JAR" -c "$JAR" "$@" "$BASE$path"
}
expect() { # expect CODE METHOD PATH [curl args...]
    local want=$1; shift
    local got; got=$(status "$@")
    if [ "$got" = "$want" ]; then echo "ok   $want $1 $2"; else echo "FAIL $1 $2: expected $want, got $got"; fails=$((fails + 1)); fi
}
login() {
    rm -f "$JAR"
    CSRF=$(curl -s -b "$JAR" -c "$JAR" "$BASE/login" | grep -o 'name="_csrf" value="[0-9a-f]*"' | head -1 | sed 's/.*value="//; s/"//')
    expect 302 POST /login --data-urlencode "_csrf=$CSRF" --data-urlencode "email=$1" --data-urlencode "password=${2:-password}"
    CSRF=$(curl -s -b "$JAR" -c "$JAR" "$BASE/profile" | grep -o 'name="_csrf" value="[0-9a-f]*"' | head -1 | sed 's/.*value="//; s/"//')
}

echo "== guest"
expect 302 GET /dashboard
expect 200 GET /login
expect 200 GET /register

echo "== admin"
login admin@example.com
for page in /dashboard /trees /trees/1 /trees/create /trees/1/edit "/trees?status=productive&sort=-planted_on" \
    /care-logs /care-logs/create "/care-logs?activity_type=watering" /harvests /harvests/create "/harvests?grade=good" \
    /categories /categories/create /categories/1/edit /tree-types "/tree-types?category_id=1" /tree-types/create \
    /blocks /blocks/1 /blocks/create /expenses "/expenses?block_id=general" /expenses/create /sales /sales/create \
    /reports "/reports?report=financial" "/reports?report=care" "/reports?report=harvest&group_by=month" \
    "/reports/export?report=harvest" "/reports/export?report=financial" "/reports/export?report=care" \
    /users /users/create /profile; do
    expect 200 GET "$page"
done
expect 404 GET /trees/999999
expect 404 GET /does-not-exist
expect 419 POST /categories --data-urlencode "name=No token"
expect 302 POST /categories --data-urlencode "_csrf=$CSRF" --data-urlencode "name=Smoke Test $RANDOM"
expect 422 POST /categories --data-urlencode "_csrf=$CSRF" --data-urlencode "name="
expect 422 POST /sales --data-urlencode "_csrf=$CSRF" --data-urlencode "harvest_id=1" --data-urlencode "buyer_name=Too Much" \
    --data-urlencode "sold_on=$(date +%F)" --data-urlencode "quantity=99999" --data-urlencode "unit_price=1000"
expect 422 POST /trees --data-urlencode "_csrf=$CSRF" --data-urlencode "tree_type_id=1" --data-urlencode "block_id=1" \
    --data-urlencode "planted_on=2999-01-01" --data-urlencode "status=growing"

echo "== staff"
login staff@example.com
for page in /dashboard /trees /trees/create /care-logs/create /harvests/create /categories /blocks/1 /profile; do
    expect 200 GET "$page"
done
for page in /expenses /sales /reports /users /categories/create /tree-types/1/edit; do
    expect 403 GET "$page"
done
expect 403 POST /trees/1/delete --data-urlencode "_csrf=$CSRF"

echo "== viewer"
login viewer@example.com
expect 200 GET /trees
expect 403 GET /trees/create
expect 403 GET /care-logs/create

rm -f "$JAR"
if [ "$fails" -gt 0 ]; then echo "$fails check(s) failed"; exit 1; fi
echo "All smoke checks passed."

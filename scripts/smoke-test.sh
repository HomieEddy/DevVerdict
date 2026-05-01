#!/bin/bash
set -e

echo "=== DevVerdict Phase 9 Smoke Test ==="
echo

# 1. Container health
echo "1. Checking container health..."
docker compose ps | grep -E "(gateway|review-service|catalog-service|auth-service|redis|angular-ui)" | grep "healthy" || {
    echo "ERROR: Not all expected containers are healthy"
    exit 1
}
echo "   All containers healthy"

# 2. Redis reachable
echo "2. Checking Redis..."
docker compose exec -T redis redis-cli ping | grep -q "PONG" || {
    echo "ERROR: Redis not responding"
    exit 1
}
echo "   Redis PONG"

# 3. Gateway routes (catalog list)
echo "3. Checking Gateway catalog route..."
curl -sf http://localhost:8085/api/catalog/frameworks > /dev/null || {
    echo "ERROR: Gateway catalog route failed"
    exit 1
}
echo "   Gateway catalog OK"

# 4. Gateway review route (list reviews for framework 1)
echo "4. Checking Gateway review route..."
curl -sf http://localhost:8085/api/reviews/framework/1 > /dev/null || {
    echo "ERROR: Gateway review route failed"
    exit 1
}
echo "   Gateway review OK"

# 5. Angular UI served by nginx
echo "5. Checking Angular UI (nginx)..."
curl -sf http://localhost:4200 > /dev/null || {
    echo "ERROR: Angular UI not reachable"
    exit 1
}
echo "   Angular UI OK"

# 6. API proxy through nginx to Gateway
echo "6. Checking API proxy through nginx..."
curl -sf http://localhost:4200/api/catalog/frameworks > /dev/null || {
    echo "ERROR: nginx API proxy failed"
    exit 1
}
echo "   nginx API proxy OK"

# 7. Rate limiting (fire 5 rapid requests, all should pass; this is a light check)
echo "7. Checking rate limiting responsiveness..."
for i in {1..5}; do
    curl -sf http://localhost:8085/api/reviews/framework/1 > /dev/null || {
        echo "ERROR: Rate limiting blocked legitimate request $i"
        exit 1
    }
done
echo "   Rate limiting allows normal traffic"

# 8. Circuit breaker fallback
echo "8. Checking circuit breaker fallback endpoint..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8085/fallback/catalog)
if [ "$STATUS" != "503" ]; then
    echo "ERROR: Fallback endpoint returned HTTP $STATUS instead of 503"
    exit 1
fi
echo "   Fallback returns 503"

echo
echo "=== All smoke tests passed ==="

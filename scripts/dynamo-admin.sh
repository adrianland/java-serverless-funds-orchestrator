#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# scripts/dynamo-admin.sh
#
# Utility commands for inspecting DynamoDB Local during development.
# Requires: AWS CLI v2 installed.
#
# Usage:
#   chmod +x scripts/dynamo-admin.sh
#   ./scripts/dynamo-admin.sh list-tables
#   ./scripts/dynamo-admin.sh scan
#   ./scripts/dynamo-admin.sh scan-client CLIENT-001
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

ENDPOINT="http://localhost:8000"
TABLE="FondosBTG"
REGION="us-east-1"

AWS_CMD="aws --endpoint-url $ENDPOINT --region $REGION"

case "${1:-help}" in

  list-tables)
    echo "📋 Tables in DynamoDB Local:"
    $AWS_CMD dynamodb list-tables
    ;;

  scan)
    echo "🔍 Scanning entire table: $TABLE"
    $AWS_CMD dynamodb scan \
      --table-name "$TABLE" \
      --output json | jq '.Items | length' | xargs -I{} echo "Total items: {}"
    $AWS_CMD dynamodb scan --table-name "$TABLE" --output table
    ;;

  scan-client)
    CLIENT_ID="${2:-CLIENT-001}"
    echo "🔍 Querying all items for client: $CLIENT_ID"
    $AWS_CMD dynamodb query \
      --table-name "$TABLE" \
      --key-condition-expression "PK = :pk" \
      --expression-attribute-values "{\":pk\":{\"S\":\"CLIENT#$CLIENT_ID\"}}" \
      --output table
    ;;

  describe-table)
    echo "📐 Table description:"
    $AWS_CMD dynamodb describe-table --table-name "$TABLE" | jq '.Table | {TableName, TableStatus, ItemCount, BillingModeSummary}'
    ;;

  help|*)
    echo "Usage: $0 {list-tables|scan|scan-client [CLIENT_ID]|describe-table}"
    ;;
esac

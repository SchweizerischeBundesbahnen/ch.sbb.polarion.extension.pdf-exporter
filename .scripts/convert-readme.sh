#!/bin/bash

set -e

INPUT_FILE="${1:-README.md}"
OUTPUT_FILE="${2:-README.html}"

# Convert the markdown file to a JSON payload
jq -R -s '{"mode": "gfm", "text": .}' < "$INPUT_FILE" > payload.json

# Send the JSON payload to the GitHub API
curl -L \
  -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/markdown \
  -d @payload.json > "$OUTPUT_FILE"

# Remove the temporary JSON payload
rm payload.json

# Remove the Build and Installation sections from readme
awk '
/<h2>Build<\/h2>/ {skip=1; next}
/<h2>Polarion configuration<\/h2>/ {skip=0}
!skip' "$OUTPUT_FILE" > "$OUTPUT_FILE.tmp" && mv "$OUTPUT_FILE.tmp" "$OUTPUT_FILE"

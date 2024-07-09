#!/bin/bash

set -e

# Check if a command is installed
function check_command() {
  if ! command -v "$1" &> /dev/null; then
    echo -e "\033[0;31m $1 is not installed! \033[0m" >&2
    echo -e "\033[0;31m Help for About page will not be generated! \033[0m" >&2

    if [ -n "$FAIL_ON_CHECK_COMMANDS" ]; then
      exit 1
    else
      exit 0
    fi
  fi
}

# Check if required commands are installed
check_command jq
check_command curl
check_command awk
check_command sed
check_command tr

INPUT_FILE="${1:-README.md}"
OUTPUT_FILE="${2:-README.html}"

# Convert the markdown file to a JSON payload
JSON_PAYLOAD=$(jq -R -s '{"mode": "gfm", "text": .}' < "$INPUT_FILE")

# Check if GITHUB_TOKEN is set
if [[ -n "$GITHUB_TOKEN" ]]; then
  AUTH_HEADER="Authorization: Bearer $GITHUB_TOKEN"
else
  AUTH_HEADER=""
fi

# Send the JSON payload to the GitHub API and capture the HTTP response code and body
HTTP_RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" -L \
  -X POST \
  -H "Accept: application/vnd.github+json" \
  ${AUTH_HEADER:+-H "$AUTH_HEADER"} \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/markdown \
  -d "$JSON_PAYLOAD")

# Extract the status and the body from the HTTP response
HTTP_BODY=$(echo "$HTTP_RESPONSE" | sed -e 's/HTTPSTATUS:.*//g')
HTTP_STATUS=$(echo "$HTTP_RESPONSE" | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')

# Check the HTTP status
if [ "$HTTP_STATUS" -ne 200 ]; then
  echo -e "\033[0;31m README.md: HTTP request failed with status $HTTP_STATUS. Response: \033[0m" >&2
  echo -e "\033[0;31m $HTTP_BODY \033[0m" >&2
  exit 1
fi

# Process the curl output with awk to remove the Build, Installation and Changelog sections
MODIFIED_CONTENT=$(echo "$HTTP_BODY" | awk '
/<h2>Build<\/h2>/ {skip=1; next}
/<h2>Polarion configuration<\/h2>/ {skip=0}
/<h2>Changelog<\/h2>/ {skip=1; next}
!skip')

# Write the modified content to the output file
echo "$MODIFIED_CONTENT" > "$OUTPUT_FILE"

echo "README.md: HTTP request successful. Output saved to $OUTPUT_FILE."

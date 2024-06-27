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

INPUT_FILE="${1:-README.md}"
OUTPUT_FILE="${2:-README.html}"

# Convert the markdown file to a JSON payload
JSON_PAYLOAD=$(jq -R -s '{"mode": "gfm", "text": .}' < "$INPUT_FILE")

# Send the JSON payload to the GitHub API
CURL_OUTPUT=$(curl -L \
  -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/markdown \
  -d "$JSON_PAYLOAD")

# Process the curl output with awk to remove the Build, Installation and Changelog sections
MODIFIED_CONTENT=$(echo "$CURL_OUTPUT" | awk '
/<h2>Build<\/h2>/ {skip=1; next}
/<h2>Polarion configuration<\/h2>/ {skip=0}
/<h2>Changelog<\/h2>/ {skip=1; next}
!skip')

# Write the modified content to the output file
echo "$MODIFIED_CONTENT" > "$OUTPUT_FILE"

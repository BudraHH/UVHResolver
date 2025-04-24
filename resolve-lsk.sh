#!/bin/bash 

API_ENDPOINT=""

if [ -z "$1" ]; then
    echo "Error: Please provide the XML file as an argument." >&2
    echo "Usage: $0 <xml_file_path>" >&2
    exit 1
fi

XML_FILE="$1"

if [ ! -f "$XML_FILE" ] || [ ! -r "$XML_FILE" ]; then
    echo "Error: Cannot read file '$XML_FILE'" >&2
    exit 1
fi

echo "Sending '$XML_FILE' to UVH Resolver..."


HTTP_STATUS=$(curl -sS -X POST \
              -H "Content-Type: application/xml" \
              --data-binary "@$XML_FILE" \
              -w "%{http_code}" \
              -o /dev/null \
              "${API_ENDPOINT}")
              
if [ "$HTTP_STATUS" -eq 200 ]; then
    echo "----------------------------------------"
    echo "Success (HTTP $HTTP_STATUS! File sent.)"
    echo "----------------------------------------"
    exit 0 
else
    echo "Error: LSK resolution failed (HTTP $HTTP_STATUS)." >&2
    echo "Server Response: (Body discarded)" >&2
    exit 1 
fi
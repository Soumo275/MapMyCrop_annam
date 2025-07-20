#!/bin/bash

echo "Downloading ZIP file from Google Drive..."

FILE_ID="1yLfbCwMy3gkpuMW2zxGwWrbabxdqUmZ_"
FILE_NAME="dataset.zip"

# Step 1: Get confirmation token for large files
CONFIRM=$(curl -sc /tmp/gcookie "https://drive.google.com/uc?export=download&id=${FILE_ID}" | \
         grep -o 'confirm=[^&]*' | sed 's/confirm=//')

# Step 2: Use token to download actual file
curl -Lb /tmp/gcookie "https://drive.google.com/uc?export=download&confirm=${CONFIRM}&id=${FILE_ID}" -o "${FILE_NAME}"

# Step 3: Unzip
echo "Unzipping to ./data ..."
mkdir -p data
unzip -o "$FILE_NAME" -d ./data

# Step 4: (Optional) Remove ZIP file
rm "$FILE_NAME"

echo "Done! Files extracted to ./data"

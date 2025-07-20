#!/bin/bash

echo "Downloading ZIP file from Google Drive..."

FILE_ID="1I5rIF8HBMpVnSTgJ9M9bDZp9Nj4QXOzr"
FILE_NAME="dataset.zip"

# Download file using the export=download URL
curl -L -o "$FILE_NAME" "https://drive.google.com/uc?export=download&id=$FILE_ID"

# Unzip
echo "📂 Unzipping to ./data ..."
mkdir -p data
unzip -o "$FILE_NAME" -d ./data

echo "Done! Files extracted to ./data"

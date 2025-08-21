#!/bin/bash

###############################################################################################
#
# syncSharedFiles.sh - Synchronizes shared Scala files between backend and frontend projects
#
# This script copies shared Scala files between the agent-registration (backend) and
# agent-registration-frontend projects. It ensures both projects have identical shared code
# by completely replacing the destination directory with files from the source.
#
# Usage:
#   ./syncSharedFiles.sh BACKEND_TO_FRONTEND  - Copy shared files from backend to frontend
#   ./syncSharedFiles.sh FRONTEND_TO_BACKEND  - Copy shared files from frontend to backend
#
# Prerequisites:
#   - Must be run from the agent-registration directory (backend root)
#   - The agent-registration-frontend directory must exist at ../agent-registration-frontend
#
# The script syncs files in the path: app/uk/gov/hmrc/agentregistration/shared/
#
###############################################################################################

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color


# Sanity check: Ensure script is run from backend root directory
current_dir=$(basename "$PWD")
if [ "$current_dir" != "agent-registration" ]; then
    echo -e "${RED}Error: This script must be run from the backend root directory (agent-registration)${NC}"
    echo -e "${RED}Current directory: $PWD${NC}"
    exit 1
fi

# Sanity check: Ensure frontend directory exists
if [ ! -d "../agent-registration-frontend" ]; then
    echo -e "${RED}Error: Frontend directory not found at ../agent-registration-frontend${NC}"
    echo -e "${RED}Make sure the agent-registration-frontend project is in the same parent directory as the backend${NC}"
    exit 1
fi

# Get direction from command line argument
DIRECTION="$1"

# Validate that direction parameter is provided
if [ -z "$DIRECTION" ]; then
    echo -e "${RED}Error: Direction parameter is required${NC}"
    echo -e "${RED}Usage: $0 {BACKEND_TO_FRONTEND|FRONTEND_TO_BACKEND}${NC}"
    exit 1
fi

case "$DIRECTION" in
    "BACKEND_TO_FRONTEND")
        SOURCE_DIR="./app/uk/gov/hmrc/agentregistration/shared"
        DEST_DIR="../agent-registration-frontend/app/uk/gov/hmrc/agentregistration/shared"
        ;;
    "FRONTEND_TO_BACKEND")
        SOURCE_DIR="../agent-registration-frontend/app/uk/gov/hmrc/agentregistration/shared"
        DEST_DIR="./app/uk/gov/hmrc/agentregistration/shared"
        ;;
    *)
        echo -e "${RED}Error: Invalid direction '$DIRECTION'${NC}"
        echo -e "${RED}Usage: $0 {BACKEND_TO_FRONTEND|FRONTEND_TO_BACKEND}${NC}"
        exit 1
        ;;
esac

echo "Syncing from $SOURCE_DIR to $DEST_DIR"

# Remove entire destination directory to ensure clean sync
rm -rfv "$DEST_DIR"

find "$SOURCE_DIR" -name "*.scala" -type f | while read file; do
    # Get relative path from SOURCE_DIR
    rel_path="${file#$SOURCE_DIR/}"
    dest_file="$DEST_DIR/$rel_path"
    # Create directory structure in destination
    mkdir -p "$(dirname "$dest_file")"
    # Copy the file
    cp -v "$file" "$dest_file"
done

# Count files for success message
total_files=$(find "$SOURCE_DIR" -name "*.scala" -type f | wc -l)
echo
echo -e "${GREEN}✓ Successfully synced $total_files Scala files from $SOURCE_DIR to $DEST_DIR${NC}"

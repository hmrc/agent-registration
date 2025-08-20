
# agent-registration

This is a companion backend microservice to the agent-registration-frontend

# Shared code

This repository contains code in the `uk.gov.hmrc.agentregistration.shared` package which is meant to be shared with the `agent-registration-frontend` microservice.

The source code for these shared components is located under:
```
app/uk/gov/hmrc/agentregistration/shared
```

## Important
This `agent-registration` repository is the source of truth for the shared code. It is the **developer's responsibility** to synchronize any changes with the `agent-registration-frontend` microservice to prevent integration issues.

The code is meant to be copied from here, and it should override all corresponding files in the frontend. The recommended synchronization process is as follows:
1. First, delete all files within the shared package in the `agent-registration-frontend` project.
2. Then, copy the updated files from this repository and paste them into the corresponding package in the `agent-registration-frontend` project.

## Synchronising files

This will copy shared scala files from this project to the frontend. 

```bash
#!/bin/bash

SOURCE_DIR="./app/uk/gov/hmrc/agentregistration/shared"
DEST_DIR="../agent-registration-frontend/app/uk/gov/hmrc/agentregistration/shared"

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
```

...or vice versa (from frontend to this project).
Hint: use intellij local history if you screw up.

```bash
#!/bin/bash

DEST_DIR="./app/uk/gov/hmrc/agentregistration/shared"
SOURCE_DIR="../agent-registration-frontend/app/uk/gov/hmrc/agentregistration/shared"

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
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
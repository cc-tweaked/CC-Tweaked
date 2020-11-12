#!/usr/bin/env bash

set -eu

DEST="${GITHUB_REF#refs/*/}"
echo "Uploading docs to https://tweaked.cc/$DEST"

# Setup ssh key
mkdir -p "$HOME/.ssh/"
echo "$SSH_KEY" > "$HOME/.ssh/key"
chmod 600 "$HOME/.ssh/key"

# And upload
rsync -avc -e "ssh -i $HOME/.ssh/key -o StrictHostKeyChecking=no -p $SSH_PORT" \
      "$GITHUB_WORKSPACE/build/docs/lua/" \
      "$SSH_USER@$SSH_HOST:/var/www/tweaked.cc/$DEST"

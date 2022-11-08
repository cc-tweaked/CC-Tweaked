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
      "$GITHUB_WORKSPACE/projects/web/build/site/" \
      "$SSH_USER@$SSH_HOST:/$DEST"
rsync -avc -e "ssh -i $HOME/.ssh/key -o StrictHostKeyChecking=no -p $SSH_PORT" \
      "$GITHUB_WORKSPACE/projects/common-api/build/docs/javadoc/" \
      "$SSH_USER@$SSH_HOST:/$DEST/javadoc"

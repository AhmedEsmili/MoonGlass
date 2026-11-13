#!/data/data/com.termux/files/usr/bin/bash
# Usage: bash termux_push.sh [repo-name]
set -e
REPO="${1:-MoonGlass}"
pkg update -y && pkg install -y git gh
termux-setup-storage || true
gh auth status >/dev/null 2>&1 || gh auth login
git config user.name  >/dev/null || git config --global user.name  "$(gh api user -q .login)"
git config user.email >/dev/null || git config --global user.email "$(gh api user -q .login)@users.noreply.github.com"
[ -d .git ] || git init -b main
git add -A && git commit -m "MoonGlass widget" || true
gh repo create "$REPO" --public --source=. --remote=origin --push
echo "Waiting for GitHub Actions build..."
sleep 15
gh run watch --exit-status
gh run download -n MoonGlass-apk -D ~/storage/downloads/
echo "APK saved to Downloads/app-debug.apk"

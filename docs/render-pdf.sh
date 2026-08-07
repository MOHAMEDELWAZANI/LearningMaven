#!/usr/bin/env bash
# Regenerate "Bookshop Master Roadmap Guide.pdf" from the markdown source.
# Requires: pandoc, weasyprint
set -euo pipefail

cd "$(dirname "$0")"

SRC="bookshop-master-roadmap.md"
OUT="Bookshop Master Roadmap Guide.pdf"
HTML="$(mktemp -t roadmap-XXXXXX.html)"
trap 'rm -f "$HTML"' EXIT

pandoc "$SRC" \
    --standalone \
    --from=gfm \
    --to=html5 \
    --metadata title="Bookshop Master Roadmap Guide" \
    --css=roadmap.css \
    -o "$HTML"

weasyprint "$HTML" "$OUT"

echo "Wrote $OUT"

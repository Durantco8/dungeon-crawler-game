#!/usr/bin/env bash
#
# Writes a coverage badge from JaCoCo's CSV report.
#
# Self-contained on purpose: no third-party action and no external coverage service, so nothing about
# this project leaves the repository in order to draw a number on the README.
#
# The view package and the launcher are excluded. They are JavaFX, they are not unit tested by design,
# and including them would report a number that says more about how much UI there is than about how well
# the game logic is covered.
#
# Usage: .github/scripts/coverage-badge.sh [csv] [output.svg]

set -euo pipefail

CSV="${1:-target/site/jacoco/jacoco.csv}"
OUT="${2:-.github/badges/coverage.svg}"

if [[ ! -f "$CSV" ]]; then
  echo "No JaCoCo report at $CSV; run the tests first." >&2
  exit 1
fi

PERCENT=$(awk -F, '
  NR == 1 { next }                      # header
  $2 ~ /\.view$/ { next }               # JavaFX, not unit tested
  $3 == "Main" { next }                 # the launcher
  { missed += $8; covered += $9 }
  END {
    total = missed + covered
    if (total == 0) { printf "0.0"; exit }
    printf "%.1f", 100 * covered / total
  }
' "$CSV")

# Green when healthy, amber when slipping, red when it needs attention.
ROUNDED=${PERCENT%.*}
if (( ROUNDED >= 90 )); then
  COLOUR="#4c1"
elif (( ROUNDED >= 75 )); then
  COLOUR="#dfb317"
else
  COLOUR="#e05d44"
fi

LABEL="coverage"
VALUE="${PERCENT}%"
# Roughly 7px per character, plus padding, so the two halves fit their text.
LABEL_WIDTH=$(( ${#LABEL} * 7 + 10 ))
VALUE_WIDTH=$(( ${#VALUE} * 7 + 10 ))
TOTAL_WIDTH=$(( LABEL_WIDTH + VALUE_WIDTH ))
LABEL_MID=$(( LABEL_WIDTH * 5 ))
VALUE_MID=$(( (LABEL_WIDTH + VALUE_WIDTH / 2) * 10 ))

mkdir -p "$(dirname "$OUT")"
cat > "$OUT" <<SVG
<svg xmlns="http://www.w3.org/2000/svg" width="${TOTAL_WIDTH}" height="20" role="img" aria-label="${LABEL}: ${VALUE}">
  <title>${LABEL}: ${VALUE}</title>
  <linearGradient id="s" x2="0" y2="100%">
    <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
    <stop offset="1" stop-opacity=".1"/>
  </linearGradient>
  <clipPath id="r"><rect width="${TOTAL_WIDTH}" height="20" rx="3" fill="#fff"/></clipPath>
  <g clip-path="url(#r)">
    <rect width="${LABEL_WIDTH}" height="20" fill="#555"/>
    <rect x="${LABEL_WIDTH}" width="${VALUE_WIDTH}" height="20" fill="${COLOUR}"/>
    <rect width="${TOTAL_WIDTH}" height="20" fill="url(#s)"/>
  </g>
  <g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" font-size="110" text-rendering="geometricPrecision">
    <text x="${LABEL_MID}" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="$(( (LABEL_WIDTH - 10) * 10 ))">${LABEL}</text>
    <text x="${LABEL_MID}" y="140" transform="scale(.1)" textLength="$(( (LABEL_WIDTH - 10) * 10 ))">${LABEL}</text>
    <text x="${VALUE_MID}" y="150" fill="#010101" fill-opacity=".3" transform="scale(.1)" textLength="$(( (VALUE_WIDTH - 10) * 10 ))">${VALUE}</text>
    <text x="${VALUE_MID}" y="140" transform="scale(.1)" textLength="$(( (VALUE_WIDTH - 10) * 10 ))">${VALUE}</text>
  </g>
</svg>
SVG

echo "$PERCENT"

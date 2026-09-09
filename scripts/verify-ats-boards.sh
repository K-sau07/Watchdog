#!/bin/bash
# Probe each candidate against the EXACT poller URL. Keep only 200s with a real board.
probe() {
  local src="$1" slug="$2" url code count
  case "$src" in
    GREENHOUSE) url="https://boards-api.greenhouse.io/v1/boards/$slug/jobs?content=true";;
    LEVER)      url="https://api.lever.co/v0/postings/$slug?mode=json";;
    ASHBY)      url="https://api.ashbyhq.com/posting-api/job-board/$slug";;
  esac
  local body
  body=$(curl -s --max-time 12 -w "\n%{http_code}" "$url" 2>/dev/null)
  code=$(echo "$body" | tail -1)
  local json=$(echo "$body" | sed '$d')
  if [ "$code" = "200" ]; then
    # count jobs to confirm it's a real, non-empty board
    case "$src" in
      GREENHOUSE) count=$(echo "$json" | python3 -c "import sys,json;d=json.load(sys.stdin);print(len(d.get('jobs',[])))" 2>/dev/null || echo 0);;
      LEVER)      count=$(echo "$json" | python3 -c "import sys,json;d=json.load(sys.stdin);print(len(d) if isinstance(d,list) else 0)" 2>/dev/null || echo 0);;
      ASHBY)      count=$(echo "$json" | python3 -c "import sys,json;d=json.load(sys.stdin);print(len(d.get('jobs',[])))" 2>/dev/null || echo 0);;
    esac
    if [ "${count:-0}" -gt 0 ] 2>/dev/null; then
      echo "OK   $src $slug $count"
    else
      echo "EMPTY $src $slug 0"
    fi
  else
    echo "MISS $src $slug $code"
  fi
}
export -f probe
# run with limited parallelism to be polite
cat /tmp/candidates.txt | while read src slug; do
  [ -z "$src" ] && continue
  echo "$src|$slug"
done | xargs -P 8 -I{} bash -c 'IFS="|" read s g <<< "{}"; probe "$s" "$g"' > /tmp/probe_results.txt 2>/dev/null
echo "=== done. summary ==="
echo "OK:    $(grep -c '^OK' /tmp/probe_results.txt)"
echo "EMPTY: $(grep -c '^EMPTY' /tmp/probe_results.txt)"
echo "MISS:  $(grep -c '^MISS' /tmp/probe_results.txt)"

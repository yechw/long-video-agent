#!/bin/bash
# Frontend E2E Tests for Markdown Rendering
# Usage: ./markdown-rendering.spec.sh [app_url]
# Requires: playwright-cli installed globally

set -e

APP_URL="${1:-http://localhost:8080}"

echo "========================================"
echo "Frontend E2E Tests: Markdown Rendering"
echo "App URL: $APP_URL"
echo "========================================"

FAILED=0

# Test 1: Page loads and CDN scripts are present
echo ""
echo "Test 1: CDN scripts load correctly"
echo "-----------------------------------"

# Open page and capture output
OUTPUT=$(playwright-cli open "$APP_URL" 2>&1)
echo "$OUTPUT" > /tmp/playwright-test.log

# Check for errors in output
if echo "$OUTPUT" | grep -qi "error\|failed"; then
    echo "  ❌ Page load errors detected"
    echo "$OUTPUT" | grep -i "error\|failed" | head -5
    FAILED=1
else
    echo "  ✓ Page loaded successfully"
fi

# Test 2: Check HTML contains CDN scripts
echo ""
echo "Test 2: HTML contains CDN references"
echo "-------------------------------------"

HTML=$(curl -s "$APP_URL")

if echo "$HTML" | grep -q "marked.min.js"; then
    echo "  ✓ marked.js CDN reference found"
else
    echo "  ❌ marked.js CDN reference not found"
    FAILED=1
fi

if echo "$HTML" | grep -q "highlight.js"; then
    echo "  ✓ highlight.js CDN reference found"
else
    echo "  ❌ highlight.js CDN reference not found"
    FAILED=1
fi

if echo "$HTML" | grep -q "github-dark.min.css"; then
    echo "  ✓ highlight.js CSS reference found"
else
    echo "  ❌ highlight.js CSS reference not found"
    FAILED=1
fi

# Test 3: Check template contains markdown containers
echo ""
echo "Test 3: Template contains markdown containers"
echo "----------------------------------------------"

TEMPLATE=$(cat src/main/resources/templates/index.html)

if echo "$TEMPLATE" | grep -q 'class="markdown-content"'; then
    echo "  ✓ markdown-content container in template"
else
    echo "  ❌ markdown-content container not in template"
    FAILED=1
fi

if echo "$TEMPLATE" | grep -q 'class="markdown-rendered"'; then
    echo "  ✓ markdown-rendered container in template"
else
    echo "  ❌ markdown-rendered container not in template"
    FAILED=1
fi

if echo "$TEMPLATE" | grep -q "DOMContentLoaded"; then
    echo "  ✓ DOMContentLoaded script in template"
else
    echo "  ❌ DOMContentLoaded script not in template"
    FAILED=1
fi

if echo "$TEMPLATE" | grep -q 'th:data-content="\${smartAnswer}"'; then
    echo "  ✓ smartAnswer binds to markdown container"
else
    echo "  ❌ smartAnswer not bound to markdown container"
    FAILED=1
fi

if echo "$TEMPLATE" | grep -q 'th:data-content="\${summary}"'; then
    echo "  ✓ summary binds to markdown container"
else
    echo "  ❌ summary not bound to markdown container"
    FAILED=1
fi

# Test 4: CSS contains markdown styles
echo ""
echo "Test 4: CSS contains markdown styles"
echo "-------------------------------------"

CSS=$(curl -s "$APP_URL/style.css")

if echo "$CSS" | grep -q ".markdown-content"; then
    echo "  ✓ .markdown-content style found"
else
    echo "  ❌ .markdown-content style not found"
    FAILED=1
fi

if echo "$CSS" | grep -q ".markdown-rendered"; then
    echo "  ✓ .markdown-rendered style found"
else
    echo "  ❌ .markdown-rendered style not found"
    FAILED=1
fi

if echo "$CSS" | grep -q ".markdown-rendered pre"; then
    echo "  ✓ code block styles found"
else
    echo "  ❌ code block styles not found"
    FAILED=1
fi

# Test 5: Verify JS rendering logic
echo ""
echo "Test 5: JavaScript rendering logic"
echo "-----------------------------------"

if echo "$HTML" | grep -q "marked.setOptions"; then
    echo "  ✓ marked.setOptions found"
else
    echo "  ❌ marked.setOptions not found"
    FAILED=1
fi

if echo "$HTML" | grep -q "hljs.highlight"; then
    echo "  ✓ hljs.highlight integration found"
else
    echo "  ❌ hljs.highlight integration not found"
    FAILED=1
fi

if echo "$HTML" | grep -q "marked.parse"; then
    echo "  ✓ marked.parse found"
else
    echo "  ❌ marked.parse not found"
    FAILED=1
fi

# Cleanup
playwright-cli session-stop-all 2>/dev/null || true
playwright-cli session-delete 2>/dev/null || true

# Summary
echo ""
echo "========================================"
if [ $FAILED -eq 0 ]; then
    echo "✅ All tests passed!"
    exit 0
else
    echo "❌ Some tests failed"
    exit 1
fi

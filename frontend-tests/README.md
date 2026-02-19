# Frontend E2E Tests

Automated frontend tests using Playwright to verify markdown rendering functionality.

## Prerequisites

- [playwright-cli](https://www.npmjs.com/package/playwright-cli) installed globally:
  ```bash
  npm install -g playwright-cli
  ```
- Application running on localhost:8080 (or specify URL)

## Running Tests

```bash
# Run with default URL (http://localhost:8080)
./frontend-tests/markdown-rendering.spec.sh

# Run with custom URL
./frontend-tests/markdown-rendering.spec.sh http://localhost:3000
```

## Test Coverage

| Test | Description |
|------|-------------|
| CDN Scripts | Verify marked.js and highlight.js load correctly |
| Sample Subtitles | Verify sample subtitles can be loaded |
| Summary Markdown | Verify summary section renders markdown content |
| Smart Q&A Markdown | Verify smart Q&A section renders markdown content |
| Regular Chat Plain Text | Verify regular chat uses plain text (not markdown) |

## Integration with CI

Add to your CI pipeline:

```yaml
# Example GitHub Actions step
- name: Start Application
  run: mvn spring-boot:run &
- name: Wait for App
  run: sleep 20
- name: Run Frontend Tests
  run: ./frontend-tests/markdown-rendering.spec.sh
```

## Troubleshooting

### Playwright browser not installed

```bash
playwright-cli install
```

### Tests timeout

Increase sleep times in the script if AI responses are slow.

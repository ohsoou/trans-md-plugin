# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`trans-md-plugin` is a Node.js plugin (early stage). The entry point is `index.js`. No build step, test runner, or linter is configured yet.

## Commands

```bash
node index.js        # run the entry point
```

No test or lint commands are set up — `npm t/est` exits with an error by design until tests are added.

## Agent skills

### Issue tracker

Issues live as local markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default label vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout — `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
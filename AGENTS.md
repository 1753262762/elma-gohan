# Repository Guidelines

## Project Structure & Module Organization

This repository is currently contract- and documentation-first. `contracts/openapi.yaml` is the authoritative V0.1 API definition; `contracts/README.md` explains the agreed cross-client rules, and `contracts/validate_openapi.py` checks the contract. Planning and acceptance criteria live in `docs/`. The root Chinese-language Markdown files contain the product introduction and technical proposal.

The planned client stack is uni-app, Vue 3, and TypeScript. When application code is added, keep runtime code under `src/`, static files under `static/`, and tests beside features or in a top-level `tests/` directory. Do not duplicate API DTO definitions independently of the OpenAPI contract.

## Build, Test, and Development Commands

There is no frontend package manifest or build script yet. The current executable check is:

```powershell
py -m pip install PyYAML
py contracts/validate_openapi.py
```

The validator must print `CONTRACT_OK`. Once the frontend scaffold lands, expose standard scripts such as `npm run dev`, `npm run build`, `npm run lint`, and `npm test` through `package.json`, then document platform-specific uni-app commands in the README.

## Coding Style & Naming Conventions

Use 2-space indentation for YAML, JSON, Vue, and TypeScript; retain 4 spaces for Python. Prefer TypeScript strict mode, Vue Composition API, and small single-purpose modules. Name Vue components in PascalCase (`RecommendationCard.vue`), composables with a `use` prefix (`useLocation.ts`), and variables/functions in camelCase. Keep API field names exactly as specified in `openapi.yaml`. Never place the Amap Web Service key or server-side risk/ranking logic in client code.

## Testing Guidelines

Run the contract validator after every API change. Add focused tests with each new behavior, especially anonymous UUID headers, GCJ-02 location handling, reroll exhaustion, feedback submission, and loading/error states. Name TypeScript tests `*.spec.ts`. No coverage threshold exists yet; new tooling should establish one before CI enforcement.

## Commit & Pull Request Guidelines

The existing history uses Conventional Commit style (`docs: define v0.1 api contract`). Continue with concise prefixes such as `feat:`, `fix:`, `test:`, `docs:`, and `chore:`. Keep commits narrowly scoped. Pull requests should explain behavior and contract impact, link the relevant task or issue, list verification commands, and include screenshots for UI changes. Call out configuration changes and never commit secrets or generated build output.

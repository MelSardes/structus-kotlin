# Contributing

Thanks for taking the time to contribute to Structus!

## Ways to Contribute

- Bug reports and discussions
- Documentation improvements
- New features and refactors
- Tests

## Development Setup

### Prerequisites

- JDK 21 (the build uses a Java toolchain)
- Git
- (Optional) Node.js
  - Root tooling (semantic-release): Node >= 20
  - Documentation website (Docusaurus): Node >= 18

### Build

From the repository root:

- Build:
  - `./gradlew build`
- Run tests:
  - `./gradlew test`
- Run full checks (includes coverage verification):
  - `./gradlew check`
- Publish to your local Maven repository:
  - `./gradlew publishToMavenLocal`

## Documentation Website

The documentation website lives in `website/`.

- Install dependencies:
  - `cd website && npm ci`
- Start dev server:
  - `npm run start`
- Build:
  - `npm run build`

## Project Constraints & Conventions

### Dependency Policy (Important)

Structus is intentionally minimal and framework-agnostic.

- Production code should remain dependency-free **except** for Kotlin stdlib and `kotlinx-coroutines-core`.
- Avoid introducing additional runtime dependencies unless there is a strong justification and it is discussed/approved first.

### Public API / KDoc

The library is compiled with Kotlin explicit API mode.

- Add explicit visibility/return types for public declarations.
- Provide KDoc for public API.

### Code Style

- Keep changes small and focused.
- Prefer clear, explicit code over clever abstractions.
- Preserve package naming conventions (`com.melsardes.libraries.structuskotlin`).

## Credentials / Secrets

Never commit credentials.

If you need to publish artifacts locally or to GitHub Packages, use one of:

- Environment variables
- Your global Gradle properties (`~/.gradle/gradle.properties`)
- A local, gitignored file such as `local.properties` / `gradle.local.properties`

If you accidentally expose a token, revoke it immediately and generate a new one.

## Pull Requests

### Before Opening a PR

- Ensure `./gradlew check` passes
- Add/update tests when changing behavior
- Update documentation when changing public API

### Commit Messages

This repository uses Conventional Commits (semantic-release).

Examples:

- `feat: add domain event dispatcher`
- `fix: handle null aggregate id`
- `docs: improve quick start`
- `refactor: simplify command bus`

## Reporting Issues

- Use GitHub Issues
- Include steps to reproduce, expected behavior, and actual behavior
- Provide version, JDK version, and OS details when relevant

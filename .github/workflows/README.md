# GitHub Actions Workflows - Currently Disabled

## Status

All workflows are currently **disabled** (renamed with `.disabled` extension).

## Disabled Workflows

- `ci.yml.disabled` - Continuous Integration (build, test, code quality)
- `publish.yml.disabled` - Publish to GitHub Packages
- `release.yml.disabled` - Semantic Release automation

## Reason

The library is currently distributed via manual build and local Maven installation (`publishToMavenLocal`). GitHub Actions will be re-enabled once the publishing infrastructure is properly configured.

## Manual Installation

Users should build and install the library locally:

```bash
git clone https://github.com/structus-io/structus-kotlin.git
cd structus-kotlin
./gradlew build publishToMavenLocal
```

## Re-enabling Workflows

To re-enable the workflows in the future, simply remove the `.disabled` extension:

```bash
mv ci.yml ci.yml
mv publish.yml publish.yml
mv release.yml release.yml
```

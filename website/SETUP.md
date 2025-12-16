# Docusaurus Website Setup Guide

## Installation

```bash
cd website
npm install
```

## Development

Start the development server:

```bash
npm start
```

This opens `http://localhost:3000` with live reload.

## Build

Build the static site:

```bash
npm run build
```

Output is in `website/build/`.

## Serve Built Site

Test the production build locally:

```bash
npm run serve
```

## Deployment

### GitHub Pages

The site is configured to deploy to GitHub Pages automatically via GitHub Actions.

**Setup:**

1. Enable GitHub Pages in repository settings
2. Set source to "GitHub Actions"
3. Push changes to `main` branch
4. GitHub Actions will build and deploy automatically

**Manual deployment:**

```bash
npm run deploy
```

### Other Platforms

The `build/` directory can be deployed to:
- **Netlify**: Drag and drop `build/` folder
- **Vercel**: Connect repository and set build command to `cd website && npm run build`
- **AWS S3**: Upload `build/` contents to S3 bucket
- **Any static host**: Upload `build/` contents

## Project Structure

```
website/
├── blog/                   # Blog posts
├── docs/                   # Documentation
│   ├── getting-started/
│   ├── architecture/
│   ├── advanced/
│   ├── best-practices/
│   └── reference/
├── src/
│   ├── components/        # React components
│   ├── css/              # Custom styles
│   └── pages/            # Custom pages
├── static/               # Static assets
│   └── img/
├── docusaurus.config.ts  # Configuration
├── sidebars.ts          # Sidebar structure
└── package.json
```

## Configuration

### Site Metadata

Edit `docusaurus.config.ts`:

```typescript
const config: Config = {
  title: 'Structus',
  tagline: 'Your tagline',
  url: 'https://your-domain.com',
  baseUrl: '/',
  // ...
};
```

### Theme Colors

Edit `src/css/custom.css`:

```css
:root {
  --ifm-color-primary: #6366f1;
  /* ... */
}
```

### Navigation

Edit `docusaurus.config.ts` navbar and footer sections.

### Sidebar

Edit `sidebars.ts` to organize documentation structure.

## Writing Documentation

### Create a Doc

1. Add `.md` file to `docs/` subdirectory
2. Add frontmatter:

```markdown
---
sidebar_position: 1
title: Your Title
---

# Content
```

### Create a Blog Post

1. Add file to `blog/` with format `YYYY-MM-DD-slug.md`
2. Add frontmatter:

```markdown
---
slug: your-slug
title: Your Title
authors: [melsardes]
tags: [tag1, tag2]
---

Content

<!-- truncate -->

More content
```

## Markdown Features

### Admonitions

```markdown
:::note
This is a note
:::

:::tip
This is a tip
:::

:::warning
This is a warning
:::

:::danger
This is a danger alert
:::
```

### Code Blocks

````markdown
```kotlin
fun main() {
    println("Hello, World!")
}
```
````

### Tabs

```markdown
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

<Tabs>
  <TabItem value="gradle" label="Gradle">
    ```kotlin
    // Gradle code
    ```
  </TabItem>
  <TabItem value="maven" label="Maven">
    ```xml
    <!-- Maven code -->
    ```
  </TabItem>
</Tabs>
```

## Troubleshooting

### Build Errors

Clear cache and rebuild:

```bash
npm run clear
npm run build
```

### Port Already in Use

Change port:

```bash
npm start -- --port 3001
```

### Module Not Found

Reinstall dependencies:

```bash
rm -rf node_modules package-lock.json
npm install
```

## Resources

- [Docusaurus Documentation](https://docusaurus.io/)
- [Markdown Features](https://docusaurus.io/docs/markdown-features)
- [Deployment](https://docusaurus.io/docs/deployment)

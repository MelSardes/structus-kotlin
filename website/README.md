# Structus Documentation Website

This directory contains the source code for the Structus documentation website, built with [Docusaurus 3](https://docusaurus.io/).

## 🚀 Quick Start

### Prerequisites

- Node.js 18.0 or higher
- npm 10.0 or higher

### Installation

```bash
cd website
npm install
```

### Local Development

```bash
npm start
```

This command starts a local development server and opens up a browser window. Most changes are reflected live without having to restart the server.

### Build

```bash
npm run build
```

This command generates static content into the `build` directory and can be served using any static contents hosting service.

### Serve Built Site

```bash
npm run serve
```

This command serves the built website locally for testing.

## 📁 Project Structure

```
website/
├── blog/                   # Blog posts
│   ├── authors.yml        # Blog authors
│   └── *.md               # Blog post files
├── docs/                   # Documentation files
│   ├── intro.md           # Introduction page
│   ├── getting-started/   # Getting started guides
│   ├── architecture/      # Architecture documentation
│   ├── advanced/          # Advanced topics
│   ├── best-practices/    # Best practices
│   └── reference/         # API reference
├── src/
│   ├── components/        # React components
│   ├── css/              # Custom CSS
│   └── pages/            # Custom pages (homepage, etc.)
├── static/
│   └── img/              # Static images
├── docusaurus.config.ts   # Docusaurus configuration
├── sidebars.ts           # Sidebar configuration
├── package.json          # Dependencies
└── tsconfig.json         # TypeScript configuration
```

## 🎨 Customization

### Theme Colors

Edit `src/css/custom.css` to customize the color scheme. The current theme uses a purple/indigo palette inspired by detekt.dev.

### Homepage

Edit `src/pages/index.tsx` to customize the homepage content and layout.

### Navigation

Edit `docusaurus.config.ts` to modify the navbar and footer links.

### Sidebar

Edit `sidebars.ts` to organize the documentation structure.

## 📝 Writing Documentation

### Creating a New Doc

1. Create a new `.md` file in the appropriate `docs/` subdirectory
2. Add frontmatter at the top:

```markdown
---
sidebar_position: 1
title: Your Title
---

# Your Content
```

3. The file will automatically appear in the sidebar based on the directory structure

### Adding a Blog Post

1. Create a new `.md` file in `blog/` with the format `YYYY-MM-DD-slug.md`
2. Add frontmatter:

```markdown
---
slug: your-slug
title: Your Title
authors: [melsardes]
tags: [tag1, tag2]
---

Your content here

<!-- truncate -->

More content after the fold
```

## 🚢 Deployment

### GitHub Pages

The site can be deployed to GitHub Pages using:

```bash
npm run deploy
```

Make sure to configure the `organizationName`, `projectName`, and `url` in `docusaurus.config.ts`.

### Other Platforms

The built site in the `build/` directory can be deployed to:
- Netlify
- Vercel
- AWS S3
- Any static hosting service

## 📚 Resources

- [Docusaurus Documentation](https://docusaurus.io/)
- [Markdown Features](https://docusaurus.io/docs/markdown-features)
- [Styling and Layout](https://docusaurus.io/docs/styling-layout)

## 🤝 Contributing

Contributions to the documentation are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 👨‍💻 Creator

**Structus** was created by [Mel Sardes](https://github.com/melsardes).

## 📄 License

This documentation is part of the Structus project and is licensed under the MIT License.

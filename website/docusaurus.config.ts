import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
  title: 'Structus',
  tagline: 'A pure Kotlin JVM library for implementing Explicit Architecture',
  favicon: 'img/favicon.ico',

  url: 'https://melsardes.github.io',
  baseUrl: '/structus-kotlin/',

  organizationName: 'melsardes',
  projectName: 'structus-kotlin',

  onBrokenLinks: 'warn',
  
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },

  i18n: {
    defaultLocale: 'en',
    locales: ['en', 'fr'],
    path: 'i18n',
    localeConfigs: {
      en: {
        label: 'English',
        direction: 'ltr',
        htmlLang: 'en-US',
      },
      fr: {
        label: 'Français',
        direction: 'ltr',
        htmlLang: 'fr-FR',
      },
    },
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/melsardes/structus-kotlin/tree/main/website/',
          showLastUpdateTime: true,
          showLastUpdateAuthor: true,
          breadcrumbs: true,
          
          // Versioning configuration
          lastVersion: 'current',
          versions: {
            current: {
              label: '0.1.0 (Latest)',
              path: '',
              banner: 'none',
            },
          },
        },
        blog: {
          showReadingTime: true,
          feedOptions: {
            type: ['rss', 'atom'],
            xslt: true,
          },
          editUrl: 'https://github.com/melsardes/structus-kotlin/tree/main/website/',
          onInlineTags: 'warn',
          onInlineAuthors: 'warn',
          onUntruncatedBlogPosts: 'warn',
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    image: 'img/structus-social-card.png',
    navbar: {
      title: 'Structus',
      logo: {
        alt: 'Structus Logo',
        src: 'img/logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'tutorialSidebar',
          position: 'left',
          label: 'Docs',
        },
        {
          type: 'docsVersionDropdown',
          position: 'left',
        },
        {to: '/blog', label: 'Blog', position: 'left'},
        {
          type: 'localeDropdown',
          position: 'right',
        },

        {
          href: 'https://github.com/melsardes/structus-kotlin',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Getting Started',
              to: '/docs/intro',
            },
            {
              label: 'Core Concepts',
              to: '/docs/getting-started/core-concepts',
            },
            {
              label: 'Architecture',
              to: '/docs/architecture/overview',
            },
            {
              label: 'Roadmap',
              to: '/docs/roadmap',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/melsardes/structus-kotlin',
            },
            {
              label: 'Issues',
              href: 'https://github.com/melsardes/structus-kotlin/issues',
            },
            {
              label: 'Discussions',
              href: 'https://github.com/melsardes/structus-kotlin/discussions',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'Blog',
              to: '/blog',
            },
            {
              label: 'License',
              href: 'https://github.com/melsardes/structus-kotlin/blob/main/LICENSE',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Structus. <a href="https://github.com/melsardes/structus-kotlin" target="_blank">View on GitHub</a>`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['kotlin', 'java', 'groovy', 'gradle'],
    },
    colorMode: {
      defaultMode: 'light',
      disableSwitch: false,
      respectPrefersColorScheme: false,
    },
    announcementBar: {
      id: 'support_us',
      content:
        'If you like Structus, give it a star on <a target="_blank" rel="noopener noreferrer" href="https://github.com/melsardes/structus-kotlin">GitHub</a>!',
      backgroundColor: '#244B6B',
      textColor: '#ffffff',
      isCloseable: true,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;

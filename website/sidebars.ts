import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  tutorialSidebar: [
    {
      type: 'doc',
      id: 'intro',
      label: 'Introduction',
    },
    {
      type: 'doc',
      id: 'about',
      label: 'About',
    },
    {
      type: 'category',
      label: 'Introduction',
      items: [
        'introduction/compatibility',
      ],
    },
    {
      type: 'category',
      label: 'Getting Started',
      items: [
        'getting-started/installation',
        'getting-started/quick-start',
        'getting-started/core-concepts',
      ],
    },
    {
      type: 'category',
      label: 'Architecture',
      items: [
        'architecture/overview',
      ],
    },
    {
      type: 'category',
      label: 'Advanced Topics',
      items: [
        'advanced/cqrs-implementation',
        'advanced/transactional-outbox',
      ],
    },
    {
      type: 'category',
      label: 'Best Practices',
      items: [
        'best-practices/guidelines',
      ],
    },
    {
      type: 'category',
      label: 'Reference',
      items: [
        'reference/api-overview',
      ],
    },
    {
      type: 'category',
      label: 'AI Agent Support',
      items: [
        'ai/overview',
        'ai/library-overview',
        'ai/usage-patterns',
        'ai/code-templates',
        'ai/prompts',
      ],
    },
    {
      type: 'category',
      label: 'Learning Resources',
      items: [
        'learning-resources/coroutines',
        'learning-resources/clean-architecture',
      ],
    },
    {
      type: 'category',
      label: 'Cookbook & Recipes',
      items: [
        'cookbook/domain-modeling',
        'cookbook/event-sourcing',
      ],
    },
    {
      type: 'category',
      label: 'Integration Guides',
      items: [
        'integration-guides/spring-boot',
        'integration-guides/ktor',
      ],
    },
    {
      type: 'category',
      label: 'Playground',
      items: [
        'playground/interactive-demo',
      ],
    },
    {
      type: 'doc',
      id: 'migration-guide',
      label: 'Migration Guide',
    },
    {
      type: 'doc',
      id: 'roadmap',
      label: 'Roadmap & Future Projects',
    },
  ],
};

export default sidebars;

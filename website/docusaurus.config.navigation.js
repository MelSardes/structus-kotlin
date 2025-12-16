/**
 * Navigation configuration for Docusaurus
 * This can be imported in the docusaurus.config.ts file
 */
export const docsNavigation = {
  previous: {
    'getting-started/installation': 'intro',
    'getting-started/quick-start': 'getting-started/installation',
    'getting-started/core-concepts': 'getting-started/quick-start',
    'architecture/overview': 'getting-started/core-concepts',
    'advanced/cqrs-implementation': 'architecture/overview',
    'advanced/transactional-outbox': 'advanced/cqrs-implementation',
    'best-practices/guidelines': 'advanced/transactional-outbox',
    'reference/api-overview': 'best-practices/guidelines',
    'ai/overview': 'reference/api-overview',
    'ai/library-overview': 'ai/overview',
    'ai/usage-patterns': 'ai/library-overview',
    'ai/code-templates': 'ai/usage-patterns',
    'ai/prompts': 'ai/code-templates',
  },
  next: {
    'intro': 'about',
    'about': 'getting-started/installation',
    'getting-started/installation': 'getting-started/quick-start',
    'getting-started/quick-start': 'getting-started/core-concepts',
    'getting-started/core-concepts': 'architecture/overview',
    'architecture/overview': 'advanced/cqrs-implementation',
    'advanced/cqrs-implementation': 'advanced/transactional-outbox',
    'advanced/transactional-outbox': 'best-practices/guidelines',
    'best-practices/guidelines': 'reference/api-overview',
    'reference/api-overview': 'ai/overview',
    'ai/overview': 'ai/library-overview',
    'ai/library-overview': 'ai/usage-patterns',
    'ai/usage-patterns': 'ai/code-templates',
    'ai/code-templates': 'ai/prompts',
  },
};
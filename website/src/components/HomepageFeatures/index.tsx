import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
  title: string;
  Svg?: React.ComponentType<React.ComponentProps<'svg'>>;
  emoji?: string;
  description: JSX.Element;
};

const FeatureList: FeatureItem[] = [
  {
    title: 'Your companion for cleaner Kotlin',
    Svg: require('@site/static/img/illustrations/architecture.svg').default,
    description: (
      <>
        Structus helps you write cleaner Kotlin code with Explicit Architecture 
        principles so you can focus on what matters the most: building amazing software.
      </>
    ),
  },
  {
    title: 'Integrate in any project',
    Svg: require('@site/static/img/illustrations/integration.svg').default,
    description: (
      <>
        Structus is framework-agnostic and works seamlessly with Spring Boot, Ktor, 
        Micronaut, Quarkus, or pure Kotlin applications. No vendor lock-in.
      </>
    ),
  },
  {
    title: 'Easy to extend',
    Svg: require('@site/static/img/illustrations/extension.svg').default,
    description: (
      <>
        Built with extensibility in mind. Implement your own repositories, event publishers, 
        and command handlers while following clean architecture principles.
      </>
    ),
  },
  {
    title: 'Community Driven',
    Svg: require('@site/static/img/illustrations/community.svg').default,
    description: (
      <>
        Structus is entirely open-source and developed by the community. 
        Join us on GitHub and help us shape the future of this library.
      </>
    ),
  },
];

function Feature({title, Svg, description}: FeatureItem) {
  return (
    <div className={clsx('col col--6')}>
      <div className={styles.feature}>
        <div className={styles.featureIcon}>
          {Svg && <Svg className={styles.featureSvg} role="img" />}
        </div>
        <div className={styles.featureContent}>
          <Heading as="h3">{title}</Heading>
          <p>{description}</p>
        </div>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): JSX.Element {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
        <div className={styles.quickLinks}>
          <h2>Quick Links</h2>
          <div className={styles.linkGrid}>
            <a href="/docs/getting-started/installation" className={styles.linkCard}>
              <h3>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 3H5a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2V5a2 2 0 00-2-2zm0 16H5V5h14v14z"/>
                  <path d="M12 6l-4 4h3v4h2v-4h3l-4-4z"/>
                </svg>
                Installation Guide
              </h3>
              <p>Get started with Structus in your project</p>
            </a>
            <a href="/docs/getting-started/quick-start" className={styles.linkCard}>
              <h3>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z"/>
                  <path d="M10 7.5l6 4.5-6 4.5v-9z"/>
                </svg>
                Quick Start Tutorial
              </h3>
              <p>Build your first app in 15 minutes</p>
            </a>
            <a href="/docs/architecture/overview" className={styles.linkCard}>
              <h3>
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 5C11.31 5 10.75 5.04 10.25 5.13L9 3L6 7h5.17L12 5c1.66 0 3 1.34 3 3 0 .08-.01.15-.02.22L17.9 9.37c.06-.11.1-.23.1-.37 0-2.21-1.79-4-4-4z"/>
                  <path d="M6.34 8.44l-1.05 1.05c-.39.39-.39 1.02 0 1.41s1.02.39 1.41 0l1.05-1.05c.39-.39.39-1.02 0-1.41s-1.02-.39-1.41 0z"/>
                  <path d="M12 18c-.89 0-1.74-.2-2.5-.55l-1.17 1.17L6 15h4.17L12 18c2.21 0 4-1.79 4-4 0-.08-.01-.15-.02-.22l2.92-1.15c.06.11.1.23.1.37 0 2.21-1.79 4-4 4z"/>
                  <path d="M18.71 16.59l-1.05 1.05c-.39.39-.39 1.02 0 1.41.39.39 1.02.39 1.41 0l1.05-1.05c.39-.39.39-1.02 0-1.41s-1.02-.39-1.41 0z"/>
                </svg>
                Architecture Overview
              </h3>
              <p>Understand the core architecture</p>
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}

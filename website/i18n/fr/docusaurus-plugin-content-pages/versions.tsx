import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Link from '@docusaurus/Link';
import Translate from '@docusaurus/Translate';
import {
  useVersions,
  useLatestVersion,
} from '@docusaurus/plugin-content-docs/client';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';

function Version(): JSX.Element {
  const {siteConfig} = useDocusaurusContext();
  const versions = useVersions();
  const latestVersion = useLatestVersion();
  const currentVersion = versions.find(
    (version) => version.name === 'current',
  )!;
  const pastVersions = versions.filter(
    (version) => version !== latestVersion && version.name !== 'current',
  );
  const stableVersions = pastVersions.filter(
    (version) => !version.name.includes('-alpha') && !version.name.includes('-beta') && !version.name.includes('-rc'),
  );
  const preReleaseVersions = pastVersions.filter(
    (version) => version.name.includes('-alpha') || version.name.includes('-beta') || version.name.includes('-rc'),
  );

  const documentationRepoUrl = `${siteConfig.customFields?.githubUrl ?? siteConfig.organizationName}/${siteConfig.projectName}`;

  return (
    <Layout
      title="Versions"
      description="Page des versions Structus listant toutes les versions documentées">
      <main className="container margin-vert--lg">
        <Heading as="h1">
          <Translate id="page.versions.title">
            Versions de la Documentation Structus
          </Translate>
        </Heading>

        <div className="margin-bottom--lg">
          <Heading as="h3" id="latest">
            <Translate id="page.versions.current">
              Version actuelle (Stable)
            </Translate>
          </Heading>
          <p>
            <Translate id="page.versions.currentInfo">
              Ici vous pouvez trouver la documentation pour la version actuelle.
            </Translate>
          </p>
          <table>
            <tbody>
              <tr>
                <th>{latestVersion.label}</th>
                <td>
                  <Link to={latestVersion.path + '/intro'}>
                    <Translate id="page.versions.documentation">
                      Documentation
                    </Translate>
                  </Link>
                </td>
                <td>
                  <a href={`${documentationRepoUrl}/releases/tag/v${latestVersion.name}`}>
                    <Translate id="page.versions.release">
                      Notes de version
                    </Translate>
                  </a>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        {currentVersion !== latestVersion && (
          <div className="margin-bottom--lg">
            <Heading as="h3" id="latest">
              <Translate id="page.versions.unreleased">
                Dernière version (Non publiée)
              </Translate>
            </Heading>
            <p>
              <Translate id="page.versions.unreleasedInfo">
                Ici vous pouvez trouver la documentation pour la version en cours de développement.
              </Translate>
            </p>
            <table>
              <tbody>
                <tr>
                  <th>{currentVersion.label}</th>
                  <td>
                    <Link to={currentVersion.path + '/intro'}>
                      <Translate id="page.versions.documentation">
                        Documentation
                      </Translate>
                    </Link>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        )}

        {stableVersions.length > 0 && (
          <div className="margin-bottom--lg">
            <Heading as="h3" id="archive">
              <Translate id="page.versions.stableVersions">
                Versions stables précédentes
              </Translate>
            </Heading>
            <p>
              <Translate id="page.versions.stableVersionsInfo">
                Ici vous pouvez trouver la documentation pour les versions précédentes de Structus.
              </Translate>
            </p>
            <table>
              <tbody>
                {stableVersions.map((version) => (
                  <tr key={version.name}>
                    <th>{version.label}</th>
                    <td>
                      <Link to={version.path + '/intro'}>
                        <Translate id="page.versions.documentation">
                          Documentation
                        </Translate>
                      </Link>
                    </td>
                    <td>
                      <a href={`${documentationRepoUrl}/releases/tag/v${version.name}`}>
                        <Translate id="page.versions.release">
                          Notes de version
                        </Translate>
                      </a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {preReleaseVersions.length > 0 && (
          <div className="margin-bottom--lg">
            <Heading as="h3" id="archive">
              <Translate id="page.versions.preReleaseVersions">
                Versions préliminaires
              </Translate>
            </Heading>
            <p>
              <Translate id="page.versions.preReleaseVersionsInfo">
                Ici vous pouvez trouver la documentation pour les versions préliminaires de Structus.
              </Translate>
            </p>
            <table>
              <tbody>
                {preReleaseVersions.map((version) => (
                  <tr key={version.name}>
                    <th>{version.label}</th>
                    <td>
                      <Link to={version.path + '/intro'}>
                        <Translate id="page.versions.documentation">
                          Documentation
                        </Translate>
                      </Link>
                    </td>
                    <td>
                      <a href={`${documentationRepoUrl}/releases/tag/v${version.name}`}>
                        <Translate id="page.versions.release">
                          Notes de version
                        </Translate>
                      </a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </Layout>
  );
}

export default Version;
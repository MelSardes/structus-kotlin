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
      description="Structus Versions page listing all documented versions">
      <main className="container margin-vert--lg">
        <Heading as="h1">
          <Translate id="page.versions.title">
            Structus Documentation Versions
          </Translate>
        </Heading>

        <div className="margin-bottom--lg">
          <Heading as="h3" id="latest">
            <Translate id="page.versions.current">
              Current version (Stable)
            </Translate>
          </Heading>
          <p>
            <Translate id="page.versions.currentInfo">
              Here you can find the documentation for current released version.
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
                      Release Notes
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
                Latest version (Unreleased)
              </Translate>
            </Heading>
            <p>
              <Translate id="page.versions.unreleasedInfo">
                Here you can find the documentation for work-in-process unreleased version.
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
                Past Stable Versions
              </Translate>
            </Heading>
            <p>
              <Translate id="page.versions.stableVersionsInfo">
                Here you can find documentation for previous versions of Structus.
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
                          Release Notes
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
                Pre-release versions
              </Translate>
            </Heading>
            <p>
              <Translate id="page.versions.preReleaseVersionsInfo">
                Here you can find documentation for pre-release versions of Structus.
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
                          Release Notes
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
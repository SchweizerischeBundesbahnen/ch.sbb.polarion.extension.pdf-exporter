import { type ReactNode, useEffect, useState } from 'react';
import { PageLayout } from '@grigoriev/react-sbb-polarion';
import { DISCLAIMER_URL, PROJECT_URL, fetchArticle } from '../services/articles';

/**
 * Usage Disclaimer: the build-generated DISCLAIMER.html.
 *
 * Unlike About and User Guide there is no REST endpoint for it - generic serves `/readme` and
 * `/user-guide` only - so the article is read straight from this extension's app webapp, where
 * markdown2html writes it during the build. When it is missing (a build without the plugin run) the
 * page points at the online source, exactly as the JSP page it replaces did.
 */
export default function Disclaimer() {
  const [html, setHtml] = useState<string | null>(null);
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetchArticle(DISCLAIMER_URL)
      .then((article) => {
        if (cancelled) return;
        if (article) {
          setHtml(article);
        } else {
          setMissing(true);
        }
      })
      .catch(() => {
        if (!cancelled) setMissing(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  let content: ReactNode;
  if (missing) {
    content = (
      <p>
        No disclaimer has been generated during build. Please check{' '}
        <a href={`${PROJECT_URL}/DISCLAIMER.md`} target="_blank" rel="noreferrer">
          the online documentation
        </a>
        .
      </p>
    );
  } else if (html === null) {
    content = <p>Loading...</p>;
  } else {
    // Trusted, build-generated HTML from DISCLAIMER.md.
    content = <article className="markdown-body user-guide-page" dangerouslySetInnerHTML={{ __html: html }} />;
  }

  return <PageLayout title="Usage Disclaimer">{content}</PageLayout>;
}

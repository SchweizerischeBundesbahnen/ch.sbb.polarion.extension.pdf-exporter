import { type ReactNode, useEffect, useState } from 'react';
import { PageLayout } from '@grigoriev/react-sbb-polarion';
import useRemote from '../services/useRemote';

/** Where this extension's sources live; used when the build-generated article is missing. */
const PROJECT_URL = 'https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter';

/**
 * Usage Disclaimer: the build-generated DISCLAIMER article.
 *
 * Read from generic's `/disclaimer` endpoint, the same way About and User Guide read theirs - so the
 * page no longer needs the extension context in a static URL. The endpoint answers with an empty
 * body when nothing was generated, which is how a consumer tells "not generated" from "not
 * applicable"; that case points at the online source, exactly as the JSP page it replaces did.
 */
export default function Disclaimer() {
  const { sendRequest } = useRemote();
  const [html, setHtml] = useState<string | null>(null);
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    let cancelled = false;
    sendRequest({ method: 'GET', url: '/disclaimer' })
      .then(async (response) => {
        if (cancelled) return;
        const article = response.ok ? (await response.text()).trim() : '';
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
  }, [sendRequest]);

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

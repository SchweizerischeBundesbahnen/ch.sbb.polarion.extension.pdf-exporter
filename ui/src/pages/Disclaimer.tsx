import { Disclaimer } from '@sbb-polarion/react-sbb-polarion';
import useRemote from '../services/useRemote';

/** Where this extension's sources live; used when the build-generated article is missing. */
const PROJECT_URL = 'https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter';

/** Usage Disclaimer, rendered by the shared RSP Disclaimer over this extension's REST hook. */
export default function DisclaimerPage() {
  const { sendRequest } = useRemote();
  return <Disclaimer sendRequest={sendRequest} sourceUrl={`${PROJECT_URL}/DISCLAIMER.md`} />;
}

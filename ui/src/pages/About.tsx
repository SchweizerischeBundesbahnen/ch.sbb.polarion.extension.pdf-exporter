import { About } from '@sbb-polarion/react-sbb-polarion';
import appIcon from '../assets/app-icon.svg';
import useRemote from '../services/useRemote';

/** Feeds the shared About page this extension's REST hook, app icon and REST-token-test URL. */
export default function AboutPage() {
  const { sendRequest } = useRemote();
  return <About sendRequest={sendRequest} appIcon={appIcon} restApiUrl="/polarion/pdf-exporter/rest/api/version" />;
}

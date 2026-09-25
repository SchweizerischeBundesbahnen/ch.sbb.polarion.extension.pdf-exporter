import { Disclaimer } from '@sbb-polarion/react-sbb-polarion';
import { SOURCE_BASE_URL } from '../docs/source';
import useRemote from '../services/useRemote';

/** Usage Disclaimer, rendered by the shared RSP Disclaimer over this extension's REST hook. */
export default function DisclaimerPage() {
  const { sendRequest } = useRemote();
  return <Disclaimer sendRequest={sendRequest} sourceUrl={`${SOURCE_BASE_URL}/DISCLAIMER.md`} />;
}

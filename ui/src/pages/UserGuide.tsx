import { UserGuide } from '@sbb-polarion/react-sbb-polarion';
import useRemote from '../services/useRemote';

/**
 * The shared User Guide page, fed this extension's REST hook. The article itself is generated from
 * USER_GUIDE.md at build time and served by generic's `/user-guide` endpoint.
 */
export default function UserGuidePage() {
  const { sendRequest } = useRemote();
  return <UserGuide sendRequest={sendRequest} />;
}

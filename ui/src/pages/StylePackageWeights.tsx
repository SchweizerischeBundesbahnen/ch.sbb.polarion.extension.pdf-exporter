import { useMemo } from 'react';
import { StylePackageWeights, createStylePackageWeightsService } from '@grigoriev/react-sbb-polarion';
import useRemote from '../services/useRemote';

/**
 * PDF Exporter: Style Package Weights - the shared ordering page over this extension's own
 * `settings/style-package/weights` endpoint. Both exporters have this page, so the list itself (the
 * drag-and-drop reordering, the caret buttons, the read-only entries a project scope inherits from
 * the global one) lives in react-sbb-polarion; only the endpoint is per extension.
 */
export default function StylePackageWeightsPage() {
  const { sendRequest } = useRemote();
  const service = useMemo(() => createStylePackageWeightsService(sendRequest), [sendRequest]);
  return <StylePackageWeights title="PDF Exporter: Style Package Weights" service={service} />;
}

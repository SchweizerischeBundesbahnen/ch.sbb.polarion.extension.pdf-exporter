import { useMemo } from 'react';
import { AuthorizationSettings, createAuthorizationService } from '@sbb-polarion/react-sbb-polarion';
import useRemote from '../services/useRemote';

/** The named-settings feature the export permissions are stored under. */
const AUTHORIZATION_SETTING = 'authorization';

/**
 * PDF Exporter: Authorization - the shared role-selection page over this extension's `authorization`
 * setting. Each role set is a multi-select dropdown; the selected roles are the ones the export is
 * allowed for, and with none selected the export is unrestricted.
 */
export default function Authorization() {
  const { sendRequest } = useRemote();
  const service = useMemo(() => createAuthorizationService(sendRequest, AUTHORIZATION_SETTING), [sendRequest]);
  return (
    <AuthorizationSettings
      title="PDF Exporter: Authorization"
      service={service}
      quickHelp={
        <>
          <h3>Permissions</h3>
          <p>
            Here you can restrict who is allowed to export documents to PDF, based on Polarion global or project roles.
          </p>
          <p>
            When no role is selected the export is <b>unrestricted</b> and available to every user.
          </p>
          <p>
            As soon as at least one role is selected, only users holding one of the selected global roles or one of the
            selected roles within this project are allowed to export. All other users receive an authorization error.
          </p>
          <p>Project administrators can further tune the allowed roles for their specific project.</p>
        </>
      }
    />
  );
}

import React from 'react';
import ReactDOM from 'react-dom/client';
import { configureGenericModules } from '@grigoriev/react-sbb-polarion';
import '@grigoriev/react-sbb-polarion/style.css';
import App from './App';
import './App.css';

// The shared UI wrappers (SearchableSelect) load the generic ES modules from this extension's own
// Polarion webapp context at runtime. The dev proxy forwards this absolute path to Polarion, and in
// Polarion it is same-origin.
configureGenericModules('/polarion/pdf-exporter-app/ui/generic/js/modules/');

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);

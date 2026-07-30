import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';
import tseslint from 'typescript-eslint';

// Mirrors react-sbb-polarion's eslint.config.js so the React apps lint identically. src/vendor holds
// the vendored petrel code editor + highlight.js + jsonlint (kept verbatim from the legacy webapp);
// it is third-party and not restyled here, so it is ignored (like RSP's src/generic).
export default tseslint.config(
  {
    ignores: [
      'dist',
      'node',
      'node_modules',
      'coverage',
      '.vite',
      'src/vendor',
      'test/expected',
      'test/__diff__',
      'test/__screenshots__',
      '.vitest-attachments',
    ],
  },
  // TypeScript + React sources.
  {
    files: ['**/*.{ts,tsx}'],
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    plugins: { 'react-hooks': reactHooks },
    languageOptions: {
      ecmaVersion: 2022,
      globals: { ...globals.browser, ...globals.node },
    },
    rules: {
      // eslint-plugin-react-hooks recommended set (declared explicitly - the plugin's shipped flat
      // config uses a legacy string-array `plugins` key that ESLint 10 rejects when spread directly).
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
    },
  },
  // Plain JS/ESM (this config, vite.config.js, the docker-test wrapper).
  {
    files: ['**/*.{js,mjs}'],
    extends: [js.configs.recommended],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: { ...globals.node },
    },
  },
  // Keep last: turns off ESLint rules that would conflict with Prettier's formatting.
  prettier,
);

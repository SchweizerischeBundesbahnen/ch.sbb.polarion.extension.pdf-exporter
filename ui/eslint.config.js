import js from '@eslint/js';
import { polarionEslintConfig } from '@sbb-polarion/react-sbb-polarion/eslint-config';
import globals from 'globals';

// The shared setup of the SBB Polarion React apps (TypeScript, React hooks, jsx-a11y, Prettier), plus the
// plain JS tooling. src/vendor holds the vendored petrel code editor + highlight.js + jsonlint (kept verbatim
// from the legacy webapp); it is third-party and not restyled here, so it is ignored (like RSP's src/generic).
export default polarionEslintConfig({
  ignores: ['node', '.vite', 'src/vendor', 'test/expected', 'test/__diff__', 'test/__screenshots__', '.vitest'],
  configs: [
    // Plain JS/ESM (this config, vite.config.js, the node scripts under scripts/).
    {
      files: ['**/*.{js,mjs}'],
      extends: [js.configs.recommended],
      languageOptions: {
        ecmaVersion: 2022,
        sourceType: 'module',
        globals: { ...globals.node },
      },
    },
  ],
});

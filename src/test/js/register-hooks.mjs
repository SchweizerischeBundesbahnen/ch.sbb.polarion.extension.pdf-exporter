// Registers path-redirector's module customization hooks before anything else is loaded.
//
// This is the supported replacement for `--experimental-loader=./path-redirector.mjs`, which Node warns
// about on every process it is inherited by and has announced it may remove. `register()` does the same
// thing through the stable API, and the test script passes this file to Node's `--import`, so the hooks
// are in place before mocha resolves a single spec file.
//
// The parent URL is this file's own, so `./path-redirector.mjs` resolves next to it rather than against
// whatever the working directory happens to be.
import { register } from 'node:module';

register('./path-redirector.mjs', import.meta.url);

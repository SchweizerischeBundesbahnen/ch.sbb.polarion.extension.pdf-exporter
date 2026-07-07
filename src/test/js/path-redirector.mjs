// Generic modules aren't present in this project's source tree; they are built into
// target/generic-jar-content. This loader redirects any import of a generic asset — whether written
// as an absolute `/polarion/<ext>/ui/generic/js/...` path or a relative `../ui/generic/js/...` one —
// to that folder. See the scripts/test entry in package.json.

import { fileURLToPath, pathToFileURL } from 'url';
import { resolve as pathResolve, dirname } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const GENERIC_SUFFIX = /(?:^|\/)(?:ui\/)?generic\/(js\/.+\.js)$/;

export async function resolve(specifier, context, nextResolve) {
    const match = GENERIC_SUFFIX.exec(specifier);
    if (match) {
        const target = pathResolve(__dirname, '../../../target/generic-jar-content/' + match[1]);
        return {
            url: pathToFileURL(target).href,
            shortCircuit: true
        };
    }
    return nextResolve(specifier, context);
}

export async function load(url, context, nextLoad) {
    return nextLoad(url, context);
}

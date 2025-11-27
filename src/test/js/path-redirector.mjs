// This path redirector is used to replace paths in imports of JS modules from Generic project which is not available for tests
// by paths to these modules in build target, in its generic-jar-content folder. See scripts/test item in package.json located in project's root

import { fileURLToPath, pathToFileURL } from 'url';
import { resolve as pathResolve, dirname } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const pathMappings = {
    '/polarion/pdf-exporter/ui/generic/js/modules/ConfigurationsPane.js': pathResolve(__dirname, '../../../target/generic-jar-content/js/modules/ConfigurationsPane.js'),
    '/polarion/pdf-exporter/ui/generic/js/modules/CustomSelect.js': pathResolve(__dirname, '../../../target/generic-jar-content/js/modules/CustomSelect.js'),
    '/polarion/pdf-exporter/ui/generic/js/modules/ExtensionContext.js': pathResolve(__dirname, '../../../target/generic-jar-content/js/modules/ExtensionContext.js')
};

export async function resolve(specifier, context, nextResolve) {
    if (pathMappings[specifier]) {
        return {
            url: pathToFileURL(pathMappings[specifier]).href,
            shortCircuit: true
        };
    }
    return nextResolve(specifier, context);
}

export async function load(url, context, nextLoad) {
    return nextLoad(url, context);
}

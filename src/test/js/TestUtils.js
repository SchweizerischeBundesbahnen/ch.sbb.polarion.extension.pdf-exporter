import * as td from 'testdouble';

const GENERIC_JS_MODULES_IMPORT_PATH_PREFIX = '/polarion/pdf-exporter/ui/generic/js/modules';
const GENERIC_UNPACKED_JS_MODULES_ROOT_FOLDER = '../../../target/generic-jar-content/js/modules';
const SRC_JS_MODULES_IMPORT_PATH_PREFIX = '../../main/resources/webapp/pdf-exporter/js/modules';

const TestUtils = {
    importUsingGeneric: async function (moduleFileName) {
        const modulesToReplace = ['ConfigurationsPane.js', 'CustomSelect.js', 'ExtensionContext.js'];
        await Promise.all(modulesToReplace.map(module => this.replaceGenericModule(module)));
        return (await import(`${SRC_JS_MODULES_IMPORT_PATH_PREFIX}/${moduleFileName}`)).default;
    },

    replaceGenericModule: async function (moduleFileName) {
        return td.replaceEsm(`${GENERIC_JS_MODULES_IMPORT_PATH_PREFIX}/${moduleFileName}`,
            {default: (await import(`${GENERIC_UNPACKED_JS_MODULES_ROOT_FOLDER}/${moduleFileName}`)).default});
    }
}

export default TestUtils;

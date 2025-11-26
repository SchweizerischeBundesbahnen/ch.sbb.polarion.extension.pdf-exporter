const SRC_JS_MODULES_IMPORT_PATH = '../../main/resources/webapp/pdf-exporter/js/modules';

const TestUtils = {
    loadModule: async function (moduleFileName) {
        return (await import(`${SRC_JS_MODULES_IMPORT_PATH}/${moduleFileName}`)).default;
    }
}

export default TestUtils;

package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.RichPageParameterEnum;
import com.polarion.alm.shared.api.model.rp.parameter.impl.RichPageParameterInternal;
import com.polarion.alm.shared.api.model.rp.parameter.impl.persist.RichPageParameterPersistor;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class DataSetQueryTypeReaderTest {

    @Test
    void luceneQueryTypeIsNotSql() {
        assertFalse(DataSetQueryTypeReader.isSqlQuery(parameterPersisting("lucene")));
        assertFalse(DataSetQueryTypeReader.isSqlQuery(parameterPersisting("luceneVelocity")));
    }

    @Test
    void sqlQueryTypesAreRecognized() {
        assertTrue(DataSetQueryTypeReader.isSqlQuery(parameterPersisting("sql")));
        assertTrue(DataSetQueryTypeReader.isSqlQuery(parameterPersisting("sqlVelocity")));
    }

    @Test
    void theOuterQueryTypeWins() {
        // A nested parameter persists its own values afterwards, the data set's own query type comes first
        DataSetParameter parameter = mock(DataSetParameter.class, withSettings().extraInterfaces(RichPageParameterInternal.class));
        doAnswer(invocation -> {
            RichPageParameterPersistor persistor = invocation.getArgument(0);
            persistor.childParameters().add("queryType").string("sql");
            persistor.childParameters().add("children").childParameters().add("queryType").string("lucene");
            return null;
        }).when((RichPageParameterInternal) parameter).persist(any());

        assertTrue(DataSetQueryTypeReader.isSqlQuery(parameter));
    }

    @Test
    void everyPersistedValueTypeIsAccepted() {
        DataSetParameter parameter = mock(DataSetParameter.class, withSettings().extraInterfaces(RichPageParameterInternal.class));
        doAnswer(invocation -> {
            RichPageParameterPersistor persistor = invocation.getArgument(0);
            // What a data set persists around its query type: nulls, other value types and list children must not
            // disturb the reader
            persistor.childParameters().add("top").integer(50);
            persistor.childParameters().add("top").integer(null);
            persistor.childParameters().add("enabled").bool(true);
            persistor.childParameters().add("empty").bool(null);
            persistor.childParameters().add("date").dateOnly(null);
            persistor.childParameters().add("since").dateOnly(new Date(0));
            // The three-argument form, which a page parameter is persisted through
            persistor.childParameters().add("pageParam", "value", RichPageParameterEnum.String).string("x");
            // Straight on the root persistor, which has no name to record the value under
            persistor.string("unnamed");
            persistor.childParameters().add("scope").inherited("project/elibrary/");
            persistor.childParameters().add("sortBy").list().add().string("id");
            persistor.childParameters().add("luceneQuery").formattedString("id:test");
            persistor.childParameters().add("queryType").string("sql");
            return null;
        }).when((RichPageParameterInternal) parameter).persist(any());

        assertTrue(DataSetQueryTypeReader.isSqlQuery(parameter));
    }

    @Test
    void anUnknownParameterIsTreatedAsLucene() {
        assertFalse(DataSetQueryTypeReader.isSqlQuery(mock(DataSetParameter.class)));
        assertFalse(DataSetQueryTypeReader.isSqlQuery(parameterPersisting(null)));
    }

    @Test
    void aFailingParameterIsTreatedAsLucene() {
        DataSetParameter parameter = mock(DataSetParameter.class, withSettings().extraInterfaces(RichPageParameterInternal.class));
        doThrow(new IllegalStateException("no persistence here")).when((RichPageParameterInternal) parameter).persist(any());

        assertFalse(DataSetQueryTypeReader.isSqlQuery(parameter));
    }

    @Test
    void queryTypeMatchingIsCaseInsensitive() {
        assertTrue(DataSetQueryTypeReader.isSqlQueryType("SQL"));
        assertFalse(DataSetQueryTypeReader.isSqlQueryType(null));
        assertFalse(DataSetQueryTypeReader.isSqlQueryType(""));
        // "sql" anywhere but at the start is a field name, not a query type
        assertFalse(DataSetQueryTypeReader.isSqlQueryType("luceneSql"));
    }

    private static DataSetParameter parameterPersisting(String queryType) {
        DataSetParameter parameter = mock(DataSetParameter.class, withSettings().extraInterfaces(RichPageParameterInternal.class));
        doAnswer(invocation -> {
            RichPageParameterPersistor persistor = invocation.getArgument(0);
            persistor.childParameters().add("queryType").string(queryType);
            return null;
        }).when((RichPageParameterInternal) parameter).persist(any());
        return parameter;
    }
}

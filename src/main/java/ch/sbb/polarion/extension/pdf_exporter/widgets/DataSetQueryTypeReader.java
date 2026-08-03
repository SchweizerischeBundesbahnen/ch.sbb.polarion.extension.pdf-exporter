package ch.sbb.polarion.extension.pdf_exporter.widgets;

import com.polarion.alm.shared.api.model.rp.parameter.DataSetParameter;
import com.polarion.alm.shared.api.model.rp.parameter.impl.RichPageParameterEnum;
import com.polarion.alm.shared.api.model.rp.parameter.impl.RichPageParameterInternal;
import com.polarion.alm.shared.api.model.rp.parameter.impl.persist.RichPageParameterListPersistor;
import com.polarion.alm.shared.api.model.rp.parameter.impl.persist.RichPageParameterPersistor;
import com.polarion.alm.shared.api.model.rp.parameter.impl.persist.RichPageParametersPersistor;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tells whether a data set parameter queries with SQL or with Lucene.
 * <p>
 * The two need different search APIs when the query runs again in the widget's REST endpoint, but the public
 * parameter API exposes no query type. What it does expose is persistence: a parameter writes its own values into a
 * persistor, query type included. This asks a parameter to persist itself into a persistor that only remembers what
 * it was told, which is the same mechanism Polarion uses to store the parameter on the page.
 * <p>
 * A data set in a collection widget context is always SQL, a plain one is Lucene unless the page author chose
 * otherwise, so Lucene is the answer whenever the query type cannot be read.
 */
// java:S2143 - java.util.Date is the parameter type of Polarion's persistor interface, which this implements
@SuppressWarnings("java:S2143")
public final class DataSetQueryTypeReader {

    private static final String KEY_QUERY_TYPE = "queryType";
    private static final Logger logger = Logger.getLogger(DataSetQueryTypeReader.class);

    private DataSetQueryTypeReader() {
    }

    public static boolean isSqlQuery(@NotNull DataSetParameter dataSetParameter) {
        if (!(dataSetParameter instanceof RichPageParameterInternal internalParameter)) {
            return false;
        }
        Map<String, String> values = new HashMap<>();
        try {
            internalParameter.persist(new CapturingPersistor(values, null));
        } catch (RuntimeException e) {
            logger.warn("Could not read the query type of a bulk export widget, assuming Lucene", e);
            return false;
        }
        return isSqlQueryType(values.get(KEY_QUERY_TYPE));
    }

    /**
     * Matches both SQL query types Polarion offers, {@code sql} and {@code sqlVelocity}.
     */
    @VisibleForTesting
    static boolean isSqlQueryType(@Nullable String queryType) {
        return queryType != null && queryType.toLowerCase(Locale.ROOT).startsWith("sql");
    }

    /**
     * Records the values a parameter persists, keyed by the name it persists them under. A parameter tree is
     * persisted depth first and the outer data set writes its own query type before any nested parameter, so the
     * first value wins.
     */
    private record CapturingPersistor(@NotNull Map<String, String> values, @Nullable String key)
            implements RichPageParameterPersistor, RichPageParametersPersistor, RichPageParameterListPersistor {

        @Override
        public void inherited(@NotNull String value) {
            capture(value);
        }

        @Override
        public void string(@Nullable String value) {
            capture(value);
        }

        @Override
        public void formattedString(@Nullable String value) {
            capture(value);
        }

        @Override
        public void bool(@Nullable Boolean value) {
            capture(value == null ? null : value.toString());
        }

        @Override
        public void integer(@Nullable Integer value) {
            capture(value == null ? null : value.toString());
        }

        @Override
        public void dateOnly(@Nullable Date value) {
            capture(value == null ? null : value.toString());
        }

        @Override
        public @NotNull RichPageParametersPersistor childParameters() {
            return this;
        }

        @Override
        public @NotNull RichPageParameterListPersistor list() {
            return this;
        }

        @Override
        public RichPageParameterPersistor add(@NotNull String name) {
            return new CapturingPersistor(values, name);
        }

        @Override
        public @NotNull RichPageParameterPersistor add(@NotNull String name, @Nullable String value, @NotNull RichPageParameterEnum type) {
            return new CapturingPersistor(values, name);
        }

        @Override
        public @NotNull RichPageParameterPersistor add() {
            return this;
        }

        private void capture(@Nullable String value) {
            if (key != null && value != null) {
                values.putIfAbsent(key, value);
            }
        }
    }
}

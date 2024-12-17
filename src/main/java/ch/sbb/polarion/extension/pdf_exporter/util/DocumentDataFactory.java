package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.pdf_exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.DocumentData;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.ModelObjectProvider;
import ch.sbb.polarion.extension.pdf_exporter.rest.model.documents.UniqueObjectConverter;
import ch.sbb.polarion.extension.pdf_exporter.service.PolarionBaselineExecutor;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@UtilityClass
public class DocumentDataFactory {

    public @NotNull <T extends IUniqueObject> DocumentData<T> getDocumentData(@NotNull ExportParams exportParams, boolean withContent) {
        return Objects.requireNonNull(TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> PolarionBaselineExecutor.executeInBaseline(exportParams.getBaselineRevision(), transaction, () -> {

                    ModelObject modelObject = new ModelObjectProvider(exportParams)
                            .getModelObject(transaction);

                    return new UniqueObjectConverter(modelObject)
                            .withExportParams(exportParams)
                            .withContent(withContent)
                            .toDocumentData(transaction);
                })));
    }

}

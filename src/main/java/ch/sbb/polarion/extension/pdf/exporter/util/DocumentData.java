package ch.sbb.polarion.extension.pdf.exporter.util;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWikiPage;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DocumentData {
    private String projectName;
    private IModule document;
    private IWikiPage wikiPage;
    private IRichPage richPage;
    private ITestRun testRun;
    private String lastRevision;
    private String baselineName;
    private String documentId;
    private String documentTitle;
    private String documentContent;
}

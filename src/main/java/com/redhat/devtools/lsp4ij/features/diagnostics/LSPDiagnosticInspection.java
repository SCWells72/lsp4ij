package com.redhat.devtools.lsp4ij.features.diagnostics;

import com.intellij.codeInspection.*;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.redhat.devtools.lsp4ij.LSPIJUtils;
import com.redhat.devtools.lsp4ij.LanguageServiceAccessor;
import com.redhat.devtools.lsp4ij.internal.CancellationSupport;
import com.redhat.devtools.lsp4ij.internal.CompletableFutures;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.isDoneNormally;
import static com.redhat.devtools.lsp4ij.internal.CompletableFutures.waitUntilDone;

public class LSPDiagnosticInspection extends GlobalSimpleInspectionTool {

    @Override
    public void checkFile(@NotNull PsiFile file,
                          @NotNull InspectionManager manager,
                          @NotNull ProblemsHolder holder,
                          @NotNull GlobalInspectionContext context,
                          @NotNull ProblemDescriptionsProcessor processor) {
        Project project = file.getProject();
        VirtualFile virtualFile = file.getVirtualFile();

        CompletableFuture<List<DocumentDiagnosticReport>> diagnosticReportsFuture = LanguageServiceAccessor.getInstance(project).getLanguageServers(
                        virtualFile,
                        cf -> cf.getDiagnosticFeature().isEnabled(file),
                        cf -> cf.getDiagnosticFeature().isSupported(file)
                )
                .thenComposeAsync(languageServers -> {
                    if (languageServers.isEmpty()) {
                        return CompletableFuture.completedStage(Collections.emptyList());
                    }

                    TextDocumentIdentifier textDocumentIdentifier = LSPIJUtils.toTextDocumentIdentifier(virtualFile);
                    DocumentDiagnosticParams documentDiagnosticParams = new DocumentDiagnosticParams(textDocumentIdentifier);
                    List<CompletableFuture<DocumentDiagnosticReport>> diagnosticReportFutures = languageServers
                            .stream()
                            .map(ls -> ls.getTextDocumentService().diagnostic(documentDiagnosticParams))
                            .toList();
                    return CompletableFutures.mergeInOneFuture2(diagnosticReportFutures, new CancellationSupport());
                });

        try {
            waitUntilDone(diagnosticReportsFuture, file);
        } catch (ProcessCanceledException e) {
            return;
        } catch (CancellationException e) {
            return;
        } catch (ExecutionException e) {
            return;
        }
        if (!isDoneNormally(diagnosticReportsFuture)) {
            return;
        }

        List<DocumentDiagnosticReport> diagnosticReports = diagnosticReportsFuture.getNow(Collections.emptyList());
        if (!ContainerUtil.isEmpty(diagnosticReports)) {
            for (DocumentDiagnosticReport diagnosticReport : diagnosticReports) {
                RelatedFullDocumentDiagnosticReport fullDiagnosticReport = diagnosticReport.getRelatedFullDocumentDiagnosticReport();
                if (fullDiagnosticReport != null) {
                    Map<String, Either<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>> relatedDocuments = fullDiagnosticReport.getRelatedDocuments();
                    if (relatedDocuments != null) {
                        for (Map.Entry<String, Either<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>> entry : relatedDocuments.entrySet()) {
                            String key = entry.getKey();
                            Either<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport> either = entry.getValue();
                        }
                    }
                }

                RelatedUnchangedDocumentDiagnosticReport unchangedDiagnosticReport = diagnosticReport.getRelatedUnchangedDocumentDiagnosticReport();
                if (unchangedDiagnosticReport != null) {
                    Map<String, Either<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>> relatedDocuments = unchangedDiagnosticReport.getRelatedDocuments();
                    if (relatedDocuments != null) {
                        for (Map.Entry<String, Either<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>> entry : relatedDocuments.entrySet()) {
                            String key = entry.getKey();
                            Either<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport> either = entry.getValue();
                        }
                    }
                }
            }
        }
    }
}

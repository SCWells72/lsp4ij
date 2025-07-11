/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.lsp4ij.installation;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.redhat.devtools.lsp4ij.LanguageServerBundle;
import com.redhat.devtools.lsp4ij.internal.CancellationSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract class for handling server installation tasks.
 * <p>
 * This class manages the server installation process, including checking the installation status,
 * starting the installation process, and updating the installation state.
 * It is responsible for tracking the progress of the installation and interacting with the
 * {@link ProgressIndicator} to show status updates during installation.
 * <p>
 * Subclasses should implement the {@link #checkServerInstalled(ProgressIndicator)} and {@link #install(ProgressIndicator)}
 * methods to provide custom logic for checking if the server is installed and performing the installation.
 */
public abstract class ServerInstallerBase implements ServerInstaller {

    protected static final CompletableFuture<ServerInstallationStatus> INSTALLED_FUTURE = CompletableFuture.completedFuture(ServerInstallationStatus.INSTALLED);

    /**
     * A {@link CompletableFuture} representing the installation process.
     * This future tracks the state of the installation and will be completed once installation is finished.
     */
    @Nullable
    private CompletableFuture<ServerInstallationStatus> installFuture;

    /**
     * The current status of the server installation.
     * Defaults to {@link ServerInstallationStatus#NOT_INSTALLED}.
     * This field is used to track whether the server is installed, in the process of installation,
     * or not installed at all.
     */
    @NotNull
    private ServerInstallationStatus status;

    private final List<ConsoleProvider> consoleProviders;

    public ServerInstallerBase() {
        reset();
        this.consoleProviders = new CopyOnWriteArrayList<>();
    }

    /**
     * Checks the current installation status and starts the installation if necessary.
     * <p>
     * If the server is already installed, it returns a pre-completed future.
     * If not, it creates a new installation task and returns a future representing the installation process.
     *
     * @return a {@link CompletableFuture} that will be completed once the installation is finished.
     */
    @Override
    public @NotNull CompletableFuture<ServerInstallationStatus> checkInstallation() {
        return checkInstallation(new ServerInstallationContext());
    }

    /**
     * Checks the current installation status and starts the installation if necessary.
     * <p>
     * If the server is already installed, it returns a pre-completed future.
     * If not, it creates a new installation task and returns a future representing the installation process.
     *
     * @return a {@link CompletableFuture} that will be completed once the installation is finished.
     */
    @Override
    public @NotNull CompletableFuture<ServerInstallationStatus> checkInstallation(@NotNull ServerInstallationContext context) {
        if (status == ServerInstallationStatus.INSTALLED) {
            // Server is already installed, return completed future
            return INSTALLED_FUTURE;
        }
        if (installFuture == null || !isInstallFutureInitialized()) {
            installFuture = createInstallFuture(context);
        }
        return installFuture;
    }

    @Override
    public @NotNull ServerInstallationStatus getStatus() {
        return status;
    }

    /**
     * Determines if the installation future is initialized and has not been cancelled or completed exceptionally.
     *
     * @return true if the installation future is initialized and valid, false otherwise.
     */
    protected boolean isInstallFutureInitialized() {
        return installFuture != null && (!installFuture.isCompletedExceptionally() || !installFuture.isCancelled());
    }

    /**
     * Creates a new install future and starts the installation process asynchronously.
     * This method runs the installation task in the background and tracks progress.
     *
     * @return a {@link CompletableFuture} representing the installation process.
     */
    @NotNull
    private synchronized CompletableFuture<ServerInstallationStatus> createInstallFuture(@NotNull ServerInstallationContext context) {
        if (installFuture != null && isInstallFutureInitialized()) {
            return installFuture;
        }
        CompletableFuture<ServerInstallationStatus> installFuture = new CompletableFuture<>();
        ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), getInstallationTaskTitle(), canBeCancelled()) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator = new PrintableProgressIndicator(indicator) {
                        @Override
                        protected void printProgress(@NotNull String progressMessage) {
                            getConsoleProviders()
                                    .forEach(provider -> provider.printProgress(progressMessage));
                        }
                    };
                    indicator.setIndeterminate(false);
                    if (status == ServerInstallationStatus.NOT_INSTALLED) {
                        progressCheckingServerInstalled(indicator);
                        updateStatus(ServerInstallationStatus.CHECKING_INSTALLED);
                    }
                    // Check if user has canceled the server installer task
                    ProgressManager.checkCanceled();

                    if (!context.isForceInstall()) {
                        // Checking if the server is installed
                        if (checkServerInstalled(indicator)) {
                            markAsInstalled(context, installFuture);
                            return;
                        }
                    }

                    // Check if user has canceled the server installer task
                    ProgressManager.checkCanceled();

                    // Installing the server
                    progressInstallingServer(indicator);
                    updateStatus(ServerInstallationStatus.INSTALLING);

                    var beforeCode = getBeforeCode(context);
                    if (beforeCode != null) {
                        beforeCode.run();
                    }
                    // Check if user has canceled the server installer task
                    ProgressManager.checkCanceled();

                    // Process the installation
                    install(indicator);

                    markAsInstalled(context, installFuture);
                } catch (ProcessCanceledException e) {
                    //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
                    //TODO delete block when minimum required version is 2024.2
                    CancellationSupport.cancel(installFuture);
                } catch (CancellationException e) {
                    CancellationSupport.cancel(installFuture);
                } catch (Throwable e) {
                    updateStatus(ServerInstallationStatus.NOT_INSTALLED);
                    installFuture.completeExceptionally(e);
                }
            }
        });
        return installFuture;
    }

    protected void updateStatus(@NotNull ServerInstallationStatus status) {
        this.status = status;
    }

    private void markAsInstalled(@NotNull ServerInstallationContext context,
                                 @NotNull CompletableFuture<ServerInstallationStatus> installFuture) {
        updateStatus(ServerInstallationStatus.INSTALLED);
        var afterCode = getAfterCode(context);
        if (afterCode != null) {
            afterCode.run();
        }
        installFuture.complete(status);
    }

    /**
     * Displays progress while checking if the server is installed.
     *
     * @param indicator the progress indicator to update with progress.
     */
    protected void progressCheckingServerInstalled(@NotNull ProgressIndicator indicator) {
        progress(LanguageServerBundle.message("server.installer.progress.check.installed"), 0.1d, indicator);
    }

    /**
     * Displays progress during the installation process.
     *
     * @param indicator the progress indicator to update with progress.
     */
    protected void progressInstallingServer(@NotNull ProgressIndicator indicator) {
        progress(LanguageServerBundle.message("server.installer.progress.installing"), 0.2d, indicator);
    }

    /**
     * Updates the progress indicator with a message and fraction value.
     *
     * @param text      the message to display.
     * @param fraction  the progress fraction (0.0 to 1.0).
     * @param indicator the progress indicator to update.
     */
    protected void progress(@NotNull String text,
                            double fraction,
                            @NotNull ProgressIndicator indicator) {
        progress(text, indicator);
        indicator.setFraction(fraction);
    }

    /**
     * Updates the progress indicator with a message.
     *
     * @param text      the message to display.
     * @param indicator the progress indicator to update.
     */
    protected void progress(@NotNull String text,
                            @NotNull ProgressIndicator indicator) {
        indicator.setText(text);
    }

    /**
     * Resets the installation status to {@link ServerInstallationStatus#NOT_INSTALLED}.
     * This method can be used to re-initiate the installation process if needed.
     */
    @Override
    public void reset() {
        status = ServerInstallationStatus.NOT_INSTALLED;
        installFuture = null;
    }

    /**
     * Gets the title of the installation task.
     *
     * @return a string representing the title for the installation task.
     */
    protected String getInstallationTaskTitle() {
        return LanguageServerBundle.message("server.installer.task.installing");
    }

    /**
     * Determines whether the installation process can be cancelled.
     *
     * @return true if the installation can be cancelled, false otherwise.
     */
    protected boolean canBeCancelled() {
        return true;
    }

    /**
     * Checks if the server is installed.
     * <p>
     * Subclasses should implement this method to provide the logic for checking if the server
     * is already installed.
     *
     * @param indicator the progress indicator to update during the check.
     * @return true if the server is installed, false otherwise.
     */
    protected abstract boolean checkServerInstalled(@NotNull ProgressIndicator indicator) throws Exception;

    /**
     * Performs the actual installation of the server.
     * <p>
     * Subclasses should implement this method to define the installation process.
     *
     * @param indicator the progress indicator to update during the installation.
     * @throws Exception if an error occurs during installation.
     */
    protected abstract void install(@NotNull ProgressIndicator indicator) throws Exception;

    /**
     * Gets the project associated with this server installation process.
     *
     * @return the {@link Project} for the installation.
     */
    protected abstract @Nullable Project getProject();

    @Override
    public void registerConsoleProvider(@NotNull ConsoleProvider consoleProvider) {
        this.consoleProviders.add(consoleProvider);
        // Unregister the console provider when the console is disposed.
        Disposer.register(consoleProvider.getConsole(), () -> unregisterConsoleProvider(consoleProvider));
    }

    @Override
    public void unregisterConsoleProvider(@NotNull ConsoleProvider consoleProvider) {
        this.consoleProviders.remove(consoleProvider);
    }

    public @NotNull List<ConsoleProvider> getConsoleProviders() {
        return consoleProviders;
    }
}

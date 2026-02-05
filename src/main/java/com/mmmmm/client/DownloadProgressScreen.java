package com.mmmmm.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DownloadProgressScreen extends Screen {

    private volatile String downloadSource;
    private volatile String downloadLabel;
    private volatile int progress = 0;
    private volatile String downloadSpeed = "0 KB/s";
    private volatile String estimatedTimeRemaining = "";
    private Button cancelButton;
    private volatile boolean isProcessing = false;
    private volatile String processingTitle = "";
    private volatile String processingDetail = "";
    private volatile int processingProgress = 0;
    private volatile boolean processingHasProgress = false;
    private final Screen returnScreen;
    private volatile boolean showSummary = false;
    private volatile String summaryTitle = "Update complete";
    private volatile List<String> summaryLines = Collections.emptyList();
    private volatile boolean isCancelled = false;

    public DownloadProgressScreen(String downloadLabel, String downloadSource, Screen returnScreen) {
        super(Component.literal("Downloading Update"));
        this.downloadLabel = downloadLabel == null || downloadLabel.isBlank() ? "files" : downloadLabel;
        this.downloadSource = downloadSource == null || downloadSource.isBlank() ? "server" : downloadSource;
        this.returnScreen = returnScreen;
    }
    @Override
    protected void init() {
        super.init();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY = (this.height / 2) + 50;

        cancelButton = Button.builder(Component.literal("Cancel"), (button) -> {
            if (!showSummary) {
                isCancelled = true; // Signal cancellation
            }
            minecraft.execute(() -> minecraft.setScreen(returnScreen)); // Return to the previous screen
        }).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();

        this.addRenderableWidget(cancelButton);
    }

    /**
     * Checks if the download has been cancelled.
     */
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Resets the UI for a new download.
     */
    public void startNewDownload(String downloadLabel, String downloadSource) {
        this.downloadLabel = downloadLabel == null || downloadLabel.isBlank() ? "files" : downloadLabel;
        this.downloadSource = downloadSource == null || downloadSource.isBlank() ? "server" : downloadSource;
        this.progress = 0;
        this.downloadSpeed = "0 KB/s";
        this.estimatedTimeRemaining = "";
        this.isProcessing = false;
        this.processingTitle = "";
        this.processingDetail = "";
        this.processingProgress = 0;
        this.processingHasProgress = false;
        this.showSummary = false;
        this.summaryTitle = "Update complete";
        this.summaryLines = Collections.emptyList();
        if (cancelButton != null) {
            cancelButton.setMessage(Component.literal("Cancel"));
        }
    }

    /**
     * Updates the progress bar, download speed, and estimated time remaining.
     *
     * @param progress      The current progress (0-100).
     * @param downloadSpeed The current download speed in KB/s.
     * @param estimatedTimeRemaining Estimated time remaining (optional).
     */
    public void updateProgress(int progress, String downloadSpeed, String estimatedTimeRemaining) {
        this.progress = Math.min(100, Math.max(0, progress));
        this.downloadSpeed = downloadSpeed;
        this.estimatedTimeRemaining = estimatedTimeRemaining;
    }

    /**
     * Call this when extraction starts after download finishes.
     * Shows extraction info including last download speed.
     */
    public void startExtraction(String extractionMessage) {
        startProcessing(extractionMessage, "Last download speed: " + downloadSpeed);
    }

    public void startProcessing(String title, String detail) {
        updateProcessing(title, detail, 0, false);
    }

    public void updateProcessing(String title, String detail, int progress, boolean hasProgress) {
        this.isProcessing = true;
        this.processingTitle = title == null ? "" : title;
        this.processingDetail = detail == null ? "" : detail;
        this.processingProgress = Math.min(100, Math.max(0, progress));
        this.processingHasProgress = hasProgress;
    }

    public void showSummary(String title, List<String> lines) {
        this.showSummary = true;
        this.summaryTitle = title == null || title.isBlank() ? "Update complete" : title;
        if (lines == null || lines.isEmpty()) {
            this.summaryLines = Collections.emptyList();
        } else {
            this.summaryLines = Collections.unmodifiableList(new ArrayList<>(lines));
        }
        this.isProcessing = false;
        this.processingTitle = "";
        this.processingDetail = "";
        this.progress = 100;
        if (cancelButton != null) {
            cancelButton.setMessage(Component.literal("Close"));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (showSummary) {
            renderSummary(guiGraphics);
            return;
        }

        if (isProcessing) {
            renderProcessing(guiGraphics);
            return;
        }

        renderDownloadProgress(guiGraphics);
    }

    private void renderDownloadProgress(GuiGraphics guiGraphics) {
        guiGraphics.drawCenteredString(this.font, "Downloading " + downloadLabel + " from " + downloadSource, this.width / 2, 20, 0xFFFFFF);

        int barWidth = 200;
        int barHeight = 20;
        int barX = (this.width - barWidth) / 2;
        int barY = this.height / 2;

        guiGraphics.drawCenteredString(this.font, downloadSpeed, this.width / 2, barY - 30, 0xFFFFFF);
        if (!estimatedTimeRemaining.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "ETA: " + estimatedTimeRemaining, this.width / 2, barY - 55, 0xFFFFFF);
        }

        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFFAAAAAA);

        int progressWidth = (int) (barWidth * (progress / 100.0));
        guiGraphics.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF00FF00);

        guiGraphics.drawCenteredString(this.font, progress + "%", this.width / 2, barY + 5, 0xFFFFFF);
    }

    private void renderProcessing(GuiGraphics guiGraphics) {
        String title = processingTitle == null || processingTitle.isBlank()
                ? "Processing update..."
                : processingTitle;
        guiGraphics.drawCenteredString(this.font, title, this.width / 2, 20, 0xFFFFFF);

        int barWidth = 200;
        int barHeight = 20;
        int barX = (this.width - barWidth) / 2;
        int barY = this.height / 2;

        if (processingDetail != null && !processingDetail.isBlank()) {
            guiGraphics.drawCenteredString(this.font, processingDetail, this.width / 2, barY - 30, 0xFFFFFF);
        }

        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFFAAAAAA);

        int progressWidth = processingHasProgress
                ? (int) (barWidth * (processingProgress / 100.0))
                : 0;
        guiGraphics.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFF00FF00);

        String progressLabel = processingHasProgress ? (processingProgress + "%") : "...";
        guiGraphics.drawCenteredString(this.font, progressLabel, this.width / 2, barY + 5, 0xFFFFFF);
    }

    private void renderSummary(GuiGraphics guiGraphics) {
        guiGraphics.drawCenteredString(this.font, summaryTitle, this.width / 2, 20, 0xFFFFFF);

        int maxWidth = Math.max(200, this.width - 40);
        int y = (this.height / 2) - 40;

        if (summaryLines.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "No details available.", this.width / 2, y, 0xFFFFFF);
            return;
        }

        for (String line : summaryLines) {
            for (var wrapped : this.font.split(Component.literal(line), maxWidth)) {
                guiGraphics.drawCenteredString(this.font, wrapped, this.width / 2, y, 0xFFFFFF);
                y += 12;
            }
            y += 4;
        }
    }
}

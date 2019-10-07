package io.proximax.app.controller;

import com.qoppa.pdfViewerFX.PDFViewer;
import io.proximax.app.core.ui.IApp;
import io.proximax.download.DownloadParameter;
import io.proximax.download.DownloadResult;
import io.proximax.download.Downloader;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.LocalFileHelpers;
import io.proximax.app.utils.MimeTypes;
import io.proximax.app.utils.StringUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author thcao
 */
public class FileViewDialog extends AbstractController {

    @FXML
    WebView webView;
    @FXML
    MediaView mediaView;
    private LocalAccount localAccount;
    private LocalFile localFile;
    private boolean isMediaViewer = false;

    public FileViewDialog(LocalAccount account, LocalFile localFile) {
        super(true);
        this.localAccount = account;
        this.localFile = localFile;
    }

    @Override
    protected void initialize() {
        String mimeType = MimeTypes.getMimeType(localFile.fileName);
        if (mimeType.contains("video") || mimeType.contains("audio")) {
            if (!localFile.isSecure()) {
                String fileURL = localAccount.getNetworkConfiguration().getDownloadUrl() + localFile.hash; //.toExternalForm();
                playVideo(fileURL);
            } else {
                reloadContent();
            }
        } else {
            reloadContent();
        }
    }

    private void playVideo(String url) {
        try {
            Media media = new Media(url);
            MediaPlayer player = new MediaPlayer(media);
            player.setOnEndOfMedia(() -> playBtn.setVisible(true));
            mediaView.setMediaPlayer(player);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @FXML
    private Button playBtn;

    @FXML
    public void playAndHide(ActionEvent event) {
        playBtn.setVisible(false);
        mediaView.getMediaPlayer().seek(Duration.ZERO);
        mediaView.getMediaPlayer().play();
    }

    @Override
    protected void dispose() {

    }

    @Override
    public String getTitle() {
        return CONST.FILEVIEWER_TITLE + localFile.fileName;
    }

    @Override
    public String getFXML() {
        String mimeType = MimeTypes.getMimeType(localFile.fileName);
        String resource = CONST.FILEVIEWER_FXML;
        isMediaViewer = false;
        if (mimeType.contains("video") || mimeType.contains("audio")) {
            resource = CONST.MEDIAVIEWER_FXML;
            isMediaViewer = true;
        }
        return resource;
    }

    public boolean isMediaViewer() {
        return isMediaViewer;
    }

    AtomicInteger taskExecution = new AtomicInteger(0);

    private void reloadContent() {
        if (localFile != null) {
            Alert alert = new Alert(
                    Alert.AlertType.INFORMATION,
                    "Operation in progress",
                    ButtonType.CANCEL
            );
            alert.setTitle("Load File");
            alert.setHeaderText("Please wait... ");
            ProgressIndicator progressIndicator = new ProgressIndicator();
            alert.setGraphic(progressIndicator);

            Task<Void> task = new Task<Void>() {
                final int N_ITERATIONS = 999;

                {
                    setOnFailed(a -> {
                        alert.close();
                        updateMessage("Failed");
                        close();
                        showParent();
                    });
                    setOnSucceeded(a -> {
                        alert.close();
                        updateMessage("Succeeded");
                    });
                    setOnCancelled(a -> {
                        alert.close();
                        updateMessage("Cancelled");
                        close();
                        showParent();
                    });
                }

                @Override
                protected Void call() throws Exception {
                    updateMessage("Processing");
                    int progress = 100;
                    updateProgress(progress, N_ITERATIONS);
                    File fileCache = LocalFileHelpers.getSourceFile(localAccount, localFile);
                    if (fileCache == null) {
                        try {
                            fileCache = LocalFileHelpers.createFileCache(localAccount, localFile);
                            //need download
                            DownloadParameter parameter = LocalFileHelpers.createDownloadParameter(localFile);
                            Downloader download = new Downloader(localAccount.connectionConfig);
                            DownloadResult downloadResult = download.download(parameter);
                            InputStream byteStream = downloadResult.getData().getByteStream();
                            FileOutputStream fouts = new FileOutputStream(fileCache);
                            byte[] buffer = new byte[1024];
                            int read = 0;
                            long sum = 0;
                            while ((read = byteStream.read(buffer)) >= 0) {
                                fouts.write(buffer, 0, read);
                                sum += read;
                                updateProgress(progress + 800 * sum / localFile.fileSize, 999);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            failed();
                        }
                    }
                    String mimeType = MimeTypes.getMimeType(localFile.fileName);
                    if (mimeType.contains("video") || mimeType.contains("audio")) {
                        if (localFile.isSecure()) {
                            String fileURL = fileCache.toURI().toURL().toString(); //.toExternalForm();                            
                            updateProgress(N_ITERATIONS, N_ITERATIONS);
                            playVideo(fileURL);

                        }
                    } else if (localFile.fileName.endsWith(".pdf")) {
                        try {
                            updateProgress(N_ITERATIONS, N_ITERATIONS);
                            PDFViewer pdfViewer = new PDFViewer();
                            pdfViewer.getToolBar().getOpenDoc().setVisible(false);
                            pdfViewer.setSplitVisible(false);
                            pdfViewer.loadPDF(fileCache.getAbsolutePath());
                            ((BorderPane) mainPane).setCenter(pdfViewer);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            failed();
                        }
                    } else {
                        String fileURL = fileCache.toURI().toURL().toExternalForm();
                        System.out.println("Open URL: " + fileURL);
                        updateProgress(N_ITERATIONS, N_ITERATIONS);
                        IApp.runSafe(() -> {                            
                            WebEngine webEngine = webView.getEngine();                            
                            webEngine.load(fileURL);
                        });
                    }

                    if (!isCancelled()) {
                        updateProgress(0, N_ITERATIONS);
                    }

                    return null;
                }
            };
            progressIndicator.progressProperty()
                    .bind(task.progressProperty());
            Thread taskThread = new Thread(
                    task,
                    "view-thread-" + taskExecution.getAndIncrement()
            );

            taskThread.start();

            alert.initOwner(getStage());
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent()
                    && result.get() == ButtonType.CANCEL && task.isRunning()) {
                task.cancel();
            }
        }
    }
}

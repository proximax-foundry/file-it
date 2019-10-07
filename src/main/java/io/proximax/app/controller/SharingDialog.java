package io.proximax.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import io.proximax.async.AsyncCallbacks;
import io.proximax.async.AsyncTask;
import io.proximax.download.DownloadParameter;
import io.proximax.download.DownloadResult;
import io.proximax.download.Downloader;
import io.proximax.app.core.ui.IApp;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.db.ShareFile;
import io.proximax.app.utils.AccountHelpers;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.LocalFileHelpers;
import io.proximax.app.utils.StringUtils;
import io.proximax.upload.UploadParameter;
import io.proximax.upload.UploadResult;
import io.proximax.upload.Uploader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author thcao
 */
public class SharingDialog extends AbstractController {

    @FXML
    private Label passwdLbl;
    @FXML
    private JFXTextField fileField;
    @FXML
    private JFXProgressBar progressBar;
    @FXML
    private JFXComboBox<String> uptypeCbx;
    @FXML
    private JFXTextField passwdField;
    @FXML
    private JFXTextField nameField;
    @FXML
    private JFXTextField addressField;
    @FXML
    private JFXButton shareBtn;
    @FXML
    private JFXButton cancelBtn;

    private LocalAccount localAccount;

    private LocalFile localFile = null;
    Task<Void> uploadTask = null;
    AsyncTask uploadAsync = null;
    private boolean bUploading = false;

    public SharingDialog(LocalAccount account, LocalFile localFile) {
        super(true);
        this.localAccount = account;
        this.localFile = localFile;
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        List<String> list = Arrays.asList(CONST.UPLOAD_TYPES);
        ObservableList<String> obList = FXCollections.observableList(list);
        uptypeCbx.setItems(obList);
        uptypeCbx.setValue(obList.get(localFile.uType));
        setPasswordVisible(localFile.uType == CONST.UTYPE_SECURE_PASSWORD);
        uptypeCbx.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (newValue.equals(CONST.UPLOAD_TYPES[CONST.UTYPE_SECURE_PASSWORD])) {
                setPasswordVisible(true);
            } else {
                setPasswordVisible(false);
            }
        });
        nameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            //System.out.println(newVal ? "Focused" : "Unfocused");
            if (!newVal) {
                String address = AccountHelpers.findAddressByName(localAccount, nameField.getText());
                if (AccountHelpers.isAddressValid(address)) {
                    addressField.setText(AccountHelpers.formatAddressPretty(address));
                }
            }
        });
        fileField.setText(localFile.fileName);
        passwdField.setText(localFile.password);
        progressBar.setProgress(0);
    }

    @FXML
    private void cancelBtn(ActionEvent event) {
        hide();
    }

    @FXML
    private void shareFile(ActionEvent event) {
        try {
            if (shareBtn.getText().equals("CANCEL")) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Do you want to cancel upload ?",
                        ButtonType.YES,
                        ButtonType.NO);
                ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(getMainApp().getIcon());
                alert.setTitle(CONST.APP_NAME);
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.YES) {
                    cancelJob();
                    bUploading = false;
                    shareBtn.setText("SHARE");
                }
                return;
            }
            String sPublicKey = "";
            final String sAddress = addressField.getText().trim().replace("-", "");
            if (AccountHelpers.isAddressValid(sAddress)) {
                try {
                    sPublicKey = AccountHelpers.getPublicKeyFromAddress(localAccount.connectionConfig, sAddress);
                } catch (MalformedURLException ex) {
                }
            }
            if (StringUtils.isEmpty(sPublicKey)) {
                ErrorDialog.showError(this, "Invalid address: " + sAddress);
                addressField.requestFocus();
                return;
            }
            if (LocalFileHelpers.isShared(localAccount.fullName, localAccount.network, localFile.id, sAddress)) {
                ErrorDialog.showError(this, "This file is already shared with your friend.");
                addressField.requestFocus();
                return;
            }
            int upType = uptypeCbx.getSelectionModel().getSelectedIndex();
            if (upType == CONST.UTYPE_SECURE_PASSWORD) {
                if (passwdField.getText().trim().length() < 10) {
                    ErrorDialog.showError(this, "Minimum length for password is 10");
                    passwdField.requestFocus();
                    return;
                }
            }
            final String friendName = nameField.getText();
            if (friendName.isEmpty()) {
                ErrorDialog.showError(this, "Please input recipient's name field");
                nameField.requestFocus();
                return;
            }

            if (!AccountHelpers.isFriendExisted(localAccount, friendName, sAddress)) {
                AccountHelpers.addFriend(localAccount, friendName, sAddress, sPublicKey);
            }
            String password = passwdField.getText();
            shareBtn.setText("CANCEL");
            String fPublicKey = sPublicKey;
            LocalFile sharedFile = new LocalFile();
            sharedFile.publicKey = fPublicKey;
            sharedFile.uType = upType;
            sharedFile.password = password;
            sharedFile.address = sAddress;
            sharedFile.shared = friendName;
            uploadTask = createUploadTask(sharedFile);
            progressBar.progressProperty().bind(uploadTask.progressProperty());
            uploadTask.setOnSucceeded(taskEvent -> {
                progressBar.progressProperty().unbind();
            });
            // run task in single-thread executor (will queue if another task is running):
            execJob.submit(uploadTask);
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorDialog.showError(this, ex.getMessage());
        }
    }

    private void setPasswordVisible(boolean bVisible) {
        passwdLbl.setVisible(bVisible);
        passwdField.setVisible(bVisible);
    }

    @Override
    public String getTitle() {
        return CONST.SHARING_TITLE;
    }

    @Override
    public String getFXML() {
        return CONST.SHARING_FXML;
    }

    /**
     * Create download task when user click download
     *
     * @param localFile
     * @return
     */
    private Task<Void> createUploadTask(LocalFile sharedFile) {
        final SharingDialog dlg = this;
        final int taskNumber = taskCount.incrementAndGet();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (localAccount.testNode()) {
                    int progress = 100;
                    bUploading = true;
                    updateMessage("Status: Sharing " + localFile.fileName + " to " + sharedFile.address);
                    updateProgress(progress, 999);
                    File fileCache = LocalFileHelpers.getSourceFile(localAccount, localFile);
                    if (fileCache == null) {
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
                            updateProgress(progress + 400 * sum / localFile.fileSize, 999);
                        }
                        progress = 500;
                    } else {
                        progress = 200;
                    }
                    try {
                        updateProgress(progress, 999);
                        LocalFile uploadFile = new LocalFile(localFile);
                        uploadFile.uType = sharedFile.uType;
                        uploadFile.password = sharedFile.password;
                        uploadFile.publicKey = sharedFile.publicKey;
                        uploadFile.address = sharedFile.address;
                        final String friendName = sharedFile.shared;
                        final String sAddress = sharedFile.address;
                        final int upType = sharedFile.uType;
                        final String password = sharedFile.password;
                        UploadParameter uploadParameter = LocalFileHelpers.createUploadFileParameter(localAccount, uploadFile, fileCache);
                        Uploader upload = new Uploader(localAccount.connectionConfig);
                        uploadAsync = upload.uploadAsync(uploadParameter,
                                AsyncCallbacks.create(
                                        (UploadResult uploadResult) -> {
                                            bUploading = false;
                                            ShareFile sharedFile = new ShareFile(uploadFile.id, friendName, sAddress, System.currentTimeMillis(), uploadResult.getData().getDataHash(), uploadResult.getTransactionHash(), upType, password, CONST.FILE_STATUS_NOR);
                                            LocalFileHelpers.shareLocalFile(localAccount, sharedFile);
                                            IApp.runSafe(() -> {
                                                updateProgress(999, 999);
                                                close();
                                                showParent();
                                            });
                                        },
                                        (Throwable ex) -> {
                                            bUploading = false;
                                            ex.printStackTrace();
                                            updateProgress(0, 999);
                                            if (isCancelled()) {
                                                return;
                                            }
                                            ErrorDialog.showErrorSafe(dlg, ex.getMessage());
                                        }));
                        while (!uploadAsync.isDone()) {
                            if (progress < 900) {
                                progress += 10;
                                updateProgress(progress, 999);
                            }
                            if (uploadAsync.isCancelled()) {
                                break;
                            }
                            Thread.sleep(100);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                return null;
            }
        };
        return task;
    }

    private AtomicInteger taskCount = new AtomicInteger(0);
    private ExecutorService execJob = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // allows app to exit if tasks are running
        return t;
    });

    private void cancelJob() {
        if (uploadAsync != null) {
            if (!uploadAsync.isDone()) {
                uploadAsync.cancel();
            }
            uploadAsync = null;
        }
        if (uploadTask != null) {
            uploadTask.cancel(true);
            uploadTask = null;
        }
    }

    @Override
    protected void dispose() {
        cancelJob();
        execJob.shutdownNow();
        try {
            if (!execJob.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                execJob.shutdownNow();
            }
        } catch (InterruptedException e) {
            execJob.shutdownNow();
        }
    }

    @Override
    protected boolean canExit() {
        if (bUploading || (uploadAsync != null && !uploadAsync.isDone())) {
            return false;
        }
        return true;
    }
}

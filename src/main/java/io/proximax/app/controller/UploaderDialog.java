package io.proximax.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import io.proximax.async.AsyncCallbacks;
import io.proximax.async.AsyncTask;
import io.proximax.app.core.ui.IApp;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.utils.AccountHelpers;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.LocalFileHelpers;
import io.proximax.upload.UploadParameter;
import io.proximax.upload.UploadResult;
import io.proximax.upload.Uploader;
import java.io.File;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author thcao
 */
public class UploaderDialog extends AbstractController {

    @FXML
    private Label fileLbl;
    @FXML
    private JFXTextField fileField;
    @FXML
    private Label titleLbl;
    @FXML
    private Label passwdLbl;
    @FXML
    private Label addressLbl;
    @FXML
    private JFXProgressBar progressBar;
    @FXML
    private JFXComboBox<String> uptypeCbx;
    @FXML
    private JFXTextField passwdField;
    @FXML
    private JFXTextField addressField;
    @FXML
    private JFXButton uploadBtn;
    @FXML
    private JFXButton cancelBtn;
    @FXML
    private JFXButton browserBtn;

    private LocalAccount localAccount;

    private File file = null;

    private boolean bUploading = false;

    private String curFolder = CONST.HOME;

    public UploaderDialog(LocalAccount account, String curFolder) {
        super(true);
        this.localAccount = account;
        this.curFolder = curFolder;
        this.file = null;
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        List<String> list = Arrays.asList(CONST.UPLOAD_TYPES);
        ObservableList<String> obList = FXCollections.observableList(list);
        uptypeCbx.setItems(obList);
        uptypeCbx.setValue(obList.get(0));
        setPasswordVisible(false);
        uptypeCbx.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            if (newValue.equals(CONST.UPLOAD_TYPES[CONST.UTYPE_SECURE_PASSWORD])) {
                setPasswordVisible(true);
            } else {
                setPasswordVisible(false);
            }
        });
        addressField.setPromptText(localAccount.address);
        progressBar.setProgress(0);
    }

    private void setPasswordVisible(boolean bVisible) {
        passwdLbl.setVisible(bVisible);
        passwdField.setVisible(bVisible);
    }

    public boolean isSelectFile() {
        return (this.file != null);
    }

    @FXML
    private void browseBtn(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");
        fileChooser.setInitialDirectory(mainApp.getCurrentFolder());
        this.file = fileChooser.showOpenDialog(primaryStage);
        if (this.file != null) {
            mainApp.saveCurrentDir(file.getAbsoluteFile().getParent());
            fileField.setText(file.getName());
        }
    }

    @FXML
    private void uploadFile(ActionEvent event) {
        try {
            if (uploadBtn.getText().equalsIgnoreCase("cancel")) {
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
                    uploadBtn.setText("UPLOAD");
                }
                return;
            }
            if (this.file == null) {
                ErrorDialog.showError(this, "Please choose file to upload");
                return;
            }
            int upType = uptypeCbx.getSelectionModel().getSelectedIndex();
            if (upType == CONST.UTYPE_PUBLIC || upType == CONST.UTYPE_SECURE_NEMKEYS) { //public key
                if (LocalFileHelpers.isExisted(localAccount, file, upType)) {
                    ErrorDialog.showError(this, "File " + file.getName() + " already existed");
                    return;
                }
            }
            if (upType == CONST.UTYPE_SECURE_PASSWORD) {
                if (passwdField.getText().trim().length() < 10) {
                    ErrorDialog.showError(this, "Minimum length for password is 10");
                    passwdField.requestFocus();
                    return;
                }
            }
            LocalFile localFile = new LocalFile();
            final String sAddress = addressField.getText().trim().replace("-", "");
            if (sAddress.isEmpty()) {
                localFile.address = localAccount.address;
                localFile.publicKey = localAccount.publicKey;
            } else {
                if (!AccountHelpers.isAddressValid(sAddress)) {
                    ErrorDialog.showError(this, "Invalid address: " + sAddress);
                    addressField.requestFocus();
                    return;
                }
                localFile.address = sAddress;
                localFile.publicKey = AccountHelpers.getPublicKeyFromAddress(localAccount.getApiHost(), localAccount.getApiPort(), sAddress);
            }
            localFile.password = passwdField.getText();
            if (localFile.password.trim().isEmpty()) {
                localFile.password = "";
            }
            uploadBtn.setText("CANCEL");
            localFile.privateKey = localAccount.privateKey;
            localFile.uType = upType;
            localFile.fileName = file.getName();
            localFile.filePath = file.getAbsolutePath();
            localFile.modified = file.lastModified();
            localFile.fileSize = file.length();
            localFile.category = curFolder;
            if (localAccount.isConnected()) {
                uploadTask = createUploadTask(localFile);
                progressBar.progressProperty().bind(uploadTask.progressProperty());
                uploadTask.setOnSucceeded(taskEvent -> {
                    progressBar.progressProperty().unbind();
                });
                execJob.submit(uploadTask);
            } else {
                try {
                    localFile.status = CONST.FILE_STATUS_FAILED;
                    LocalFileHelpers.addFile(localAccount, localFile);
                    IApp.runSafe(() -> {
                        close();
                        showParent();
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorDialog.showError(this, ex.getMessage());
        }
    }
    Task<Void> uploadTask = null;
    AsyncTask asyncTask = null;

    /**
     * Create download task when user click download
     *
     * @param localFile
     * @return
     */
    private Task<Void> createUploadTask(LocalFile localFile) {
        final UploaderDialog dlg = this;
        final int taskNumber = taskCount.incrementAndGet();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (localAccount.testNode()) {
                    bUploading = true;
                    updateMessage("Status: uploading ...");
                    updateProgress(100, 999);
                    UploadParameter uploadParameter = LocalFileHelpers.createUploadFileParameter(localAccount, localFile, file);
                    Uploader upload = new Uploader(localAccount.connectionConfig);                    
                    asyncTask = upload.uploadAsync(uploadParameter,
                            AsyncCallbacks.create(
                                    (UploadResult uploadResult) -> {
                                        bUploading = false;
                                        localFile.uploadDate = System.currentTimeMillis();
                                        localFile.hash = uploadResult.getData().getDataHash();
                                        localFile.nemHash = uploadResult.getTransactionHash();
                                        localFile.status = CONST.FILE_STATUS_TXN;
                                        LocalFileHelpers.addFile(localAccount, localFile);
                                        file = null;
                                        IApp.runSafe(() -> {
                                            updateProgress(999, 999);
                                            close();
                                            showParent();
                                        });

                                    },
                                    (Throwable ex) -> {
                                        ex.printStackTrace();
                                        bUploading = false;
                                        IApp.runSafe(() -> {
                                            updateProgress(0, 999);
                                            ErrorDialog.showError(dlg, ex.getMessage());
                                        });
                                        if (isCancelled()) {
                                            return;
                                        }
                                    }));
                    int progress = 100;
                    while (!asyncTask.isDone()) {
                        if (progress < 900) {
                            progress += 10;
                            updateProgress(progress, 999);
                        }
                        if (asyncTask.isCancelled()) {
                            break;
                        }
                        Thread.sleep(100);
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
        if (asyncTask != null) {
            if (!asyncTask.isDone()) {
                asyncTask.cancel();
            }
            asyncTask = null;
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
        if (bUploading || (asyncTask != null && !asyncTask.isDone())) {
            return false;
        }
        return true;
    }

    @Override
    public String getTitle() {
        return CONST.UPLOAD_TITLE;
    }

    @Override
    public String getFXML() {
        return CONST.UPLOAD_FXML;
    }
}

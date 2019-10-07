package io.proximax.app.controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;
import io.proximax.sdk.infrastructure.Listener;
import io.proximax.sdk.model.account.Address;
import io.proximax.sdk.model.transaction.Transaction;
import io.proximax.sdk.model.transaction.TransferTransaction;
import io.proximax.download.DownloadParameter;
import io.proximax.download.DownloadResult;
import io.proximax.download.Downloader;
import io.proximax.app.core.ui.IApp;
import io.proximax.app.db.LocalAccount;
import java.io.File;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import io.proximax.app.db.LocalFile;
import io.proximax.app.fx.control.ProxiBoxStatusBar;
import io.proximax.app.fx.control.text.RichTextEditor;
import io.proximax.app.utils.AccountHelpers;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.DBHelpers;
import io.proximax.app.utils.LocalFileHelpers;
import io.proximax.app.utils.MimeTypes;
import io.proximax.app.utils.StringUtils;
import io.proximax.async.AsyncCallbacks;
import io.proximax.async.AsyncTask;
import io.proximax.model.ProximaxMessagePayloadModel;
import io.proximax.service.RetrieveProximaxMessagePayloadService;
import io.proximax.upload.UploadParameter;
import io.proximax.upload.UploadResult;
import io.proximax.upload.Uploader;
import io.reactivex.disposables.Disposable;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * FXML Controller class
 *
 * @author thcao
 */
public class HomeDialog extends AbstractController {

    @FXML
    private ProxiBoxStatusBar statusBar;
    @FXML
    private Label statusLbl;
    @FXML
    private Label titleLbl;
    @FXML
    private Label userLbl;
    @FXML
    private JFXToggleButton nightModeBtn;
    @FXML
    private ToggleButton homeBtn;
    @FXML
    private JFXButton uploadBtn;
    @FXML
    private AnchorPane profilePane;
    @FXML
    private JFXTextField searchField;
    @FXML
    private TableView<LocalFile> fileTable;
    @FXML
    private TableColumn<LocalFile, String> nameCol;
    @FXML
    private TableColumn<LocalFile, String> modifiedCol;
    @FXML
    private TableColumn<LocalFile, String> typeCol;
    @FXML
    private TableColumn<LocalFile, Void> actionsCol;
    //Local account
    private LocalAccount localAccount;
    //Download file
    private File fileSave;
    //bind status text property
    private final StringProperty statusProperty;
    // The table's data
    private ObservableList<LocalFile> localFiles = FXCollections.observableArrayList();
    // The table's data
    private FilteredList<LocalFile> filteredData = null;
    // TableView type: 0 normal, 1: delete, 2: history    
    private int tableType = 0;

    //private String curFolder = CONST.HOME;
    private LocalFile curFolder = null;

    private BooleanProperty bConnected = new SimpleBooleanProperty(false);

    /**
     *
     * @param account
     */
    public HomeDialog(LocalAccount account) {
        super(false);
        this.statusProperty = new SimpleStringProperty();
        this.localAccount = account;
    }

    final ContextMenu fileMenu = new ContextMenu();

    private void viewFileAction() {
        viewFileAction(null);
    }

    private void viewFileAction(LocalFile localFile) {
        try {
            if (localFile == null) {
                localFile = filteredData.get(fileTable.getSelectionModel().getSelectedIndex());
            }
            FileViewDialog viewer = new FileViewDialog(localAccount, localFile);
            viewer.openWindow(this);
        } catch (IOException ex) {
            ErrorDialog.showError(this, ex.getMessage());
        }
    }

    private void downloadFileAction() {
        downloadFileAction(null);
    }

    private void downloadFileAction(LocalFile localFile) {
        if (localFile == null) {
            localFile = filteredData.get(fileTable.getSelectionModel().getSelectedIndex());
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName(localFile.fileName);
        fileChooser.setInitialDirectory(mainApp.getCurrentFolder());
        fileSave = fileChooser.showSaveDialog(primaryStage);
        if (fileSave != null) {
            mainApp.saveCurrentDir(fileSave.getAbsoluteFile().getParent());
            IntegerProperty pendingTasks = new SimpleIntegerProperty(0);
            Task<Void> task = createDownloadTask(localFile, fileSave);
            pendingTasks.set(pendingTasks.get() + 1);

            task.setOnSucceeded(taskEvent -> {
                statusBar.progressProperty().unbind();
                statusBar.textProperty().unbind();
                pendingTasks.set(pendingTasks.get() - 1);
            });
            // run task in single-thread executor (will queue if another task is running):
            execJob.submit(task);
        }
    }

    private void shareFileAction() {
        shareFileAction(null);
    }

    private void shareFileAction(LocalFile localFile) {
        try {
            if (localFile == null) {
                localFile = filteredData.get(fileTable.getSelectionModel().getSelectedIndex());
            }
            SharingDialog upload = new SharingDialog(localAccount, localFile);
            upload.openWindow(this);
        } catch (IOException ex) {
            ErrorDialog.showError(this, ex.getMessage());
        }
    }

    private void editFileAction() {
        editFileAction(null);
    }

    private void editFileAction(LocalFile localFile) {
        try {
            if (localFile == null) {
                localFile = filteredData.get(fileTable.getSelectionModel().getSelectedIndex());
            }
            if (MimeTypes.isPlainText(localFile.fileName)) {
                RichTextEditor dlg = new RichTextEditor(localAccount, localFile);
                dlg.openWindow(this);
            } else {
                throw new Exception("Don't support this format");
            }
        } catch (Exception ex) {
            ErrorDialog.showError(this, ex.getMessage());
        }

    }

    private void deleteFileAction() {
        deleteFileAction(null);
    }

    private void deleteFileAction(LocalFile localFile) {
        try {
            if (localFile == null) {
                localFile = filteredData.get(fileTable.getSelectionModel().getSelectedIndex());
            }
            String quest = "Do you want to delete a file ?";
            if (localFile.isFolder) {
                quest = "Do you want to delete all sub folders and files ?";
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, quest, ButtonType.YES, ButtonType.NO);
            ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(getMainApp().getIcon());
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                if (localFile.isFolder) {
                    LocalFileHelpers.deleteFolder(localAccount, localFile);
                } else {
                    LocalFileHelpers.deleteFile(localAccount, localFile);
                }
                updateFileTable();
            }
        } catch (Exception ex) {
            ErrorDialog.showError(this, ex.getMessage());
        }

    }

    private void renameFileAction() {
        renameFileAction(null);
    }

    private void renameFileAction(LocalFile localFile) {
        try {
            int selectedRowIndex = fileTable.getSelectionModel().getSelectedIndex();
            fileTable.edit(selectedRowIndex, fileTable.getColumns().get(0));
        } catch (Exception ex) {
            ErrorDialog.showError(this, ex.getMessage());
        }

    }

    private void historyFileAction() {
        historyFileAction(null);
    }

    private void historyFileAction(LocalFile localFile) {
        if (localFile == null) {
            localFile = filteredData.get(fileTable.getSelectionModel().getSelectedIndex());
        }
        showHistory(localFile);

    }

    private void moveFileAction() {
        moveFileAction(null);
    }

    private void moveFileAction(LocalFile localFile) {
        try {
            if (localFile == null) {
                localFile = filteredData.get(fileTable.getSelectionModel().getSelectedIndex());
            }
            FolderDialog dlg = new FolderDialog(localAccount, false, localFile);
            dlg.openWindow(this);
        } catch (IOException ex) {
            ErrorDialog.showError(this, ex.getMessage());
        }
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize() {
        try {
            userLbl.setText(localAccount.fullName);
            profilePane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                showProfile();
            });
            SeparatorMenuItem separatorM = new SeparatorMenuItem();
            fileMenu.getItems().add(createMenuItem("Copy Hash", "copyhash", this::copyHashToClipboard, true));
            fileMenu.getItems().add(createMenuItem("Copy Url", "copyurl", this::copyUrlToClipboard, true));
            fileMenu.getItems().add(createMenuItem("Copy Information", "copyinfo", this::copyFullInfoToClipboard, true));
            fileMenu.getItems().add(createMenuItem("Properties", "", this::showFileProperties, true));
            fileMenu.getItems().add(separatorM);
            fileMenu.getItems().add(createMenuItem("View", "view-img", this::viewFileAction, true));
            fileMenu.getItems().add(createMenuItem("Download", "download-img", this::downloadFileAction, true));
            fileMenu.getItems().add(createMenuItem("Sharing", "sharing-img-d", this::shareFileAction, true));
            fileMenu.getItems().add(createMenuItem("Edit", "edit-img", this::editFileAction, true));
            fileMenu.getItems().add(createMenuItem("Delete", "", this::deleteFileAction, false));
            fileMenu.getItems().add(createMenuItem("Rename", "", this::renameFileAction, false));
            fileMenu.getItems().add(createMenuItem("Move To Folder", "folder-img", this::moveFileAction, false));
            fileMenu.getItems().add(createMenuItem("History", "history-img", this::historyFileAction, true));
            fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            fileTable.setEditable(true);
            fileTable.setPlaceholder(null);
            nameCol = new TableColumn<>("FILE NAME");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
//            nameCol.setOnEditCommit((TableColumn.CellEditEvent<LocalFile, String> t) -> {
//                LocalFile localFile = ((LocalFile) t.getTableView().getItems().get(
//                        t.getTablePosition().getRow()));                
//                System.out.println("Edit: " + localFile);
//                updateFileTable(tableType);
//                
//
////                    ((LocalFile) t.getTableView().getItems().get(
////                            t.getTablePosition().getRow())).setName(t.getNewValue());
//            });
            modifiedCol = new TableColumn<>("MODIFIED");
            modifiedCol.setCellValueFactory(new PropertyValueFactory<>("modified"));
            typeCol = new TableColumn<>("TYPE");
            typeCol.setCellValueFactory(new PropertyValueFactory<>("member"));
            actionsCol = new TableColumn<>("ACTIONS");
            HomeDialog fileController = this;
            actionsCol.setCellFactory(col -> new TableCell<LocalFile, Void>() {
                private final HBox container;
                JFXButton historyBtn, folderBtn, viewBtn, downloadBtn, shareBtn, editBtn;

                {
                    viewBtn = createImageButtonCSS("view-img", null, "Quick View");
                    downloadBtn = createImageButtonCSS("download-img", null, "Download File");
                    shareBtn = createImageButtonCSS("sharing-img-d", null, "Share File");
                    editBtn = createImageButtonCSS("edit-img", null, "Edit File");
                    folderBtn = createImageButtonCSS("folder-img", null, "Move to Folder");
                    historyBtn = createImageButtonCSS("history-img", null, "View History");
//                    viewBtn.disableProperty().bind(Bindings.not(bConnected));
//                    editBtn.disableProperty().bind(Bindings.not(bConnected));
                    downloadBtn.disableProperty().bind(Bindings.not(bConnected));
                    shareBtn.disableProperty().bind(Bindings.not(bConnected));
                    downloadBtn.setOnAction(evt -> {
                        LocalFile localFile = filteredData.get(getIndex());
                        downloadFileAction(localFile);
                    });
                    shareBtn.setOnAction(evt -> {
                        LocalFile localFile = filteredData.get(getIndex());
                        shareFileAction(localFile);
                    });
                    viewBtn.setOnAction(evt -> {
                        LocalFile localFile = filteredData.get(getIndex());
                        viewFileAction(localFile);
                    });
                    editBtn.setOnAction(evt -> {
                        LocalFile localFile = filteredData.get(getIndex());
                        editFileAction(localFile);
                    });
                    historyBtn.setOnAction(evt -> {
                        LocalFile localFile = filteredData.get(getIndex());
                        historyFileAction(localFile);
                    });
                    folderBtn.setOnAction(evt -> {
                        LocalFile localFile = filteredData.get(getIndex());
                        moveFileAction(localFile);
                    });
                    //container = new HBox(5, viewBtn, downloadBtn, shareBtn, editBtn, folderBtn, historyBtn);
                    container = new HBox(5, viewBtn, downloadBtn, shareBtn, editBtn, historyBtn);
                    container.setStyle("-fx-alignment: CENTER;");
                }

                @Override
                public void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    int idx = getIndex();
                    if (idx >= 0 && idx < filteredData.size()) {
                        LocalFile localFile = filteredData.get(idx);
                        if (localFile.isFolder) {
                            setGraphic(null);
                        } else {
                            if (localFile.status == CONST.FILE_STATUS_NOR
                                    || localFile.status == CONST.FILE_STATUS_DEL) {
                                setGraphic(empty ? null : container);
                                if (getTableType() == CONST.TABLEVIEW_HIS) {
                                    historyBtn.setVisible(false);
                                    folderBtn.setVisible(false);
                                } else {
                                    historyBtn.setVisible(true);
                                    folderBtn.setVisible(true);
                                }
                                BooleanProperty bDisable2 = new SimpleBooleanProperty(LocalFileHelpers.isViewSupport(localFile) == false);
                                viewBtn.disableProperty().bind(Bindings.or(bDisable2, Bindings.not(bConnected)));
                                BooleanProperty bDisable3 = new SimpleBooleanProperty(LocalFileHelpers.isEditSupport(localFile) == false);
                                editBtn.disableProperty().bind(Bindings.or(bDisable3, Bindings.not(bConnected)));
                            } else {
                                //Label lbl = new Label("Waiting...");
                                JFXSpinner progress = new JFXSpinner();
                                progress.setPrefSize(1.0, 1.0);
                                //progress.setId("upload-progress");
                                setGraphic(empty ? null : progress);
                            }
                        }
                    } else {
                        setGraphic(empty ? null : container);
                    }
                }
            });
            fileTable.setRowFactory(tv -> {
                TableRow<LocalFile> row = new TableRow<>();
                row.setOnMouseClicked(event -> {
                    if (!row.isEmpty()) {
                        LocalFile rowFile = row.getItem();
                        if (event.getButton() == MouseButton.PRIMARY) {
                            if (event.getClickCount() == 2) {
                                if (rowFile.isFolder) {
                                    curFolder = rowFile;
                                    updateFileTable();
                                }
                            }
                        } else if (event.getButton() == MouseButton.SECONDARY) {
                            BooleanProperty bDisable = new SimpleBooleanProperty(rowFile.isFolder);
                            for (MenuItem item : fileMenu.getItems()) {
                                String mnuText = item.getText();
                                if ("History".equals(mnuText)) {
                                    BooleanProperty bDisable1 = new SimpleBooleanProperty(getTableType() == CONST.TABLEVIEW_HIS);
                                    item.disableProperty().bind(Bindings.or(bDisable, bDisable1));
                                } else {
                                    if ("Sharing".equals(mnuText) || "Download".equals(mnuText)) {
                                        item.disableProperty().bind(Bindings.or(bDisable, Bindings.not(bConnected)));
                                    } else if ("Edit".equals(mnuText)) {
                                        BooleanProperty bDisable2 = new SimpleBooleanProperty(LocalFileHelpers.isEditSupport(rowFile) == false);
                                        item.disableProperty().bind(Bindings.or(bDisable2, Bindings.or(bDisable, Bindings.not(bConnected))));
                                    } else if ("View".equals(mnuText)) {
                                        BooleanProperty bDisable2 = new SimpleBooleanProperty(LocalFileHelpers.isViewSupport(rowFile) == false);
                                        item.disableProperty().bind(Bindings.or(bDisable2, Bindings.or(bDisable, Bindings.not(bConnected))));
                                    } else if ("Delete".equals(mnuText) || "Rename".equals(mnuText) || "Move To Folder".equals(mnuText)) {
                                        BooleanProperty bDisable1 = new SimpleBooleanProperty(getTableType() == CONST.TABLEVIEW_HIS);
                                        item.disableProperty().bind(bDisable1);
                                    } else {
                                        item.disableProperty().bind(bDisable);
                                    }
                                }
                            }
                        }
                    } else {
                        if (event.getButton() == MouseButton.SECONDARY) {
                            goBack();
                        }
                    }
                });
                row.setOnDragDropped(new EventHandler<DragEvent>() {
                    @Override
                    public void handle(DragEvent event) {
                        Dragboard db = event.getDragboard();
                        boolean success = false;
                        if (db.hasString()) {
                            int dropIndex = -1;
                            if (!row.isEmpty()) {
                                dropIndex = row.getIndex();
                                LocalFile category = filteredData.get(dropIndex);
                                if (category.isFolder) {
                                    String text = db.getString();
                                    String[] selected = text.split(";");
                                    for (String i : selected) {
                                        LocalFile localFile = filteredData.get(Integer.parseInt(i));
                                        if (localFile.isFolder) {
                                            if (category.id != localFile.id) {
                                                LocalFileHelpers.moveFolderToFolder(localAccount, localFile, category);
                                            }
                                        } else {
                                            LocalFileHelpers.moveFileFolder(localAccount, localFile.id, category.filePath);
                                        }
                                    }
                                    updateFileTable();
                                }
                            }
                            success = true;
                        }
                        event.setDropCompleted(success);
                        event.consume();
                    }
                });

                // only display context menu for non-null items:
                row.contextMenuProperty().bind(
                        Bindings.when(Bindings.isNotNull(row.itemProperty()))
                                .then(fileMenu)
                                .otherwise((ContextMenu) null));

                return row;
            });

            nameCol.setCellFactory(col -> {
                TextFieldTableCell cell = new TextFieldTableCell<LocalFile, String>(new StringConverter<String>() {
                    @Override
                    public String toString(String object) {
                        return object.toString();
                    }

                    @Override
                    public String fromString(String string) {
                        return string;
                    }
                }) {
                    @Override
                    public void cancelEdit() {
                        super.cancelEdit();
                        fileTable.refresh();
                    }

                    @Override
                    public void commitEdit(String newValue) {
                        if (localFile != null) {
                            if (StringUtils.isNotEmpty(newValue)) {
                                if (localFile.isFolder) {
                                    if (!newValue.equals(localFile.category)) {
                                        int idx = localFile.filePath.lastIndexOf(localFile.category);
                                        String filePath = localFile.filePath.substring(0, idx) + newValue;
                                        int ret = LocalFileHelpers.checkFolderExisted(localAccount, filePath);
                                        if (ret == 0) {
                                            LocalFileHelpers.renameFolder(localAccount, localFile, newValue);
                                            super.commitEdit(newValue);
                                            return;
                                        } else if (ret == 1) {
                                            setStatus(newValue + " already exists");
                                        } else {
                                            setStatus("something wrong, cannot change new name " + newValue);
                                        }
                                    }
                                } else {
                                    if (!newValue.equals(localFile.reName)) {
                                        int ret = LocalFileHelpers.checkNameExisted(localAccount, localFile.category, newValue);
                                        if (ret == 0) {
                                            LocalFileHelpers.renameLocalFile(localAccount, localFile, newValue);
                                            super.commitEdit(newValue);
                                            return;
                                        } else if (ret == 1) {
                                            setStatus(newValue + " already exists");
                                        } else {
                                            setStatus("something wrong, cannot change new name " + newValue);
                                        }
                                    }
                                }
                            }
                            cancelEdit();
                        }
                    }

                    private LocalFile localFile = null;

                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            int idx = getIndex();
                            localFile = filteredData.get(idx);
                            if (localFile.isFolder) {
                                HBox box = new HBox();
                                box.setSpacing(10);
                                Label lbl = new Label(localFile.getName());
                                ImageView imageView = new ImageView();
                                imageView.setFitWidth(24.0);
                                imageView.setFitHeight(24.0);
                                imageView.setId("folder-img");
                                lbl.setStyle("-fx-font-weight:bold;");
                                box.getChildren().addAll(imageView, lbl);
                                setGraphic(empty ? null : box);
                                setText(null);
                            }
                        }
                    }
                };
                return cell;

            });
            fileTable.getColumns().add(nameCol);
            fileTable.getColumns().add(modifiedCol);
            fileTable.getColumns().add(typeCol);
            fileTable.getColumns().add(actionsCol);
            modifiedCol.setStyle("-fx-alignment: CENTER;");
            typeCol.setStyle("-fx-alignment: CENTER;");
            actionsCol.setStyle("-fx-alignment: CENTER;");
            filteredData = new FilteredList<>(localFiles, p -> true);
            fileTable.setItems(filteredData);
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(theFile -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    String lowerCaseFilter = newValue.toLowerCase();
                    if (String.valueOf(theFile.getName()).toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }
                    return false; // Does not match.
                });
            });
            updateFileTable(CONST.TABLEVIEW_HOME);
            fileTable.setOnDragDetected(new EventHandler<MouseEvent>() { //drag
                @Override
                public void handle(MouseEvent event) {
                    // drag was detected, start drag-and-drop gesture
                    List<Integer> selected = fileTable.getSelectionModel().getSelectedIndices();
                    if (selected != null) {
                        String str = "";
                        for (Integer i : selected) {
                            str += i + ";";
                        }
                        if (!str.isEmpty()) {
                            Dragboard db = fileTable.startDragAndDrop(TransferMode.ANY);
                            ClipboardContent content = new ClipboardContent();
                            content.putString(str);
                            db.setContent(content);
                        }
                        event.consume();
                    }
                }
            });
            fileTable.setOnDragOver(new EventHandler<DragEvent>() {
                @Override
                public void handle(DragEvent event) {
                    // data is dragged over the target 
                    Dragboard db = event.getDragboard();
                    if (event.getDragboard().hasString()) {
                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    }
                    event.consume();
                }
            });
            fileTable.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY) {
                    if (fileTable.getItems().isEmpty()) {
                        goBack();
                    }
                }
            });
            initializeStatusBar();
            //check server is alive
            initializeCheckingConnection();
            //monitor transaction
            initializeMonitorTransaction();
            // monitor job
            initializeJob();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void goBack() {
        if (curFolder != null && getTableType() == CONST.TABLEVIEW_HOME) {
            if (curFolder.fileId > 0) {
                curFolder = LocalFileHelpers.getFolder(localAccount, curFolder.fileId);
            } else {
                curFolder = null;
            }
            updateFileTable();
        }
    }

    public int getTableType() {
        return tableType;
    }

    public void setTableType(int tableType) {
        this.tableType = tableType;
    }

    private JFXButton createImageButtonCSS(String imgUrl, Runnable action, String toolTip) {
        ImageView iv = new ImageView();
        iv.setFitWidth(16);
        iv.setFitHeight(16);
        iv.setId(imgUrl);
        JFXButton button = new JFXButton("", iv);
        button.setGraphicTextGap(0.0);
        if (action != null) {
            button.setOnAction(evt -> {
                action.run();
            });
        }
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
        return button;
    }

    private MenuItem createMenuItem(String title, String styleClass, Runnable action, boolean disable) {
        ImageView iv = new ImageView();
        iv.setId(styleClass);
        MenuItem menuItem = new MenuItem(title, iv);
        //menuItem.getStyleClass().add(styleClass);
        if (action != null) {
            menuItem.setOnAction(evt -> {
                action.run();
            });
        }
        menuItem.setDisable(disable);
        return menuItem;
    }

    private JFXButton createImageButton(String imgUrl, Runnable action, String toolTip) {
        Image image = mainApp.getImageFromResource(imgUrl + ".png");
        ImageView iv = new ImageView(image);
        iv.setFitWidth(16);
        iv.setFitHeight(16);
        JFXButton button = new JFXButton("", iv);
        button.setGraphicTextGap(0.0);
        if (action != null) {
            button.setOnAction(evt -> {
                action.run();
            });
        }
        if (toolTip != null) {
            button.setTooltip(new Tooltip(toolTip));
        }
        return button;
    }

    private void copyUrlToClipboard() {
        LocalFile localFile = fileTable.getSelectionModel().getSelectedItem();
        if (localFile != null) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(localAccount.getNetworkConfiguration().getDownloadUrl() + localFile.hash);
            clipboard.setContent(content);
        }
    }

    private void copyHashToClipboard() {
        LocalFile localFile = fileTable.getSelectionModel().getSelectedItem();
        if (localFile != null) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(localFile.hash);
            clipboard.setContent(content);
        }
    }

    private void copyFullInfoToClipboard() {
        List<LocalFile> files = fileTable.getSelectionModel().getSelectedItems();
        String str = "";
        for (LocalFile localFile : files) {
            if (!localFile.isFolder) {
                str += localFile.fileName + "(" + localFile.fileSize + ")" + " - Link: " + localAccount.getNetworkConfiguration().getDownloadUrl() + localFile.hash + "\n";
            }
        }
        if (!StringUtils.isEmpty(str)) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(str);
            clipboard.setContent(content);
        }
    }

    /**
     * Initialize status bar
     */
    private void initializeStatusBar() {
        statusBar = new ProxiBoxStatusBar();
        statusBar.setImageSuccess(getMainApp().getImageFromResource(CONST.IMAGE_GREEN, 14.0, 14.0));
        statusBar.setImageFailed(getMainApp().getImageFromResource(CONST.IMAGE_RED, 14.0, 14.0));
        statusBar.setEventHandler(this);
        ((BorderPane) mainPane).setBottom(statusBar);
        BorderPane.setAlignment(statusBar, Pos.BOTTOM_CENTER);
        // text in status bar
        statusBar.textProperty().bind(statusProperty);
        List<String> list = localAccount.getNodes();
        ObservableList<String> obList = FXCollections.observableList(list);
        if (localAccount.getCurrentNodeIndex() == -1) {
            localAccount.setConnectionIndex(0);
        }
        statusBar.setNodeItems(obList, localAccount.getCurrentNodeIndex());
        // connection status
        setConnection(localAccount.isConnected());
    }

    /**
     * Set connection status in status bar
     *
     * @param connected
     */
    public void setConnection(Boolean connected) {
        this.statusBar.setImageStatus(connected);
        bConnected.set(connected);
        if (connected) {
            if (this.listener == null) {
                initializeMonitorTransaction();
            }
        } else {
            if (this.listener != null) {
                this.listener.close();
                this.listener = null;
            }
        }
    }

    /**
     * Set connection status: image, text in status bar
     *
     * @param connected
     */
    public void setConnectionStatus(Boolean connected) {
        if (connected) {
            setStatus(CONST.STR_CONNECTED);
        } else {
            setStatus(CONST.STR_DISCONNECTED);
        }
        this.statusBar.setImageStatus(connected);
    }

    /**
     * Set status text in status bar
     *
     * @param status
     */
    public void setStatus(String status) {
        this.statusProperty.set(CONST.STR_STATUS + status);
    }

    @FXML
    private void navHome(ActionEvent event) {
        tableType = CONST.TABLEVIEW_HOME;
        curFolder = null;
        updateFileTable(tableType);
    }

    private void showProfile() {
        try {
            UserProfileDialog dlg = new UserProfileDialog(localAccount);
            dlg.openWindow(this);
        } catch (IOException ex) {
        }
    }

    private void showFileProperties() {
        showFileProperties(null);
    }

    private void showFileProperties(LocalFile localFile) {
        try {
            if (localFile == null) {
                localFile = filteredData.get(fileTable.getSelectionModel().getSelectedIndex());
            }
            FilePropertiesDialog dlg = new FilePropertiesDialog(localAccount, localFile);
            dlg.openWindow(this);
            if (dlg.getResultType() == ButtonType.OK) {
                updateFileTable();
            }
        } catch (IOException ex) {
        }
    }

    @FXML
    private void allFilesNav(ActionEvent event) {
        tableType = CONST.TABLEVIEW_ALL;
        updateFileTable(tableType);
    }

    @FXML
    private void sharingNav(ActionEvent event) {
        tableType = CONST.TABLEVIEW_SHAR;
        updateFileTable(tableType);
    }

    @FXML
    private void logoutBtn(ActionEvent event) {
        resetDefaultFocus();
        try {
            close();
            showParent();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void uploadBtn(ActionEvent event) {
        try {
            UploaderDialog dialog = new UploaderDialog(localAccount, (curFolder != null) ? curFolder.filePath : CONST.HOME);
            dialog.setParent(this);
            dialog.openWindow();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        resetDefaultFocus();
    }

    @FXML
    private void nightModeBtn1(ActionEvent event) {
        if (nightModeBtn.isSelected()) {
            nightModeBtn.setSelected(false);
        } else {
            nightModeBtn.setSelected(true);
        }
        nightModeBtn(event);
    }

    @FXML
    private void nightModeBtn(ActionEvent event) {
        if (nightModeBtn.isSelected()) {
            mainApp.setTheme(1);
            reloadTheme();
        } else {
            mainApp.setTheme(0);
            reloadTheme();
        }
    }

    @FXML
    private void netCfgBtn(ActionEvent event) {
        try {
            NetworkDialog dlg = new NetworkDialog();
            dlg.setParent(this);
            dlg.openWindow();
            if (dlg.getResultType() == ButtonType.OK) {
                List<String> list = localAccount.getNodes();
                ObservableList<String> obList = FXCollections.observableList(list);
                statusBar.setNodeItems(obList, localAccount.getCurrentNodeIndex());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        resetDefaultFocus();
    }

    @FXML
    private void newFolderBtn(ActionEvent event) {
        try {
            FolderDialog dialog = new FolderDialog(localAccount, true, curFolder);
            dialog.setParent(this);
            dialog.openWindow();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        resetDefaultFocus();
    }

    private void resetDefaultFocus() {
        homeBtn.requestFocus();
    }

    @FXML
    private void showDeleted(ActionEvent event) {
        tableType = CONST.TABLEVIEW_DEL;
        updateFileTable(tableType);
    }

    private void showHistory(LocalFile localFile) {
        tableType = CONST.TABLEVIEW_HIS;
        updateFileTable(tableType, localFile.fileId);
    }

    private void updateFileTable() {
        updateFileTable(tableType);
    }

    private void updateFileTable(int status) {
        updateFileTable(status, 0);
    }

    private void updateFileTable(int status, int fileId) {
        List<LocalFile> listFiles = null;
        List<LocalFile> listFolders = null;
        tableType = status;
        switch (status) {
            case CONST.TABLEVIEW_DEL:
                titleLbl.setText("Deleted Files");
                listFiles = LocalFileHelpers.getDelFiles(localAccount.fullName, localAccount.network);
                if (listFiles != null) {
                    fileTable.getColumns().get(3).setVisible(false);
                }
                break;
            case CONST.TABLEVIEW_HIS:
                titleLbl.setText("Version History");
                listFiles = LocalFileHelpers.getHistoryFile(localAccount.fullName, localAccount.network, fileId);
                if (listFiles != null) {
                    fileTable.getColumns().get(3).setVisible(true);
                }
                break;
            case CONST.TABLEVIEW_ALL:
                titleLbl.setText("ALL FILES");
                listFiles = LocalFileHelpers.getFiles(localAccount.fullName, localAccount.network);
                fileTable.getColumns().get(3).setVisible(true);
                break;
            case CONST.TABLEVIEW_HOME:
                if (curFolder == null) {
                    titleLbl.setText("HOME");
                    listFolders = LocalFileHelpers.getFolders(localAccount.fullName, localAccount.network, curFolder);
                    listFiles = LocalFileHelpers.getFilesFolder(localAccount.fullName, localAccount.network, CONST.HOME);
                } else {
                    String title = "HOME" + curFolder.filePath;
                    titleLbl.setText(title.replace("/", ">"));
                    listFolders = LocalFileHelpers.getFolders(localAccount.fullName, localAccount.network, curFolder);
                    listFiles = LocalFileHelpers.getFilesFolder(localAccount.fullName, localAccount.network, curFolder.filePath);
                }
                fileTable.getColumns().get(3).setVisible(true);
                break;
            case CONST.TABLEVIEW_SHAR:
                titleLbl.setText("SHARING");
                listFiles = LocalFileHelpers.getSharingFiles(localAccount.fullName, localAccount.network, CONST.HOME);
                fileTable.getColumns().get(3).setVisible(true);
                break;
        }
        localFiles.clear();
        if (listFolders != null) {
            localFiles.addAll(listFolders);
        }
        if (listFiles != null) {
            localFiles.addAll(listFiles);
        }
    }

    private ScheduledService<Boolean> serverStatus = null;
    private ScheduledService<Boolean> jobStatus = null;

    /**
     * Initialize service checking connection
     */
    private void initializeCheckingConnection() {
        serverStatus = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                Task<Boolean> aliveTask = new Task<Boolean>() {
                    @Override
                    protected Boolean call() throws Exception {
                        if (localAccount.testNode()) {
                            //sendXPXMessage(netconf.getNetworkType(), localAccount.getApiUrl(), netconf.enc2, localAccount.address, XPX_10);
                            //NetworkUtils.send10XPX()
                            return true;
                        }
                        return false;
                    }

                    @Override
                    protected void succeeded() {
                        serverStatus.setPeriod(Duration.minutes(1));
                        if (getValue()) { // alive  
                            setConnection(Boolean.TRUE);
                        } else {
                            setConnection(Boolean.FALSE);
                        }
                    }
                };
                return aliveTask;
            }
        };
        serverStatus.setPeriod(Duration.seconds(1));
        serverStatus.start();
    }

//    /**
//     * Initialize service checking connection
//     */
    private void initializeJob() {
        jobStatus = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                Task<Boolean> aliveTask = new Task<Boolean>() {
                    @Override
                    protected Boolean call() throws Exception {
                        if (localAccount.testNode()) {
                            if (uploadTask != null) {
                                return true;
                            }
                            LocalFile localFile = DBHelpers.getJob(localAccount.fullName, localAccount.network);
                            if (localFile != null) {
                                try {
                                    LocalFileHelpers.updateLocalFileStatus(localAccount.fullName, localAccount.network, localFile.id, CONST.FILE_STATUS_QUEUE);
                                    IntegerProperty pendingTasks = new SimpleIntegerProperty(0);
                                    File fileSave = new File(localFile.filePath);
                                    uploadTask = createUploadTask(localFile, fileSave);
                                    pendingTasks.set(pendingTasks.get() + 1);
                                    uploadTask.setOnSucceeded(taskEvent -> {
                                        IApp.runSafe(() -> {
                                            statusBar.progressProperty().unbind();
                                            statusBar.textProperty().unbind();
                                        });
                                        pendingTasks.set(pendingTasks.get() - 1);
                                        uploadTask = null;
                                    });
                                    uploadTask.setOnCancelled(taskEvent -> {
                                        LocalFileHelpers.updateLocalFileStatus(localAccount.fullName, localAccount.network, localFile.id, CONST.FILE_STATUS_FAILED);
                                        pendingTasks.set(pendingTasks.get() - 1);
                                        uploadTask = null;
                                    });
                                    // run task in single-thread executor (will queue if another task is running):                                    
                                    execJob.submit(uploadTask);
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                        return true;
                    }

                };
                return aliveTask;
            }
        };
        jobStatus.setPeriod(Duration.seconds(10));
        jobStatus.start();
    }

    /**
     * Dispose when close application
     */
    @Override
    protected void dispose() {
        execJob.shutdownNow();
        try {
            if (!execJob.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                execJob.shutdownNow();
            }
        } catch (InterruptedException e) {
            execJob.shutdownNow();
        }
        if (localAccount == null) {
            localAccount.disconnect();
            localAccount = null;
        }
        if (serverStatus != null) {
            serverStatus.cancel();
        }
        if (jobStatus != null) {
            jobStatus.cancel();
        }
    }

    private AtomicInteger taskCount = new AtomicInteger(0);
    private ExecutorService execJob = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true); // allows app to exit if tasks are running
        return t;
    });

    /**
     * Create download task when user click download
     *
     * @param localFile
     * @param fileSave
     * @return
     */
    private Task<Void> createDownloadTask(LocalFile localFile, File fileSave) {
        final int taskNumber = taskCount.incrementAndGet();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                IApp.runSafe(() -> {
                    statusBar.progressProperty().bind(progressProperty());
                    statusBar.textProperty().bind(messageProperty());
                });
                updateMessage("Status: downloading " + fileSave.getName() + "...");
                DownloadParameter parameter = LocalFileHelpers.createDownloadParameter(localFile);
                Downloader download = new Downloader(localAccount.connectionConfig);
                DownloadResult downloadResult = download.download(parameter);
                updateProgress(100, 999);
                InputStream byteStream = downloadResult.getData().getByteStream();
                FileOutputStream fouts = new FileOutputStream(fileSave);
                byte[] buffer = new byte[1024];
                int read = 0;
                long sum = 0;
                while ((read = byteStream.read(buffer)) >= 0) {
                    fouts.write(buffer, 0, read);
                    sum += read;
                    updateProgress(100 + 800 * sum / localFile.fileSize, 999);
                }
                fouts.close();
                byteStream.close();
                updateProgress(999, 999);
                updateMessage("Status: download " + fileSave.getName() + " completed.");
                return null;
            }
        };
        return task;
    }

    private AsyncTask asyncTask = null;
    private Task<Void> uploadTask = null;

    private Task<Void> createUploadTask(LocalFile localFile, File file) {
        final int taskNumber = taskCount.incrementAndGet();
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                if (localAccount.testNode()) {
                    IApp.runSafe(() -> {
                        statusBar.progressProperty().bind(progressProperty());
                        statusBar.textProperty().bind(messageProperty());
                        updateMessage("Status: uploading " + localFile.filePath + " ...");
                        updateProgress(100, 999);
                    });
                    LocalFileHelpers.updateLocalFileStatus(localAccount.fullName, localAccount.network, localFile.id, CONST.FILE_STATUS_UPLOAD);
                    UploadParameter uploadParameter = LocalFileHelpers.createUploadFileParameter(localAccount, localFile, file);
                    Uploader upload = new Uploader(localAccount.connectionConfig);
                    asyncTask = upload.uploadAsync(uploadParameter,
                            AsyncCallbacks.create(
                                    (UploadResult uploadResult) -> {
                                        localFile.uploadDate = System.currentTimeMillis();
                                        localFile.hash = uploadResult.getData().getDataHash();
                                        localFile.nemHash = uploadResult.getTransactionHash();
                                        localFile.status = CONST.FILE_STATUS_TXN;
                                        LocalFileHelpers.updateLocalFile(localAccount.fullName, localAccount.network, localFile.id, localFile.hash, localFile.nemHash, localFile.uploadDate, localFile.status);
                                        IApp.runSafe(() -> {
                                            updateMessage("Status: upload " + localFile.filePath + " completed.");
                                            updateProgress(999, 999);
                                        });
                                    },
                                    (Throwable ex) -> {
                                        ex.printStackTrace();
                                        LocalFileHelpers.updateLocalFileStatus(localAccount.fullName, localAccount.network, localFile.id, CONST.FILE_STATUS_FAILED);
                                        IApp.runSafe(() -> {
                                            updateMessage("Status: issue upload " + localFile.filePath);
                                            updateProgress(0, 999);
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
                            IApp.runSafe(() -> {
                                updateMessage("Status: cancel upload " + localFile.filePath);
                                updateProgress(0, 999);
                            });
                            LocalFileHelpers.updateLocalFileStatus(localAccount.fullName, localAccount.network, localFile.id, CONST.FILE_STATUS_FAILED);
                            break;
                        }
                        Thread.sleep(100);
                    }
                } else {
                    LocalFileHelpers.updateLocalFileStatus(localAccount.fullName, localAccount.network, localFile.id, CONST.FILE_STATUS_FAILED);
                }
                return null;
            }
        };
        return task;
    }

    private void disconnectNode() {
        localAccount.disconnect();
        if (this.listener != null) {
            this.listener.close();
            this.listener = null;
        }
    }

    @Override
    public void handle(Event event) {
        if (event.getSource() instanceof ImageView) {
            if (event instanceof MouseEvent && ((MouseEvent) event).getClickCount() == 2) {
                ImageView imageView = (ImageView) event.getSource();
                if (imageView.getId().equals("status-image")) {
                    if (localAccount.isConnected()) {
                        localAccount.disconnect();
                        setConnection(false);
                    } else {

                    }
                }
            }
        } else if (event.getSource() instanceof ComboBox) {
            ComboBox comboBox = (ComboBox) event.getSource();
            if (comboBox.getId().equals("status-nodes")) {
                //setConnectionStatus(localAccount.connectToNode(comboBox.getSelectionModel().getSelectedIndex()));
                localAccount.disconnect();
                setConnection(false);
                localAccount.setConnectionIndex(comboBox.getSelectionModel().getSelectedIndex());
                if (serverStatus != null) {
                    serverStatus.cancel();
                    serverStatus.setPeriod(Duration.seconds(1));
                    serverStatus.restart();
                }
            }
        }
        super.handle(event);
    }

    @Override
    protected void onClosing(Event event) {
        super.onClosing(event);
        IApp.exit(0);
    }

    @Override
    protected void show() {
        primaryStage.show();
        updateFileTable();
    }

    @Override
    public String getTitle() {
        return CONST.HOME_TITLE + " [" + localAccount.fullName + "]";
    }

    @Override
    public String getFXML() {
        return CONST.HOME_FXML;
    }

    private Listener listener = null;

    private void initializeMonitorTransaction() {
        try {
            if (!localAccount.testNode()) {
                return;
            }
            listener = (Listener)localAccount.connectionConfig.getBlockchainNetworkConnection().getBlockchainApi().createListener();
            Address address = Address.createFromRawAddress(localAccount.address);
            listener.open().get();
            //listener.status(address);
            // wait for transaction to be confirmed
            Disposable subscribe = listener.confirmed(address).subscribe((Transaction txn) -> {
                if (txn instanceof TransferTransaction) {
                    TransferTransaction transferTransaction = (TransferTransaction) txn;
                    String sender = transferTransaction.getSigner().get().getAddress().plain();
                    String recipient = transferTransaction.getRecipient().getAddress().get().plain();
                    String nemHash = transferTransaction.getTransactionInfo().get().getHash().get();
                    if (localAccount.address.equals(recipient)) {
                        if (localAccount.address.equals(sender)) {
                            LocalFileHelpers.updateFileFromTransaction(localAccount.fullName, localAccount.network, nemHash, CONST.FILE_STATUS_NOR);
                            IApp.runSafe(() -> {
                                updateFileTable();
                            });
                        } else {
                            try {
                                RetrieveProximaxMessagePayloadService retrieveProximaxMessagePayloadService = new RetrieveProximaxMessagePayloadService(localAccount.connectionConfig.getBlockchainNetworkConnection());
                                ProximaxMessagePayloadModel result = retrieveProximaxMessagePayloadService.getMessagePayload(transferTransaction, localAccount.privateKey);
                                if (result != null && result.getData() != null) {
                                    if (result.getData().getDescription().contains(CONST.APP_NAME)) {
                                        LocalFile newFile = new LocalFile();
                                        newFile.fileName = result.getData().getName();
                                        newFile.hash = result.getData().getDataHash();
                                        Map<String, String> metaData = result.getData().getMetadata();
                                        newFile.shared = metaData.get("user");
                                        newFile.uType = StringUtils.parseInt(metaData.get("utype"), 0);
                                        newFile.fileSize = StringUtils.parseLong(metaData.get("size"), 0);
                                        newFile.nemHash = nemHash;
                                        newFile.modified = result.getData().getTimestamp();
                                        newFile.uploadDate = result.getData().getTimestamp();
                                        newFile.privateKey = localAccount.privateKey;
                                        newFile.publicKey = AccountHelpers.getPublicKeyFromAddress(localAccount.getApiHost(), localAccount.getApiPort(), sender);
                                        newFile.address = recipient;
                                        newFile.metadata = metaData.toString();
                                        newFile.status = CONST.FILE_STATUS_NOR;
                                        LocalFileHelpers.addSharedFile(localAccount, newFile);
                                        //LocalFileHelpers.updateFileFromTransaction(localAccount.fullName, localAccount.network, nemHash, CONST.FILE_STATUS_NOR);
                                        IApp.runSafe(() -> {
                                            updateFileTable();
                                        });
                                    }
                                }
                            } catch (Exception ex) {
                            }
                        }
                    }
                }
            });
        } catch (Exception ex) {
            ErrorDialog.showError(this, ex.getMessage());
        }
    }

    @Override
    protected boolean canExit() {
        if (uploadTask != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you quit application and cancel upload file ?", ButtonType.YES, ButtonType.NO);
            ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(getMainApp().getIcon());
            alert.showAndWait();
            if (alert.getResult() == ButtonType.YES) {
                jobStatus.cancel();
                if (asyncTask != null) {
                    while (!asyncTask.isDone()) {
                        asyncTask.cancel();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                        if (asyncTask.isCancelled()) {
                            break;
                        }
                    }
                }
                while (!uploadTask.isDone()) {
                    uploadTask.cancel(true);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                    if (uploadTask.isCancelled()) {
                        break;
                    }
                }
                uploadTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

}

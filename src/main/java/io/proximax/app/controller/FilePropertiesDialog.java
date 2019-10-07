package io.proximax.app.controller;

import com.jfoenix.controls.JFXTextField;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.LocalFile;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.LocalFileHelpers;
import io.proximax.app.utils.StringUtils;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;

/**
 *
 * @author thcao
 */
public class FilePropertiesDialog extends AbstractController {

    private LocalFile localFile = null;
    private LocalAccount localAccount = null;

    @FXML
    JFXTextField nameField;

    @FXML
    JFXTextField hashField;

    @FXML
    JFXTextField nemField;

    @FXML
    JFXTextField sizeField;

    @FXML
    JFXTextField dateField;

    public FilePropertiesDialog(LocalAccount localAccount, LocalFile localFile) {
        super(true);
        this.localFile = localFile;
        this.localAccount = localAccount;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        nameField.setText(localFile.reName);
        hashField.setText(localFile.hash);
        nemField.setText(localFile.nemHash);
        sizeField.setText(StringUtils.getFileSize(localFile.fileSize));
        dateField.setText(localFile.getModified());
    }

    @Override
    protected void dispose() {
    }

    @FXML
    protected void renameBtn(ActionEvent event) {
        try {
            String reName = nameField.getText();
            if (StringUtils.isNotEmpty(reName) && !localFile.reName.equals(reName)) {
                int ret = LocalFileHelpers.checkNameExisted(localAccount, localFile.category, reName);
                if (ret == 0) {
                    localFile.reName = reName;
                    LocalFileHelpers.renameLocalFile(localAccount, localFile, reName);
                } else if (ret == 1) {
                    throw new Exception(reName + " already exists");
                }
                setResultType(ButtonType.OK);
            } else {
                setResultType(ButtonType.FINISH);
            }
            close();
        } catch (Exception ex) {
            ErrorDialog.showError(this, ex.getMessage());
        }
    }

    @Override
    public String getTitle() {
        return CONST.FILEPROPDLG_TITLE;
    }

    @Override
    public String getFXML() {
        return CONST.FILEPROPDLG_FXML;
    }

}

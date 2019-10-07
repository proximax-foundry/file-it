package io.proximax.app.controller;

import io.proximax.app.utils.CONST;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 *
 * @author thcao
 */
public class ProxiDialog extends AbstractController {

    @FXML
    private Label titleLbl;
    @FXML
    private Label msgLbl;

    private String title;
    private int type;
    private StringProperty msgProperty = new SimpleStringProperty();

    public ProxiDialog(int type) {
        super(true);
        this.type = type;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        msgLbl.textProperty().bind(msgProperty);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContentText(String msg) {
        msgProperty.set(msg);
    }

    @Override
    protected void dispose() {
    }

    @Override
    public String getTitle() {
        return CONST.PROXIDLG_TITLE;
    }

    @Override
    public String getFXML() {
        return CONST.PROXIDLG_FXML;
    }

    public static void showError(AbstractController parent, String msg) {
        try {
            ProxiDialog dlg = new ProxiDialog(0);
            dlg.setContentText(msg);            
            dlg.setParent(parent);
            dlg.openWindow();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}

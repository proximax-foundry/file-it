package io.proximax.app.controller;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import io.proximax.app.core.ui.IApp;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.utils.AccountHelpers;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.StringUtils;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

/**
 *
 * @author Marvin
 */
public class LoginDialog extends AbstractController {

    @FXML
    private JFXPasswordField passwordField;
    @FXML
    private JFXComboBox<String> usernameCbx;
    @FXML
    private JFXTextField passField;
    @FXML
    private ToggleButton viewBtn;

    public LoginDialog() {
        super(false);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refreshUsers();

        passField.setManaged(false);
        passField.setVisible(false);
        passField.managedProperty().bind(viewBtn.selectedProperty());
        passField.visibleProperty().bind(viewBtn.selectedProperty());
        passwordField.managedProperty().bind(viewBtn.selectedProperty().not());
        passwordField.visibleProperty().bind(viewBtn.selectedProperty().not());
        passField.textProperty().bindBidirectional(passwordField.textProperty());

        usernameCbx.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (viewBtn.isSelected()) {
                passField.requestFocus();
            } else {
                passwordField.requestFocus();
            }
        });
        passwordField.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                loginBtn(null);
            }
        });
        passField.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                loginBtn(null);
            }
        });
    }

    @FXML
    void loginBtn(ActionEvent event) {
        try {
            String password = passwordField.getText();
            String fullName = usernameCbx.getValue();
            if (!StringUtils.isEmpty(fullName)) {
                String[] usernet = fullName.split("/");
                LocalAccount account = AccountHelpers.login(usernet[1], usernet[0], password);
                if (account != null) {
                    //account.connectNextNode();
                    passwordField.setText("");
                    hide();
                    HomeDialog fileController = new HomeDialog(account);
                    fileController.setParent(this);
                    fileController.openWindow();

                } else {
                    ErrorDialog.showError(this, "Account issue: invalid password");
                }
            } else {
                throw new Exception("Please select your account");
            }
        } catch (Exception e) {
            ErrorDialog.showError(this, e.getMessage());
        }

    }

    @FXML
    void networkBtn(ActionEvent event) {
        try {
            NetworkDialog dlg = new NetworkDialog();
            dlg.setParent(this);
            dlg.openWindow();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    void registerBtn(ActionEvent event) {
        try {
            // load
            hide();
            RegistrationDialog register = new RegistrationDialog();
            register.openWindow(this);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void dispose() {
        IApp.exit(0);
    }

    public static void showDialog(Stage stage, IApp app) {
        try {
            LoginDialog dialog = new LoginDialog();
            dialog.setResizable(false);
            dialog.openWindow();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void show() {
        refreshUsers();
        reloadTheme();
        super.show();
    }

    private void refreshUsers() {
        usernameCbx.getItems().clear();
        List<String> list = AccountHelpers.getAccounts();
        ObservableList<String> obList = FXCollections.observableList(list);
        usernameCbx.setItems(obList);
    }

    @Override
    public String getTitle() {
        return CONST.LOGIN_TITLE;
    }

    @Override
    public String getFXML() {
        return CONST.LOGIN_FXML;
    }
}

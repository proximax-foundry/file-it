package io.proximax.app.controller;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXTextField;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.recovery.AccountInfo;
import io.proximax.app.utils.AccountHelpers;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.NetworkUtils;
import io.proximax.app.utils.StringUtils;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

/**
 * FXML Controller class
 *
 * @author thcao
 */
public class RegistrationDialog extends AbstractController {

    @FXML
    private JFXTextField usernameField;

    @FXML
    private JFXPasswordField pwordField;

    @FXML
    private JFXPasswordField confirmPwordField;

    @FXML
    private JFXComboBox<String> networkCbx;

    @FXML
    private JFXTextField privateField;

    @FXML
    private CheckBox termCheckBx;

    @FXML
    private Label errorLbl;

    @FXML
    private Button btnRegister;

    @FXML
    private Button btnRecovery;

    private AccountInfo accountInfo = null;

    public RegistrationDialog() {
        super(false);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> obList = FXCollections.observableArrayList();
        obList.addAll(NetworkUtils.NETWORK_SUPPORT);
        networkCbx.setItems(obList);
        networkCbx.setValue(NetworkUtils.NETWORK_DEFAULT); //by default privatetest
        if (NetworkUtils.NETWORKS.size() <= 1) {
            networkCbx.setDisable(true);
        }
        btnRegister.disableProperty().bind(
                pwordField.textProperty().isEqualTo(confirmPwordField.textProperty()).not()
                        .or(
                                usernameField.textProperty().length().lessThan(5)
                        )
                        .or(
                                pwordField.textProperty().length().lessThan(8)
                        )
                        .or(
                                termCheckBx.selectedProperty().not()
                        )
        );
        usernameField.setTooltip(new Tooltip("Username"));
        usernameField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            //System.out.println(newVal ? "Focused" : "Unfocused");
            if (!newVal) {
                if (usernameField.textProperty().length().lessThan(5).get()) {
                    if (!btnRecovery.isFocused()) {
                        showError(usernameField, "username at least 5 characters");
                    }
                } else if (AccountHelpers.isExistAccount(usernameField.getText(), networkCbx.getValue())) {
                    showError(usernameField, "Account existed");
                } else {
                    errorLbl.setText("");
                }
            }
        });
        pwordField.setTooltip(new Tooltip("Password"));
        pwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (pwordField.textProperty().length().lessThan(8).get()) {
                    showError(pwordField, "password at least 8 characters");
                } else {
                    errorLbl.setText("");
                }
            }
        });
        confirmPwordField.setTooltip(new Tooltip("Confirm Password"));
        confirmPwordField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                if (pwordField.textProperty().isEqualTo(confirmPwordField.textProperty()).not().get()) {
                    showError(confirmPwordField, "password and confirm password not match");
                } else {
                    errorLbl.setText("");
                }
            }
        });
    }

    private void showError(Control control, String errMsg) {
        if (StringUtils.isEmpty(errMsg)) {
            errorLbl.setText("");
        } else {
            errorLbl.setText("Error: " + errMsg);
            if (control != null) {
                control.requestFocus();
            }
        }
    }

    @FXML
    void submitBtn(ActionEvent event) {
        // validate
        try {
            String fullName = usernameField.getText();
            String network = networkCbx.getValue();
            String password = pwordField.getText();
            if (AccountHelpers.isExistAccount(fullName, network)) {
                showError(usernameField, "Account existed");
                return;
            }
            String privateKey = privateField.getText().trim();
            if (!StringUtils.isEmpty(privateKey)) { //create new account
                if (!AccountHelpers.isPrivateKeyValid(privateKey)) {
                    showError(privateField, "Invalid private key");
                    return;
                }
            }
            LocalAccount localAccount = AccountHelpers.createAccount(fullName, network, password, privateKey);
            if (accountInfo != null) {
                AccountHelpers.updateAccountInfo(localAccount, accountInfo);
            }
            UserProfileDialog dlg = new UserProfileDialog(localAccount);
            dlg.openWindow(this);
            close(); // hide
            showParent();
        } catch (Exception e) {
            ErrorDialog.showError(this, e.getMessage());
        }

    }

    @FXML
    void recoveryBtn(ActionEvent event) {
        try {
            errorLbl.setText("");
            RecoveryDialog dlg = new RecoveryDialog();
            dlg.setParent(this);
            dlg.openWindow();
            accountInfo = null;
            if (dlg.getResultType() == ButtonType.OK) {
                accountInfo = dlg.getAccountInfo();
                privateField.setText(dlg.getAccountInfo().getPrivateKey());
                if (StringUtils.isEmpty(usernameField.getText())) {
                    usernameField.setText(dlg.getAccountInfo().getUserName());
                }
                privateField.requestFocus();                
            }
        } catch (Exception ex) {

        }

    }

    @Override
    protected void dispose() {
        showParent();
    }

    @Override
    public String getTitle() {
        return CONST.SIGNUP_TITLE;
    }

    @Override
    public String getFXML() {
        return CONST.SIGNUP_FXML;
    }

    @Override
    protected void closeBtn(ActionEvent event) {
        close();
        showParent();
    }
}

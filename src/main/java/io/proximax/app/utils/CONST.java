package io.proximax.app.utils;

import io.proximax.app.core.ui.IApp;
import java.text.SimpleDateFormat;

/**
 *
 * @author thcao
 */
public class CONST {

    public static IApp IAPP;    
    public static final String APP_NAME = "File It!";
    public static final String APP_FOLDER = "file-it";
    public static final String DB_VERSION = "1.0.0";    
    public static final char[] ENC_STRING = {'X', 'A', 'M', 'I', 'X', 'O', 'R', 'P'};
    public static final String[] UPLOAD_TYPES = {"Encrypt with keys", "Encrypt with password", "Public"};
    public static final int UTYPE_SECURE_NEMKEYS = 0;
    public static final int UTYPE_SECURE_PASSWORD = 1;
    public static final int UTYPE_PUBLIC = 2;
    public static final SimpleDateFormat SDF = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    public static final String IMAGE_PATH = "/fxml/css/%s/img/";
    public static final String IMAGE_GREEN = "green.png";
    public static final String IMAGE_RED = "red.png";
    public static final String STR_STATUS = "Status: ";
    public static final String STR_CONNECTED = "Connected";
    public static final String STR_DISCONNECTED = "Disconnected";
    public static final String LOGIN_TITLE = APP_NAME + " - Login";
    public static final String LOGIN_FXML = "ProxiBoxLogin.fxml";
    public static final String SIGNUP_TITLE = APP_NAME + " - Sign-up new account";
    public static final String SIGNUP_FXML = "ProxiBoxSignup.fxml";
    public static final String SIGNUP2_TITLE = APP_NAME + " - Sign-up Step 2";
    public static final String SIGNUP2_FXML = "Signup2.fxml";
    public static final String HOME_TITLE = APP_NAME + " - Home";
    public static final String HOME_FXML = "ProxiBoxFiles.fxml";
    public static final String UPLOAD_TITLE = APP_NAME + " - Upload File";
    public static final String UPLOAD_FXML = "ProxiBoxUploader.fxml";
    public static final String SHARING_TITLE = APP_NAME + " - Sharing File";
    public static final String SHARING_FXML = "ProxiBoxSharing.fxml";
    public static final String FILEVIEWER_TITLE = APP_NAME + " - Quick Viewer";
    public static final String FILEVIEWER_FXML = "ProxiBoxFileViewer.fxml";
    public static final String MEDIAVIEWER_FXML = "ProxiBoxMediaViewer.fxml";
    public static final String PROXIPAD_TITLE = APP_NAME + " - ProxiPad";
    public static final String PROXIPAD_FXML = "ProxiPad.fxml";
    public static final String PROXIDLG_TITLE = APP_NAME + " - Dialog";
    public static final String PROXIDLG_FXML = "ProxiDialog.fxml";
    public static final String ABOUTDLG_TITLE = APP_NAME + " - About";
    public static final String ABOUTDLG_FXML = "AboutUs.fxml";
    public static final String ERRORDLG_TITLE = APP_NAME + " - Error Message";
    public static final String ERRORDLG_FXML = "ErrorDialog.fxml";
    public static final String USERDLG_TITLE = APP_NAME + " - User Profile";
    public static final String USERDLG_FXML = "ProxiBoxProfile.fxml";
    public static final String FILEPROPDLG_TITLE = APP_NAME + " - File Properties";
    public static final String FILEPROPDLG_FXML = "ProxiBoxFileProperties.fxml";
    public static final String FOLDERDLG_TITLE = APP_NAME + " - Folder";
    public static final String FOLDERDLG_FXML = "ProxiBoxFolder.fxml";
    public static final int FILE_STATUS_NOR = 0;
    public static final int FILE_STATUS_DEL = 1;
    public static final int FILE_STATUS_TXN = 100;
    public static final int FILE_STATUS_FAILED = 101;
    public static final int FILE_STATUS_QUEUE = 102;
    public static final int FILE_STATUS_UPLOAD = 103;
    public static final int TABLEVIEW_HOME = 0;
    public static final int TABLEVIEW_DEL = 1;
    public static final int TABLEVIEW_HIS = 2;
    public static final int TABLEVIEW_ALL = 3;
    public static final int TABLEVIEW_SHAR = 4;
    public static final String DB_USER_TABLE = "PROXIBOX_USER";
    public static final String DB_FILE_TABLE = "PROXIBOX_FILES";
    public static final String DB_SHARE_TABLE = "PROXIBOX_SHARES";
    public static final String DB_CATEGORY_TABLE = "PROXIBOX_CATEGORY";
    public static final String DB_FRIEND_TABLE = "PROXIBOX_FRIEND";
    public static final String DB_JOB_TABLE = "PROXIBOX_JOB";
    public static final String PROXIBOX_ICON = "proxibox-icon.png";
    public static final String[] THEMES = {"Light", "Dark"};
    public static final String CSS_PATH = "/fxml/css/";
    public static final String CSS_THEME = "/fxml/css/%s.css";
    public static final String HOME = "/";
    public static final String[] DEFAULT_FOLDERS = {"Document", "Photo", "Video", "Music"};
    public static final long MONITOR_TIMEOUT = 3 * 60 * 1000; //3p
    public static final String URL_ACCOUNT_GET = "/account";
    public static final String DB_NETWORK_TABLE = "PROXIMAX_NETWORK";
    public static final String DB_COMMON_TABLE = "PROXIMAX_COMMON";
    public static final String DB_SUPPORT_TABLE = "PROXIMAX_SUPPORT";
    public static final String DB_APP_CONFIGURATION = "proximax.properties";
    public static final String NETWORKDLG_TITLE = APP_NAME + " - Network Configuration";
    public static final String NETWORKDLG_FXML = "NetworkDialog.fxml";
    public static final String FXML_PATH = "/fxml/ui/";
    public static final String[] SECURE_QUESTIONS = {
        "What was your childhood nickname?",
        "In what city did you meet your spouse/significant other?",
        "What is the name of your favorite childhood friend? ",
        "What street did you live on in third grade?",
        "What is your oldest sibling’s birthday month and year? (e.g., January 1900)",
        "What is the middle name of your youngest child?",
        "What is your oldest sibling's middle name?",
        "What school did you attend for sixth grade?",
        "What was your childhood phone number including area code? (e.g., 000-000-0000)",
        "What is your oldest cousin's first and last name?",
        "What was the name of your first stuffed animal?",
        "In what city or town did your mother and father meet?",
        "Where were you when you had your first kiss? ",
        "What is the first name of the boy or girl that you first kissed?",
        "What was the last name of your third grade teacher?",
        "In what city does your nearest sibling live?",
        "What is your youngest brother’s birthday month and year? (e.g., January 1900)",
        "What is your maternal grandmother's maiden name?",
        "In what city or town was your first job?",
        "What is the name of the place your wedding reception was held?",
        "What is the name of a college you applied to but didn't attend?",
        "Where were you when you first heard about 9/11?"};

}

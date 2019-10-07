package io.proximax.app.main;

import io.proximax.app.controller.LoginDialog;
import io.proximax.app.core.ui.IApp;
import io.proximax.app.utils.AccountHelpers;
import io.proximax.app.utils.CONST;
import io.proximax.app.utils.StringUtils;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class FileIt extends Application implements IApp {

    private Stage primaryStage = null;
    private int theme = 0;
    private Map<String, Object> caches = new HashMap<>();

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setTheme(int theme) {
        this.theme = theme;
    }

    public Image getIcon() {
        return new Image(getClass().getResourceAsStream(String.format(CONST.IMAGE_PATH, getCurrentTheme()) + CONST.PROXIBOX_ICON));
    }

    public Image getImageFromResource(String resUrl) {
        return new Image(getClass().getResourceAsStream(String.format(CONST.IMAGE_PATH, getCurrentTheme()) + resUrl));
    }

    public Image getImageFromResource(String resUrl, double w, double h) {
        return new Image(getClass().getResourceAsStream(String.format(CONST.IMAGE_PATH, getCurrentTheme()) + resUrl), w, h, true, true);
    }

    @Override
    public String getCurrentTheme() {
        return CONST.THEMES[theme];
    }

    @Override
    public String getCurrentThemeUrl() {
        return getClass().getResource(String.format(CONST.CSS_THEME, getCurrentTheme())).toExternalForm();
    }

    @Override
    public String getThemeUrl(int i) {
        return getClass().getResource(String.format(CONST.CSS_THEME, CONST.THEMES[i])).toExternalForm();
    }

    @Override
    public void start(Stage primaryStage) throws InterruptedException {
        try {
            CONST.IAPP = this;
            this.primaryStage = primaryStage;
            AccountHelpers.initUserHome();
            Thread.sleep(10000);
            primaryStage.getIcons().add(getIcon());
            LoginDialog.showDialog(primaryStage, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void dispose() {
    }

    public String getString(String key) {
        return (String) caches.get(key);
    }

    public void putString(String key, String val) {
        caches.put(key, val);
    }

    public String getCurrentDir() {
        String dir = (String) caches.get("latest.dir");
        if (StringUtils.isEmpty(dir)) {
            dir = System.getProperty("user.home");
        }
        return dir;
    }
    
    public File getCurrentFolder() {
        return new File(getCurrentDir());
    }

    public void saveCurrentDir(String sDir) {
        caches.put("latest.dir", sDir);
    }

}

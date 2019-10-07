package io.proximax.app.core.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.jar.Manifest;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 *
 * @author thcao
 */
public interface IApp {

    public static String OS = System.getProperty("os.name").toLowerCase();

    public Stage getPrimaryStage();

    public Image getIcon();

    public void setTheme(int theme);

    public Image getImageFromResource(String resUrl);

    public Image getImageFromResource(String resUrl, double w, double h);

    public void dispose();

    public String getCurrentTheme();

    public String getCurrentThemeUrl();

    public String getThemeUrl(int i);

    public String getCurrentDir();

    public File getCurrentFolder();

    public void saveCurrentDir(String sDir);

    public static void runSafe(final Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            Platform.runLater(runnable);
        }
    }

    public static void exit(int status) {
        if (Platform.isFxApplicationThread()) {
            Platform.exit();
            System.exit(0);
        }
    }

    public static boolean isWindow() {
        String fileSeparator = System.getProperty("file.separator");
        boolean windows;
        if (OS.matches(".*linux.*")) {
            windows = false;
        } else if (OS.matches(".*win.*")) {
            windows = true;
        } else if (fileSeparator.equals("\\")) {
            windows = true;
        } else {
            windows = false;
        }
        return windows;
    }

    public static boolean isMac() {
        return (OS.indexOf("mac") >= 0);
    }

    public static boolean isUnix() {
        return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
    }

    public static boolean isSolaris() {
        return (OS.indexOf("sunos") >= 0);
    }

    public static String getOS() {
        if (isMac()) {
            return "mac";
        }
        return "";
    }

    public static ImageView createImageViewFromIconData(byte[] data, ImageView iv) {
        return createImageViewFromIconData(data, 20, 20, iv);
    }

    public static ImageView createImageViewFromIconData(byte[] data, int w, int h, ImageView iv) {
        if (data == null) {
            return iv;
        }
        return createImageViewFromIconData(new ByteArrayInputStream(data), w, h);
    }

    public static Image createImageFromIconData(byte[] data, int w, int h) {
        return createImageFromIconData(new ByteArrayInputStream(data), w, h);
    }

    public static Image createImageFromIconData(InputStream data, int w, int h) {
        return new Image(data, w, h, true, true);
    }

    public static ImageView createImageViewFromIconData(InputStream data, int w, int h) {
        return new ImageView(new Image(data, w, h, true, true));
    }

    public static ImageView createImageViewFromIconData(InputStream data) {
        return new ImageView(new Image(data));
    }

    public static boolean isLocalIPFS() {
        try {
            URL nodeUrl = IApp.class.getResource("/node");
            if (new File(nodeUrl.toURI()).exists()) {
                return true;
            }
        } catch (Exception ex) {
        }
        return false;
    }

    public static String getJarPath() {
        Class clazz = IApp.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (classPath.startsWith("jar")) {
            return classPath;
        }
        return "";
    }

    public static String getManifestPath() {
        Class clazz = IApp.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (classPath.startsWith("jar")) {
            return classPath.substring(0, classPath.lastIndexOf("!") + 1)
                    + "/META-INF/MANIFEST.MF";
        }
        return "";
    }

    public static Manifest getManifest() {
        try {
            Class clazz = IApp.class;
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();
            if (classPath.startsWith("jar")) {
                String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1)
                        + "/META-INF/MANIFEST.MF";
                URL manifestUrl = new URL(manifestPath);
                return new Manifest(manifestUrl.openStream());
            }
        } catch (Exception ex) {
        }
        return null;

    }
}

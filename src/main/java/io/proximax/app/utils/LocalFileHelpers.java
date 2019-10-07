package io.proximax.app.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.proximax.download.DownloadParameter;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.db.ShareFile;
import io.proximax.app.db.LocalFile;
import io.proximax.upload.ByteArrayParameterData;
import io.proximax.upload.FileParameterData;
import io.proximax.upload.UploadParameter;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.activation.MimetypesFileTypeMap;

/**
 *
 * @author Thu Cao
 */
public class LocalFileHelpers {

    public static boolean isExisted(LocalAccount localAccount, File file, int shareType) {
        try {
            return DBHelpers.isFileExisted(localAccount.fullName, localAccount.network, file.getName(), file.lastModified(), shareType);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static LocalFile getFile(String fullName, String network, String fileName, int status) {
        try {
            return DBHelpers.getFile(fullName, network, fileName, status);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void addFile(LocalAccount localAccount, LocalFile localFile) {
        try {
            LocalFile oldFile = getFile(localAccount.fullName, localAccount.network, localFile.fileName, CONST.FILE_STATUS_NOR);
            if (oldFile == null) {
                DBHelpers.addFile(localAccount.fullName, localAccount.network, localFile);
            } else {
                DBHelpers.updateFile(localAccount.fullName, localAccount.network, oldFile, localFile);
            }
            localAccount.used += localFile.fileSize;
            DBHelpers.updateUserSpace(localAccount.fullName, localAccount.network, localAccount.used);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void addSharedFile(LocalAccount localAccount, LocalFile localFile) {
        try {
            LocalFile oldFile = getFile(localAccount.fullName, localAccount.network, localFile.fileName, CONST.FILE_STATUS_NOR);
            if (oldFile == null) {
                DBHelpers.addFile(localAccount.fullName, localAccount.network, localFile);
            } else {
                DBHelpers.updateFile(localAccount.fullName, localAccount.network, oldFile, localFile);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static List<LocalFile> getFiles(String fullName, String network) {
        try {
            DBHelpers.updateFileStatus(fullName, network, System.currentTimeMillis() - CONST.MONITOR_TIMEOUT); //timeout 3p
            return DBHelpers.getFiles(fullName, network);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static List<LocalFile> getFolders(String fullName, String network, LocalFile parentFolder) {
        try {
            return DBHelpers.getFolders(fullName, network, parentFolder);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static List<LocalFile> getFilesFolder(String fullName, String network, String folder) {
        try {
            DBHelpers.updateFileStatus(fullName, network, System.currentTimeMillis() - CONST.MONITOR_TIMEOUT); //timeout 3p
            return DBHelpers.getFilesFolder(fullName, network, folder);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static List<LocalFile> getSharingFiles(String fullName, String network, String folder) {
        try {
            return DBHelpers.getSharingFiles(fullName, network, folder);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static DownloadParameter createDownloadParameter(LocalFile localFile) {
        switch (localFile.uType) {
            case CONST.UTYPE_SECURE_NEMKEYS:
                return DownloadParameter.create(localFile.nemHash)
                        .withNemKeysPrivacy(localFile.privateKey, localFile.publicKey)
                        .build();
            case CONST.UTYPE_SECURE_PASSWORD:
                return DownloadParameter.create(localFile.nemHash)
                        .withPasswordPrivacy(localFile.password)
                        .build();
            default:
                return DownloadParameter.create(localFile.nemHash)
                        .build();
        }
    }

    public static void shareLocalFile(LocalAccount localAccount, ShareFile shareFile) {
        try {
            DBHelpers.shareLocalFile(localAccount.fullName, localAccount.network, shareFile);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static String getContentType(String fileName) {
        String mimeType = null;
        if (fileName.endsWith(".html")) {
            mimeType = "text/html";
        } else if (fileName.endsWith(".css")) {
            mimeType = "text/css";
        } else if (fileName.endsWith(".js")) {
            mimeType = "application/javascript";
        } else if (fileName.endsWith(".gif")) {
            mimeType = "image/gif";
        } else if (fileName.endsWith(".png")) {
            mimeType = "image/png";
        } else if (fileName.endsWith(".txt") || fileName.endsWith(".log")) {
            mimeType = "text/plain";
        } else if (fileName.endsWith(".xml")) {
            mimeType = "application/xml";
        } else if (fileName.endsWith(".json")) {
            mimeType = "application/json";
        } else {
            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
            mimeType = mimeTypesMap.getContentType(fileName);
        }
        return mimeType;
    }

    public static String getContentType(File file) {
        return getContentType(file.getPath());
    }

    public static UploadParameter createUploadFileParameter(LocalAccount localAccount, LocalFile localFile, File uploadFile) throws IOException {
        Map<String, String> metaData = null;
        if (StringUtils.isEmpty(localFile.metadata)) {
            metaData = createMetaData(localAccount, localFile);
            localFile.metadata = metaData.toString();
        } else {
            try {
                Gson gson = new Gson();
                metaData = gson.fromJson(localFile.metadata, new TypeToken<Map<String, String>>() {
                }.getType());
            } catch (Exception ex) {
                metaData = createMetaData(localAccount, localFile);
            }
        }
        switch (localFile.uType) {
            case CONST.UTYPE_SECURE_NEMKEYS:
                return UploadParameter
                        .createForFileUpload(
                                FileParameterData.create(
                                        uploadFile,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        new MimetypesFileTypeMap().getContentType(uploadFile),
                                        metaData),
                                localFile.privateKey)
                        .withRecipientAddress(localFile.address)
                        .withNemKeysPrivacy(localFile.privateKey, localFile.publicKey)
                        .build();
            case CONST.UTYPE_SECURE_PASSWORD:
                return UploadParameter
                        .createForFileUpload(
                                FileParameterData.create(
                                        uploadFile,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        new MimetypesFileTypeMap().getContentType(uploadFile),
                                        metaData),
                                localFile.privateKey)
                        .withRecipientAddress(localFile.address)
                        .withPasswordPrivacy(localFile.password)
                        .build();
            default:
                return UploadParameter
                        .createForFileUpload(
                                FileParameterData.create(
                                        uploadFile,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        new MimetypesFileTypeMap().getContentType(uploadFile),
                                        metaData),
                                localFile.privateKey)
                        .withRecipientAddress(localFile.address)
                        .build();
        }
    }

    public static void updateFile(String fullName, String network, LocalFile oldFile, LocalFile newFile) {
        try {
            DBHelpers.updateFile(fullName, network, oldFile, newFile);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateLocalFile(String userName, String network, int fileId, String hash, String nemHash, long uploadDate, int status) {
        try {
            DBHelpers.updateLocalFile(userName, network, fileId, hash, nemHash, uploadDate, status);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void updateLocalFileStatus(String userName, String network, int fileId, int status) {
        try {
            DBHelpers.updateLocalFileStatus(userName, network, fileId, status);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static List<LocalFile> getDelFiles(String fullName, String network) {
        try {
            return DBHelpers.getDelFiles(fullName, network);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static Map<String, String> createMetaData(LocalAccount localAccount, LocalFile localFile) {
        Map<String, String> metaData = new HashMap<String, String>();
        metaData.put("file", localFile.fileName);
        metaData.put("app", CONST.APP_NAME);
        metaData.put("user", localAccount.fullName);
        metaData.put("network", localAccount.network);
        metaData.put("utype", "" + localFile.uType);
        metaData.put("size", "" + localFile.fileSize);
        return metaData;
    }

    public static void updateFileFromTransaction(String fullName, String network, String nemHash, int status) {
        try {
            DBHelpers.updateFile(fullName, network, nemHash, status);
        } catch (SQLException ex) {
        }
    }

    public static UploadParameter createUploadBinaryParameter(LocalAccount localAccount, LocalFile localFile, byte[] data) {
        Map<String, String> metaData = createMetaData(localAccount, localFile);
        localFile.metadata = metaData.toString();
        switch (localFile.uType) {
            case CONST.UTYPE_SECURE_NEMKEYS:
                return UploadParameter
                        .createForByteArrayUpload(
                                ByteArrayParameterData.create(
                                        data,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        "",
                                        metaData),
                                localFile.privateKey)
                        .withNemKeysPrivacy(localFile.privateKey, localFile.publicKey)
                        .withRecipientAddress(localFile.address)
                        .build();
            case CONST.UTYPE_SECURE_PASSWORD:
                return UploadParameter
                        .createForByteArrayUpload(
                                ByteArrayParameterData.create(
                                        data,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        "",
                                        metaData),
                                localFile.privateKey)
                        .withPasswordPrivacy(localFile.password)
                        .withRecipientAddress(localFile.address)
                        .build();
            default:
                return UploadParameter
                        .createForByteArrayUpload(
                                ByteArrayParameterData.create(
                                        data,
                                        "Uploaded by " + CONST.APP_NAME,
                                        localFile.fileName,
                                        "",
                                        metaData),
                                localFile.privateKey)
                        .withRecipientAddress(localFile.address)
                        .build();
        }
    }

    public static List<LocalFile> getHistoryFile(String fullName, String network, int fileId) {
        try {
            return DBHelpers.getHistoryFile(fullName, network, fileId);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static List<String> getAllFolder(LocalAccount localAccount) {
        try {
            return DBHelpers.getAllFolder(localAccount.fullName, localAccount.network);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static void addFolder(LocalAccount localAccount, String newFolder, LocalFile parentFolder) {
        try {
            DBHelpers.addFolder(localAccount.fullName, localAccount.network, newFolder, parentFolder);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

//    public static void updateFolder(String fullName, String network, int folderId, String folder, String parent) {
//        try {
//            DBHelpers.updateFolder(fullName, network, folderId, folder, parent);
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//        }
//    }
    public static void moveFileFolder(LocalAccount localAccount, int id, String sFolder) {
        try {
            DBHelpers.moveFileFolder(localAccount.fullName, localAccount.network, id, sFolder);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isShared(String fullName, String network, int fileId, String address) {
        try {
            return DBHelpers.isShared(fullName, network, fileId, address);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static boolean isViewSupport(LocalFile localFile) {
        String mimeType = MimeTypes.getMimeType(localFile.fileName);
        if (mimeType.contains("video") || mimeType.contains("audio") || mimeType.contains("pdf") || mimeType.contains("image")
                || MimeTypes.isPlainText(localFile.fileName)) {
            return true;
        }
        return false;
    }

    public static boolean isEditSupport(LocalFile localFile) {
        return MimeTypes.isPlainText(localFile.fileName);
    }

    public static File getSourceFile(LocalAccount localAccount, LocalFile localFile) {
        File file = new File(localFile.filePath);
        if (file.exists()) {
            if (file.length() == localFile.fileSize && file.lastModified() == localFile.modified) {
                return file;
            }
        }
        String ext = getFileExtension(localFile.fileName);
        file = new File(getCacheDir(localAccount) + File.separator + localFile.nemHash + ext);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    public static String getFileExtension(String fileName) {
        return getFileExtension(new File(fileName));
    }

    public static String getFileExtension(File file) {
        String extension;
        try {
            String name = file.getName();
            extension = name.substring(name.lastIndexOf("."));
        } catch (Exception e) {
            extension = "";
        }
        return extension;
    }

    public static File createFileCache(LocalAccount localAccount, LocalFile localFile) {
        String ext = getFileExtension(localFile.fileName);
        return new File(getCacheDir(localAccount) + File.separator + localFile.nemHash + ext);
    }

    public static String getCacheDir(LocalAccount localAccount) {
        String filePath = System.getProperty("user.home") + File.separator + CONST.APP_FOLDER + File.separator + ".cache" + File.separator + localAccount.network + File.separator + localAccount.fullName;
        new File(filePath).mkdirs();
        return filePath;
    }

    public static void deleteFile(LocalAccount localAccount, LocalFile localFile) {
        try {
            DBHelpers.delFile(localAccount.fullName, localAccount.network, localFile.id);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void renameLocalFile(LocalAccount localAccount, LocalFile localFile, String name) {
        try {
            DBHelpers.renameLocalFile(localAccount.fullName, localAccount.network, localFile.id, name);
            localFile.reName = name;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static int checkNameExisted(LocalAccount localAccount, String folder, String name) {
        try {
            if (DBHelpers.isNameExisted(localAccount.fullName, localAccount.network, folder, name)) {
                return 1;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return -1;
        }
        return 0;
    }

    public static LocalFile getFolder(LocalAccount localAccount, String filePath) {
        try {
            return DBHelpers.getFolder(localAccount.fullName, localAccount.network, filePath);
        } catch (SQLException ex) {
        }
        return null;
    }

    public static int checkFolderExisted(LocalAccount localAccount, String filePath) {
        try {
            if (DBHelpers.isFolderExisted(localAccount.fullName, localAccount.network, filePath)) {
                return 1;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return -1;
        }
        return 0;
    }

    public static LocalFile getFolder(LocalAccount localAccount, int fileId) {
        try {
            return DBHelpers.getFolder(localAccount.fullName, localAccount.network, fileId);
        } catch (SQLException ex) {
        }
        return null;

    }

    public static void deleteFolder(LocalAccount localAccount, LocalFile folder) {
        List<LocalFile> folders = getFolders(localAccount.fullName, localAccount.network, folder);
        for (LocalFile f : folders) {
            deleteFolder(localAccount, f);
        }
        try {
            DBHelpers.deleteFileInFolder(localAccount.fullName, localAccount.network, folder.filePath);
        } catch (SQLException ex) {
        }
        try {
            DBHelpers.deleteFolder(localAccount.fullName, localAccount.network, folder.id);
        } catch (SQLException ex) {
        }

    }

    public static void updateFolderPath(LocalAccount localAccount, LocalFile curFolder, String newPath, int level) {
        List<LocalFile> folders = getFolders(localAccount.fullName, localAccount.network, curFolder);
        if (level != -1) {
            level += 1;
        }
        for (LocalFile f : folders) {
            updateFolderPath(localAccount, f, newPath + "/" + f.category, level);
        }
        try {
            DBHelpers.updateFileFolderPath(localAccount.fullName, localAccount.network, curFolder.filePath, newPath);
        } catch (SQLException ex) {
        }
        try {
            DBHelpers.updateFolderPath(localAccount.fullName, localAccount.network, curFolder.id, newPath, level);
        } catch (SQLException ex) {
        }
    }

    public static void renameFolder(LocalAccount localAccount, LocalFile curFolder, String newName) {
        int idx = curFolder.filePath.lastIndexOf(curFolder.category);
        String filePath = curFolder.filePath.substring(0, idx) + newName;
        updateFolderPath(localAccount, curFolder, filePath, -1);
        try {
            DBHelpers.renameFolder(localAccount.fullName, localAccount.network, curFolder, newName);
        } catch (SQLException ex) {
        }
    }

    public static void moveFolderToFolder(LocalAccount localAccount, LocalFile srcFolder, LocalFile destFolder) {
        String filePath = CONST.HOME + srcFolder.category;
        int level = 0;
        int parentId = 0;
        if (destFolder != null) {
            filePath = destFolder.filePath + "/" + srcFolder.category;
            level = destFolder.uType;
            parentId = destFolder.id;
        }
        updateFolderPath(localAccount, srcFolder, filePath, level);
        try {
            DBHelpers.moveFolderToFolder(localAccount.fullName, localAccount.network, srcFolder.id, parentId, level + 1, filePath);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void moveFolderToFolder(LocalAccount localAccount, LocalFile srcFolder, String sFolder) {
        LocalFile destFolder = LocalFileHelpers.getFolder(localAccount, sFolder);
        moveFolderToFolder(localAccount, srcFolder, destFolder);
    }

}

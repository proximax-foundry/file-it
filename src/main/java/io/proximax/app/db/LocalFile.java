package io.proximax.app.db;

import io.proximax.app.utils.CONST;
import io.proximax.app.utils.StringUtils;

/**
 *
 * @author thcao
 */
public class LocalFile {

    public int id;
    public String fileName;
    public String filePath;
    public String reName;
    public long modified;
    public long fileSize;
    public long uploadDate;
    public String hash;
    public String nemHash;
    public String publicKey;
    public String address;
    public String privateKey;
    public String password;
    public String category;
    public int uType;
    public String shared;
    public String metadata;
    public int rev;
    public int fileId;
    public int status;
    public long createdDate;
    public long updatedDate;

    public boolean isFolder;

    public LocalFile() {
        this.id = 0;
        this.fileName = "";
        this.modified = 0;
        this.fileSize = 0;
        this.uploadDate = 0;
        this.hash = "";
        this.nemHash = "";
        this.publicKey = "";
        this.category = "";
        this.password = "";
        this.address = "";
        this.privateKey = "";
        this.uType = 0;
        this.shared = "";
        this.metadata = "";
        this.fileId = id;
        this.rev = 0;
        this.status = 0;
        this.isFolder = false;
    }

    public LocalFile(boolean isFolder) {
        this.id = 0;
        this.fileName = "";
        this.modified = 0;
        this.fileSize = 0;
        this.uploadDate = 0;
        this.hash = "";
        this.nemHash = "";
        this.publicKey = "";
        this.category = "";
        this.password = "";
        this.privateKey = "";
        this.address = "";
        this.uType = 0;
        this.shared = "";
        this.metadata = "";
        this.fileId = id;
        this.rev = 0;
        this.status = 0;
        this.isFolder = isFolder;
    }

    public LocalFile(LocalFile localFile) {
        this.id = localFile.id;
        this.fileName = localFile.fileName;
        this.filePath = localFile.filePath;
        this.reName = localFile.reName;
        this.modified = localFile.modified;
        this.fileSize = localFile.fileSize;
        this.uploadDate = localFile.uploadDate;
        this.hash = localFile.hash;
        this.nemHash = localFile.nemHash;
        this.publicKey = localFile.publicKey;
        this.category = localFile.category;
        this.password = localFile.password;
        this.privateKey = localFile.privateKey;
        this.uType = localFile.uType;
        this.shared = localFile.shared;
        this.metadata = localFile.metadata;
        this.fileId = localFile.fileId;
        this.rev = localFile.rev;
        this.status = localFile.status;
        this.isFolder = localFile.isFolder;
        this.address = localFile.address;
    }

    public boolean isSecure() {
        return (uType != CONST.UTYPE_PUBLIC);
    }

    public String toString() {
        return new StringBuffer(id).append("/f[").append(fileName).append("]/s[").append(uType).append("]/d[").append(modified).append("]/h[").append(hash).append("]/n[").append(nemHash).append("]").toString();
    }

    public String getModified() {
        if (isFolder) {
            return "";
        } else {
            return CONST.SDF.format(modified);
        }
    }

    public String getName() {
        if (isFolder) {
            return category;
        } else {
            if (StringUtils.isEmpty(reName)) {
                reName = fileName;
            }
            return reName;
        }
    }

    public String getMember() {
        if (isFolder) {
            return "";
        } else {
            return (uType == CONST.UTYPE_PUBLIC ? "Public" : "Private");
        }
    }
}

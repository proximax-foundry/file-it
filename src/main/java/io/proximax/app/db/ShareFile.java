package io.proximax.app.db;

/**
 *
 * @author thcao
 * TABLE PROXIBOX_SHARES 
    ID          INTEGER PRIMARY KEY     AUTOINCREMENT,
    FILE_ID     INTEGER, 
    USERNAME    CHAR(255)    NOT NULL,
    ADDRESS     CHAR(255),
    SHARE_DATE  INTEGER,
    HASH        CHAR(255) NOT NULL,
    NEM_HASH    CHAR(255) NOT NULL,
    PASSWORD    CHAR(255),
    SHARE_TYPE       INTEGER
 */
public class ShareFile {
    public int id;
    public int fileId;
    public String userName;
    public String address;
    public long shareDate;
    public String hash; //ipfsHash
    public String nemHash; //nemHash
    public String password; //secure password
    public int shareType; //share type
    public int status;
    
    public ShareFile() {
        id = 0;
        fileId = 0;
        userName = "";
        address = "";
        shareDate = 0;
        hash = "";
        nemHash = "";
        password = "";
        shareType = 0;
        status = 0;        
    }
    
    public ShareFile(int fileId, String userName, String address, long shareDate, String hash, String nemHash, int shareType, String password, int status) {
        this.fileId = fileId;
        this.userName = userName;
        this.address = address;
        this.shareDate = shareDate;
        this.hash = hash;
        this.nemHash = nemHash;
        this.password = password;
        this.shareType = shareType;                
        this.status = status;
    }
    
    public String toString() {
        return new StringBuffer(id).append("/f[").append(fileId).append("]/s[").append(shareType).append("]/d[").append(shareDate).append("]/h[").append(hash).append("]/n[").append(nemHash).append("]").toString();
    }
}

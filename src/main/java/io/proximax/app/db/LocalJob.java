package io.proximax.app.db;

/**
 *
 * @author thcao
 */
public final class LocalJob {
    public static final int JOB_ACCOUNT = 1;
    public static final int JOB_UPLOAD = 2;
    public static final int JOB_DOWNLOAD = 3;
    public static final int JOB_SHARE = 4;
    public static final int JOB_EDIT = 5;
    public static final int JOB_VIEW = 6;    
    
    public static final int JOB_STATUS_DONE = 0;
    public static final int JOB_STATUS_NEW = 1;
    public static final int JOB_STATUS_QUEUE = 2;
    public static final int JOB_STATUS_RUNNING = 3;
    public static final int JOB_STATUS_FAILED = 4;
    public static final int JOB_STATUS_DELETE = 0;
    
    
    public int id;
    public String filePath;    
    public String publicKey;
    public String address;    
    public int jType;    
    public int fileId;
    public int status;
    public long createdDate;    
}

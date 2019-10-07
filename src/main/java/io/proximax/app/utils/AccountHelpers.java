package io.proximax.app.utils;

import java.io.File;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.proximax.sdk.model.account.Account;
import io.proximax.sdk.model.account.Address;
import io.proximax.connection.ConnectionConfig;
import io.proximax.app.db.LocalAccount;
import io.proximax.app.recovery.AccountInfo;
import io.proximax.utils.NemUtils;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class AccountHelpers {

    public static boolean isExistAccount(String fullName, String network) {
        String fileName = System.getProperty("user.home") + "/" + CONST.APP_FOLDER + "/users/" + network + "/" + fullName + ".wlt";
        return new File(fileName).exists();
    }

    public static boolean isExistAccountDB(String fullName, String network) {
        try {
            LocalAccount account = DBHelpers.getUser(fullName, network);
            if (account != null) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void initUserHome() {
        new File(System.getProperty("user.home") + "/" + CONST.APP_FOLDER + "/node").mkdirs();
        for (String network : NetworkUtils.NETWORK_SUPPORT) {
            new File(System.getProperty("user.home") + "/" + CONST.APP_FOLDER + "/users/" + network).mkdirs();
        }
        System.setProperty("user.dir", System.getProperty("user.home") + File.separator + CONST.APP_FOLDER);
        System.setProperty("vertx.httpServiceFactory.cacheDir", System.getProperty("user.dir"));
    }

    public static List<String> getAccounts() {
        List<String> list = new ArrayList<>();
        for (String network : NetworkUtils.NETWORK_SUPPORT) {
            if (NetworkUtils.isNetworkSupport(network)) {
                String appDir = System.getProperty("user.home") + "/" + CONST.APP_FOLDER + "/users/" + network;
                File file = new File(appDir);
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        String fileName = child.getName();
                        if (child.isFile() && fileName.endsWith(".wlt")) {
                            String userName = fileName.substring(0, fileName.lastIndexOf(".wlt"));
                            if (isExistAccountDB(userName, network)) {
                                list.add(network + "/" + userName);
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    public static void addAccountDB(String fullName, String password, String network, String encodedPrivateKey,
            String encodedPublicKey, String encodedAddress) {
        LocalAccount account = new LocalAccount(fullName, password, network, encodedPrivateKey, encodedPublicKey, encodedAddress, CONST.DB_VERSION);
        addAccountDB(account);
    }

    public static void addAccountDB(LocalAccount account) {
        try {
            if (!isExistAccount(account.fullName, account.network)) {
                DBHelpers.createUserDB(account.fullName, account.network);
                String password = CryptoUtils.encryptToBase64String(account.password.getBytes(), CONST.ENC_STRING);
                DBHelpers.addUser(account.fullName, password, account.network, account.privateKey, account.publicKey, account.address);
                for (String folder : CONST.DEFAULT_FOLDERS) {
                    DBHelpers.addFolder(account.fullName, account.network, folder, null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void updateAccountDB(String fullName, String password, String network, String encodedPrivateKey,
            String encodedPublicKey, String encodedAddress) throws Exception {
        LocalAccount account = new LocalAccount(fullName, password, network, encodedPrivateKey, encodedPublicKey, encodedAddress, CONST.DB_VERSION);
        updateAccountDB(account);
    }

    public static void updateAccountDB(LocalAccount account) throws Exception {
        String password = CryptoUtils.encryptToBase64String(account.password.getBytes(), CONST.ENC_STRING);
        DBHelpers.updateUser(account.fullName, password, account.network, account.privateKey, account.publicKey, account.address);
    }

    public static void updateUserStatus(String userName, String network, int status) throws Exception {
        DBHelpers.updateUserStatus(userName, network, status);
    }

    public static LocalAccount login(String fullName, String network, String password) {
        try {
            LocalAccount account = DBHelpers.getUser(fullName, network);
            if (account != null) {
                String enc_password = CryptoUtils.encryptToBase64String(password.getBytes(), CONST.ENC_STRING);
                if (account.password.equals(enc_password)) {
                    return account;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    public static LocalAccount loginDB(String fullName, String network, String password) {
        try {
            LocalAccount account = DBHelpers.getUser(fullName, network);
            if (account != null) {
                String enc_password = CryptoUtils.encryptToBase64String(password.getBytes(), CONST.ENC_STRING);
                if (account.password.equals(enc_password)) {
                    if (account.status == 0 || account.status == 1) {
                        boolean bDone = false;
                        do {
                            try {
                                int ret = account.connectNextNode();
                                if (ret == -1) {
                                    break;
                                } else if (ret == 1) {
                                    bDone = NetworkUtils.activeAccount(network, account);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                bDone = false;
                            }
                        } while (!bDone);
                    }
                    return account;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get public Key from address.
     *
     * @param connectionCfg
     * @param address the address string
     * @param connection peer connection
     * @return the public key from address
     */
    public static String getPublicKeyFromAddress(ConnectionConfig connectionCfg, String address) throws MalformedURLException {
        return getPublicKeyFromAddress(connectionCfg.getBlockchainNetworkConnection().getApiHost(), connectionCfg.getBlockchainNetworkConnection().getApiPort(), address);
    }

    /**
     * get public Key from address.
     *
     * @param apiUrl
     * @param apiPort
     * @param addressString the address string
     * @return the public key from address
     */
    public static String getPublicKeyFromAddress(String apiUrl, int apiPort, String addressString) {
        String queryResult = NetworkUtils.sendGetHTTP(apiUrl, apiPort, CONST.URL_ACCOUNT_GET + "/" + addressString);
        Gson gson = new Gson();
        JsonObject queryAccount = gson.fromJson(queryResult, JsonObject.class);
        return queryAccount.get("account").getAsJsonObject().get("publicKey").getAsString();
    }

    /**
     * get address from private key.
     *
     * @param connectionCfg
     * @param privateKeyString the private key string
     * @return the address from private key
     */
    public static String getAddressFromPrivateKey(ConnectionConfig connectionCfg, String privateKeyString) {
        try {
            NemUtils nemUtils = new NemUtils(connectionCfg.getBlockchainNetworkConnection().getNetworkType());
            return nemUtils.getAddressFromPrivateKey(privateKeyString).plain();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * get address from public key.
     *
     * @param connectionCfg
     * @param publicKeyString the public key string
     * @return the address from public key
     */
    public static String getAddressFromPublicKey(ConnectionConfig connectionCfg, String publicKeyString) {
        try {
            NemUtils nemUtils = new NemUtils(connectionCfg.getBlockchainNetworkConnection().getNetworkType());
            return nemUtils.getAddressFromPublicKey(publicKeyString).plain();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * Test if a string is hexadecimal
     *
     * @param str - A string to test
     *
     * @return True if correct, false otherwise
     */
    public static boolean isHexadecimal(String str) {
        return str.matches("^(0x|0X)?[a-fA-F0-9]+$");
    }

    /**
     * Check if a private key is valid
     *
     * @param privateKey A private key
     * @return True if valid, false otherwise
     */
    public static boolean isPrivateKeyValid(String privateKey) {
        if (privateKey.length() != 64 && privateKey.length() != 66) {
            return false;
        } else if (!isHexadecimal(privateKey)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check if a public key is valid
     *
     * @param publicKey A public key
     *
     * @return True if valid, false otherwise
     */
    public static boolean isPublicKeyValid(String publicKey) {
        if (publicKey.length() != 64) {
            return false;
        } else if (!isHexadecimal(publicKey)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check if a address is valid
     *
     * @param address A address
     *
     * @return True if valid, false otherwise
     */
    public static boolean isAddressValid(String address) {
        try {
            if (address.length() == 40) {
                Address addr = Address.createFromRawAddress(address);
                return addr != null;
            }
        } catch (Exception ex) {
        }
        return false;
    }

    public static String formatAddressPretty(String address) {
        return new StringBuilder().append(address.substring(0, 6)).
                append("-").
                append(address.substring(6, 6 + 6)).
                append("-").
                append(address.substring(6 * 2, 6 * 2 + 6)).
                append("-").
                append(address.substring(6 * 3, 6 * 3 + 6)).
                append("-").
                append(address.substring(6 * 4, 6 * 4 + 6)).
                append("-").
                append(address.substring(6 * 5, 6 * 5 + 6)).
                append("-").
                append(address.substring(6 * 6, 6 * 6 + 4)).toString();
    }

    public static Account generateBasicAccount(String network) {
        NemUtils nemUtils = new NemUtils(NetworkUtils.getNetworkType(network));
        Account newAccount = nemUtils.generateAccount();
        return newAccount;
    }

    public static LocalAccount createAccount(String fullName, String network, String password, String privateKey) throws Exception {
        Account nemAccount = null;
        if (StringUtils.isEmpty(privateKey)) { //create new account
            nemAccount = generateBasicAccount(network);
        } else {
            if (isPrivateKeyValid(privateKey)) {
                NemUtils nemUtils = new NemUtils(NetworkUtils.getNetworkType(network));
                nemAccount = nemUtils.getAccount(privateKey);
            } else {
                throw new Exception("Provided private key is not valid!");
            }
        }
        if (nemAccount != null) {
            AccountHelpers.addAccountDB(new LocalAccount(fullName, password, network, nemAccount.getPrivateKey(), nemAccount.getPublicKey(), nemAccount.getAddress().plain(), CONST.DB_VERSION));
            return loginDB(fullName, network, password);
        }
        return null;
    }

    public static AccountInfo getAccountInfo(LocalAccount localAccount) {
        AccountInfo account = null;
        try {
            account = DBHelpers.getAccountInfo(localAccount.fullName, localAccount.network);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return account;
    }

    public static void updateAccountInfo(LocalAccount localAccount, AccountInfo accountInfo) {
        try {
            DBHelpers.updateAccountInfo(localAccount.fullName, localAccount.network, accountInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String findAddressByName(LocalAccount localAccount, String name) {
        try {
            return DBHelpers.findFriendAddress(localAccount.fullName, localAccount.network, name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void addFriend(LocalAccount localAccount, String name, String address, String publicKey) {
        try {
            DBHelpers.addFriend(localAccount.fullName, localAccount.network, name, address, publicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isFriendExisted(LocalAccount localAccount, String name, String address) {
        try {
            return DBHelpers.isFriendExisted(localAccount.fullName, localAccount.network, name, address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

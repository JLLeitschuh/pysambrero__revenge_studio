package com.ninjaflip.androidrevenge.core.security;

import com.ninjaflip.androidrevenge.core.PreferencesManager;
import com.ninjaflip.androidrevenge.enums.OS;
import com.ninjaflip.androidrevenge.utils.NetworkAddressUtil;
import com.ninjaflip.androidrevenge.utils.Utils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by Solitario on 28/11/2017.
 * <p>
 * This class wraps all logc related to licensing system
 */
public class LicenseManager {

    private static LicenseManager INSTANCE;
    private static final String SKEY_FOR_DEFAULT_SERIAL = "I4K6UOAJjfk7BTb846tX98KwRviR2u85";
    static final String SKEY_FOR_GENERATED_SERIAL = "NqVWXAuL61nXu3eqyi0QQvuKjGuzyO7c";
    private static final String SERIAL_SUFFIX = "ANDRESTUDIO";
    private static String cachedUniqueComputerId = null;


    private LicenseManager() {
    }

    public static LicenseManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LicenseManager();
        }
        return INSTANCE;
    }

    public void checkpoint() throws Exception {
        if(!checkSoftwareLicenseIsOk()){
            exitSilently();
            //Configurator.getInstance().selfDelete();
        }
    }

    String getComputerUniqueId() throws IOException {
        if(cachedUniqueComputerId != null){
            return cachedUniqueComputerId;
        }

        OS osType;
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            osType = OS.WIN;
        } else if (osName.contains("mac")) {
            osType = OS.MAC;
        } else {
            osType = OS._NIX;
        }

        if (osType == OS.WIN) {
            // process = Runtime.getRuntime().exec(new String[] { "wmic", "DISKDRIVE", "get", "SerialNumber" });
            // process = Runtime.getRuntime().exec(new String[] { "wmic", "cpu", "get", "ProcessorId" });

            Process processBios = Runtime.getRuntime().exec(new String[]{"wmic", "bios", "get", "SerialNumber"});// bios serial
            processBios.getOutputStream().close();

            Scanner scBios = new Scanner(processBios.getInputStream());
            String propertyBios = scBios.next();
            String serialBios = scBios.next();
            //System.out.println("Bios "+ propertyBios + ": " + serialBios);
            scBios.close();

            Process processBoard = Runtime.getRuntime().exec(new String[]{"wmic", "baseboard", "get", "SerialNumber"});// motherboard serial
            processBoard.getOutputStream().close();

            Scanner scBoard = new Scanner(processBoard.getInputStream());
            String propertyBoard = scBoard.next();
            String serialBoard = scBoard.next();
            //System.out.println("MotherBoard "+ propertyBoard + ": " + serialBoard);
            scBoard.close();
            cachedUniqueComputerId = serialBios + "-" + serialBoard + "-" + SERIAL_SUFFIX;
            return cachedUniqueComputerId;
        } else if (osType == OS.MAC) {
            // TODO get serial for MAC OS
            return null;
        }else{
            // TODO get serial for linux
            return null;
        }
    }

    /**
     * This method generates a default license key that is displayed in the sign-in page.
     * When the user wants to activate the product, he sends that default license to us and we generate a real license
     * key and send it back to him
     *
     * @return default license key
     * @throws Exception
     */
    public String getSerialAsHexString() throws Exception {
        String computerUniqueIdDesEnc = TripleDesEncryption.encrypt(getComputerUniqueId(), SKEY_FOR_DEFAULT_SERIAL);
        return Utils.stringToHex(computerUniqueIdDesEnc);
    }


    /**
     * The default serial is the serial that the user will send to
     * us (getSerialAsHexString()), so we can generate a serial based on it and send it back to him
     *
     * @param defaultSerial the serail that is displayed in the sign in,
     * @return
     * @throws Exception
     */
    public String generateSerial(String defaultSerial) throws Exception {
        String hexToStringDefaultSerial = Utils.hexToString(defaultSerial);
        String decryptedComputerUniqueId = TripleDesEncryption.decrypt(hexToStringDefaultSerial, SKEY_FOR_DEFAULT_SERIAL);
        // decryptedComputerUniqueId must contain SERIAL_SUFFIX
        if (decryptedComputerUniqueId.endsWith(SERIAL_SUFFIX)) {
            String computerUniqueIdDesEnc = TripleDesEncryption.encrypt(decryptedComputerUniqueId, SKEY_FOR_GENERATED_SERIAL);
            return Utils.stringToHex(computerUniqueIdDesEnc);
        } else {
            throw new Exception("The provided serial does not respect the convention!");
        }
    }

    /**
     * This method checks license keys entered by the user, if they are valid or not
     *
     * @param serial the lincense key that we generate and send to user
     * @return true if valid, false otherwise
     */
    private boolean checkGeneratedSerialIsValid(String serial) {
        try {
            String hexToStringRealSerial = Utils.hexToString(serial);
            String decryptedComputerUniqueId = TripleDesEncryption.decrypt(hexToStringRealSerial, SKEY_FOR_GENERATED_SERIAL);
            return decryptedComputerUniqueId.equals(getComputerUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean checkSoftwareLicenseIsOk() {
        String licenseKey = PreferencesManager.getInstance().getLicenseKey();
        return (licenseKey != null) && checkGeneratedSerialIsValid(licenseKey);
    }

    public boolean updateSoftwareLicense(String serial) {
        if(checkGeneratedSerialIsValid(serial)){
            PreferencesManager.getInstance().saveLicenseKey(serial);
            return true;
        }else{
            return false;
        }
    }

    /************ Serial based on MAC address wiil be used in linux an Mac Os later *********/

    public String getSerialBaseOnMacAddress() throws NullPointerException {
        try {
            String osName = System.getProperty(new String(Base64.getDecoder().decode("b3MubmFtZQ==")));// os.name
            String osVersion = System.getProperty(new String(Base64.getDecoder().decode("b3MudmVyc2lvbg==")));//os.version
            String mac = NetworkAddressUtil.GetAddress(new String(Base64.getDecoder().decode("bWFj")));//mac
            String osArch = System.getProperty(new String(Base64.getDecoder().decode("b3MuYXJjaA==")));//os.arch

            String id = "";
            if (osName != null)
                id += osName;
            if (osVersion != null)
                id += "-" + osVersion;
            if (osArch != null)
                id += "-" + osArch;
            if (mac != null)
                id += "-" + mac;
            else
                return null;

            String sl = Utils.encdes(id, id);
            System.out.println("sl :" + sl);
            return sl;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Anti reverse engineering exit
     * <p>
     * This method invokes System.exit() using java reflexion
     */
    private void exitSilently() {
        try {
            //String parameter
            Class[] paramString = new Class[1];
            paramString[0] = String.class;
            //int parameter
            Class[] paramInt = new Class[1];
            paramInt[0] = Integer.TYPE;

            // TODO encrypt all strings here using DES => do not use base 64
            Class clsSystem = Class.forName(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));//java.lang.System
            Method exit = clsSystem.getMethod(new String(Base64.getDecoder().decode("ZXhpdA==")), paramInt);//exit
            exit.invoke(null, Integer.valueOf(new String(Base64.getDecoder().decode("LTIw"))));//System.exit(-20)
        }catch (Exception e){
            //System.exit(-20);
        }
    }


    public boolean isMacSerialValid() throws Exception {
        //String parameter
        Class[] paramString = new Class[1];
        paramString[0] = String.class;
        //int parameter
        Class[] paramInt = new Class[1];
        paramInt[0] = Integer.TYPE;

        // TODO encrypt all strings here using DES => do not use base 64
        Class clsSystem = Class.forName(new String(Base64.getDecoder().decode("amF2YS5sYW5nLlN5c3RlbQ==")));//java.lang.System
        Method getProperty = clsSystem.getMethod(new String(Base64.getDecoder().decode("Z2V0UHJvcGVydHk=")), String.class);//getProperty

        String osName = (String) getProperty.invoke(null, new String(Base64.getDecoder().decode("b3MubmFtZQ==")));// os.name
        String osVersion = (String) getProperty.invoke(null, new String(Base64.getDecoder().decode("b3MudmVyc2lvbg==")));//os.version
        String osArch = (String) getProperty.invoke(null, new String(Base64.getDecoder().decode("b3MuYXJjaA==")));//os.arch
        String mac = NetworkAddressUtil.GetAddress(new String(Base64.getDecoder().decode("bWFj")));//mac
        String id = "";
        if (osName != null)
            id += osName;
        if (osVersion != null)
            id += "-" + osVersion;
        if (osArch != null)
            id += "-" + osArch;
        if (mac != null)
            id += "-" + mac;
        String sl = Utils.encdes(id, id);

        Map<String, String> config = Utils.readConfigurationFile();
        assert config != null;
        String slCfg = config.get("sl");
        if (slCfg != null)
            return sl.equals(slCfg);
        else
            throw new IllegalStateException("sl parameter not found inside configuration file");
    }
}

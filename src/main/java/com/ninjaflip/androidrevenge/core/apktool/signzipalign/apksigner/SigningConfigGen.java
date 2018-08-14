package com.ninjaflip.androidrevenge.core.apktool.signzipalign.apksigner;


import com.ninjaflip.androidrevenge.core.apktool.signzipalign.ui.Arg;
import com.ninjaflip.androidrevenge.core.apktool.signzipalign.util.CmdUtil;
import com.ninjaflip.androidrevenge.enums.KeystoreLocation;
import com.ninjaflip.androidrevenge.enums.OS;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Responsible of creating {@link SigningConfig} from the arguments. Will decide weather to
 * use a debug keystore (and its logic) or a release keystore (or multiple).
 */
public class SigningConfigGen {

    private static String WIN_DEBUG_KS_DEFAULT = "\\.android\\debug.keystore";
    private static String NIX_DEBUG_KS_DEFAULT = "~/.android/debug.keystore";
    private static String DEBUG_KEYSTORE = "debug.keystore";

    private File tempDebugFile;

    public final List<SigningConfig> signingConfig;

    public SigningConfigGen(List<Arg.SignArgs> signArgsList, boolean ksIsDebug) {
        signingConfig = generate(signArgsList, ksIsDebug);
    }

    private List<SigningConfig> generate(List<Arg.SignArgs> signArgsList, boolean ksIsDebug) {
        if (ksIsDebug || signArgsList.isEmpty()) {
            File debugKeystore = null;
            KeystoreLocation location = KeystoreLocation.DEBUG_EMBEDDED;
            OS osType = CmdUtil.getOsType();

            if (ksIsDebug && !signArgsList.isEmpty()) {
                debugKeystore = new File(signArgsList.get(0).ksFile);

                if (!debugKeystore.exists() || !debugKeystore.isFile()) {
                    throw new IllegalArgumentException("debug keystore '" + signArgsList.get(0).ksFile + "' does not exist or is not a file");
                }
                location = KeystoreLocation.DEBUG_CUSTOM_LOCATION;
            }

            if (debugKeystore == null) {
                try {
                    File rootFolder = new File(SigningConfigGen.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile();
                    File sameFolderStore = new File(rootFolder, DEBUG_KEYSTORE);
                    if (sameFolderStore.exists()) {
                        debugKeystore = sameFolderStore;
                        location = KeystoreLocation.DEBUG_SAME_FOLDER;
                    }
                } catch (Exception e) {
                }
            }

            if (debugKeystore == null) {
                if (osType == OS.WIN) {
                    String userPath = System.getenv().get("USERPROFILE");
                    if (userPath != null) {
                        File userDebugKeystoreFile = new File(userPath, WIN_DEBUG_KS_DEFAULT);
                        if (userDebugKeystoreFile.exists()) {
                            debugKeystore = userDebugKeystoreFile;
                        }
                    }
                    location = KeystoreLocation.DEBUG_ANDROID_FOLDER;
                } else if (new File(NIX_DEBUG_KS_DEFAULT).exists()) {
                    debugKeystore = new File(NIX_DEBUG_KS_DEFAULT);
                    location = KeystoreLocation.DEBUG_ANDROID_FOLDER;
                }
            }

            if (debugKeystore == null) {
                try {
                    tempDebugFile = File.createTempFile("temp_", "_" + DEBUG_KEYSTORE);
                    // code migration java 1.7 to 1.6
                    Files.copy(getClass().getClassLoader().getResourceAsStream(DEBUG_KEYSTORE),
                            tempDebugFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    /*InputStream is = getClass().getClassLoader().getResourceAsStream(DEBUG_KEYSTORE);
                    FileUtils.copyInputStreamToFile(is,tempDebugFile);
                    is.close();*/
                    location = KeystoreLocation.DEBUG_EMBEDDED;
                    debugKeystore = tempDebugFile;
                } catch (Exception e) {
                    throw new IllegalStateException("could not load embedded debug keystore: " + e.getMessage(), e);
                }
            }

            return Collections.singletonList(new SigningConfig(
                    location,
                    0, true,
                    debugKeystore,
                    "androiddebugkey",
                    "android",
                    "android"
            ));
        } else {
            List<SigningConfig> signingConfigs = new ArrayList<SigningConfig>();

            for (Arg.SignArgs signArgs : signArgsList) {
                File keystore = new File(signArgs.ksFile);

                if (signArgs.ksFile == null || !keystore.exists() || keystore.isDirectory()) {
                    throw new IllegalArgumentException("passed keystore does not exist: " + signArgs.ksFile);
                }

                if (signArgs.alias == null || signArgs.alias.trim().isEmpty()) {
                    throw new IllegalArgumentException("when you provide your own keystore you must pass the keystore alias name");
                }


                Scanner s = new Scanner(System.in);

                if (signArgs.pass == null) {
                    System.out.println("Please enter the keystore password for config [" + signArgs.index + "] '" + signArgs.ksFile + "':");
                    if (System.console() != null) {
                        signArgs.pass = String.valueOf(System.console().readPassword());
                    } else {
                        signArgs.pass = s.next();
                    }
                }

                if (signArgs.keyPass == null) {
                    System.out.println("Please enter the key password for config [" + signArgs.index + "] alias '" + signArgs.alias + "' and keystore '" + signArgs.ksFile + "':");
                    if (System.console() != null) {
                        signArgs.keyPass = String.valueOf(System.console().readPassword());
                    } else {
                        signArgs.keyPass = s.next();
                    }
                }

                s.close();

                signingConfigs.add(new SigningConfig(
                        KeystoreLocation.RELEASE_CUSTOM,
                        signArgs.index, false, keystore,
                        signArgs.alias,
                        signArgs.pass,
                        signArgs.keyPass));
            }
            return signingConfigs;
        }
    }

    public void cleanUp() {
        if (tempDebugFile != null && tempDebugFile.exists()) {
            tempDebugFile.delete();
            tempDebugFile = null;
        }
    }
}

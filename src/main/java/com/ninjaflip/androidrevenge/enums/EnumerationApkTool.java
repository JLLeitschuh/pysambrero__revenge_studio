package com.ninjaflip.androidrevenge.enums;

/**
 * Created by Solitario on 20/06/2017.
 */

public class EnumerationApkTool {

    /**
     * used by web socket to notify for process state.
     * it also notifies the user if an ongoing process is still executing while
     * he tries to launch another process
     */
    public enum EnumProcessType {
        CREATE_NEW_PROJECT(100), PREVIEW_TEST(200), SIGN_ZIPALIGN(300), ADB_INSTALL_PREVIEW_APK(400), TEXT_SEARCH(500),
        TEXT_SEARCH_AND_REPLACE(600), PACKAGE_NAME_CHANGER(700), PACKAGE_RENAMER(800),
        BUILD_DEBUG_APK(900), BUILD_RELEASE_APK(1000), ADB_INSTALL(1100), INSTANT_RUN(1200), ADB_INSTALL_SINGLE(1300),
        ADB_BIN_INSTALLER(1400), MANIFEST_ENTRIES_RENAMER(1500);
        private final int value;

        EnumProcessType(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }

        public static String stringValue(int value) {
            if (value == CREATE_NEW_PROJECT.getValue())
                return CREATE_NEW_PROJECT.name();
            else if (value == PREVIEW_TEST.getValue())
                return PREVIEW_TEST.name();
            else if (value == SIGN_ZIPALIGN.getValue())
                return SIGN_ZIPALIGN.name();
            else if (value == ADB_INSTALL_PREVIEW_APK.getValue())
                return ADB_INSTALL_PREVIEW_APK.name();
            else if (value == TEXT_SEARCH.getValue())
                return TEXT_SEARCH.name();
            else if (value == TEXT_SEARCH_AND_REPLACE.getValue())
                return TEXT_SEARCH_AND_REPLACE.name();
            else if (value == PACKAGE_NAME_CHANGER.getValue())
                return PACKAGE_NAME_CHANGER.name();
            else if (value == PACKAGE_RENAMER.getValue())
                return PACKAGE_RENAMER.name();
            else if (value == BUILD_DEBUG_APK.getValue())
                return BUILD_DEBUG_APK.name();
            else if (value == BUILD_RELEASE_APK.getValue())
                return BUILD_RELEASE_APK.name();
            else if (value == ADB_INSTALL.getValue())
                return ADB_INSTALL.name();
            else if (value == INSTANT_RUN.getValue())
                return INSTANT_RUN.name();
            else if (value == ADB_INSTALL_SINGLE.getValue())
                return ADB_INSTALL_SINGLE.name();
            else if (value == ADB_BIN_INSTALLER.getValue())
                return ADB_BIN_INSTALLER.name();
            else if (value == MANIFEST_ENTRIES_RENAMER.getValue())
                return MANIFEST_ENTRIES_RENAMER.name();
            else
                return null;
        }

        public static String description(int value) {
            if (value == CREATE_NEW_PROJECT.getValue())
                return "CREATE NEW PROJECT";
            else if (value == PREVIEW_TEST.getValue())
                return "APK PREVIEW AND TEST";
            else if (value == SIGN_ZIPALIGN.getValue())
                return "APK SIGN_ZIPALIGN";
            else if (value == ADB_INSTALL_PREVIEW_APK.getValue())
                return "APK ADB INSTALL";
            else if (value == TEXT_SEARCH.getValue())
                return "TEXT SEARCH";
            else if (value == TEXT_SEARCH_AND_REPLACE.getValue())
                return "TEXT SEARCH AND REPLACE";
            else if (value == PACKAGE_NAME_CHANGER.getValue())
                return "PACKAGE NAME CHANGER";
            else if (value == PACKAGE_RENAMER.getValue())
                return "PACKAGE RENAMER";
            else if (value == BUILD_DEBUG_APK.getValue())
                return "BUILD DEBUG APK";
            else if (value == BUILD_RELEASE_APK.getValue())
                return "BUILD RELEASE APK";
            else if (value == ADB_INSTALL.getValue())
                return "APK ADB INSTALL";
            else if (value == INSTANT_RUN.getValue())
                return "APK INSTANT RUN";
            else if (value == ADB_INSTALL_SINGLE.getValue())
                return "APK INSTALLER";
            else if (value == ADB_BIN_INSTALLER.getValue())
                return "ADB INSTALLER";
            else if (value == MANIFEST_ENTRIES_RENAMER.getValue())
                return "MANIFEST ENTRIES RENAMER";
            else
                return null;
        }
    }

    /**
     * Process execution states
     */
    public enum EnumProcessState {
        STARTED(10), INTERRUPTED(20), ERROR(30), COMPLETED(40);
        private final int value;

        EnumProcessState(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }

        public static String stringValue(int value) {
            if (value == STARTED.getValue())
                return STARTED.name();
            else if (value == INTERRUPTED.getValue())
                return INTERRUPTED.name();
            else if (value == ERROR.getValue())
                return ERROR.name();
            else if (value == COMPLETED.getValue())
                return COMPLETED.name();
            else
                return null;
        }
    }

    /**
     * Apk build types
     */
    public enum EnumBuildType {
        DEBUG(100), RELEASE(200);
        private final int value;

        EnumBuildType(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }

        public static String stringValue(int value) {
            if (value == DEBUG.getValue())
                return DEBUG.name();
            else if (value == RELEASE.getValue())
                return RELEASE.name();
            else
                return null;
        }
    }


    /**
     * used to specify which log event is sent to client.
     * Logs are displayed in different DOM elements depending on log event type
     */
    public enum EnumLogType {
        GENERAL(100), TEXT_SEARCH(200), EDITOR_LOG(300), DEBUGGER_LOG(400);
        private final int value;

        EnumLogType(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }

        public static String stringValue(int value) {
            if (value == GENERAL.getValue())
                return GENERAL.name();
            else if (value == TEXT_SEARCH.getValue())
                return TEXT_SEARCH.name();
            else if (value == EDITOR_LOG.getValue())
                return EDITOR_LOG.name();
            else if (value == DEBUGGER_LOG.getValue())
                return DEBUGGER_LOG.name();
            else
                return null;
        }
    }
}

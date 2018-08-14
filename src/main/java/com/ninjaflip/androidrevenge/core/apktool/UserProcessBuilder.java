package com.ninjaflip.androidrevenge.core.apktool;

import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Solitario on 16/06/2017.
 * <p>
 * This class is made to manage all long running threads launched by user.
 * It provides th logic to start stop and clean any user thread
 */
public abstract class UserProcessBuilder {
    private static final List<UserProcessBuilder> listProcess = new ArrayList<>();

    private String id;
    private Thread thread;
    private String userUuid;
    private EnumerationApkTool.EnumProcessType userProcessType;
    private static boolean IS_SHUTTING_DOWN = false;
    private List<Process> myProcesses = new ArrayList<>();

    public UserProcessBuilder(String userUuid, EnumerationApkTool.EnumProcessType userProcessType) {
        id = UUID.randomUUID().toString();
        this.userUuid = userUuid;
        this.userProcessType = userProcessType;
    }

    public String getId() {
        return id;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public Thread getThread() {
        return thread;
    }

    public void addProcess(Process process) {
        this.myProcesses.add(process);
    }

    public EnumerationApkTool.EnumProcessType getUserProcessType() {
        return userProcessType;
    }

    public static void setIsShuttingDown(boolean isShuttingDown) {
        IS_SHUTTING_DOWN = isShuttingDown;
    }

    /**
     * Define logic here
     */
    public abstract void buildProcessLogic();

    /**
     * Define actions to execute when a sudden interruption happen
     * for example : user stops app, websocket session closed, user cancels execution ...
     * P.S: cleanOperation() will be executed inside a thread when called from killProcess()
     */
    public abstract void cleanOperation();


    /**
     * Launch the process
     */
    public void execute() {
        thread = new Thread(() -> {
            try {
                synchronized (listProcess) {
                    listProcess.add(UserProcessBuilder.this);
                }
                buildProcessLogic();
                synchronized (listProcess) {
                    listProcess.remove(UserProcessBuilder.this);
                }
                System.out.println("execute => process " + id + " finished normally ==> UserProcessBuilder size : " + listProcess.size());
                // no catch block because exception are managed inside buildProcessLogic() when overridden
            } catch (Exception e) {
                synchronized (listProcess) {
                    listProcess.remove(UserProcessBuilder.this);
                }
                System.out.println("execute => process " + id + " finished with errors ==> UserProcessBuilder size : " + listProcess.size());
            }
        });
        // priority between 5 and 10
        //int max = Thread.MAX_PRIORITY + 1;
        //int min = Thread.NORM_PRIORITY;
        //int priority = (int) (Math.random() * (max - min)) + min;
        //thread.setPriority(priority);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * Kill the process
     */
    private void killProcess() {
        System.out.println("killProcess => UserProcessBuilder killing process : " + id);
        try {
            // stop  main thread
            thread.interrupt();
            // destroy running processes
            for (Process proc : myProcesses) {
                if (proc != null) {
                    proc.destroyForcibly();
                }
            }
            Thread.sleep(1000);
            synchronized (listProcess) {
                listProcess.remove(UserProcessBuilder.this);
            }
        } catch (Exception e1) {
            System.err.println("killProcess => Error while killing process " + id + " : " + e1.getMessage());
            e1.printStackTrace();
            //thread.interrupt();
        }
        System.out.println("killProcess => process : " + id + " killed ==> UserProcessBuilder size : " + listProcess.size());
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }

        Thread cleaningThread = new Thread(() -> {
            try {
                System.out.println("killProcess => UserProcessBuilder cleaning process : " + id);
                cleanOperation();
                System.out.println("killProcess => UserProcessBuilder process : " + id + " killed and cleaned!");
            } catch (Exception e3) {
                e3.printStackTrace();
                System.err.println("killProcess => Error while cleaning process " + id + " : " + e3.getMessage());
            }
        });
        cleaningThread.start();
    }

    /**
     * logic to execute when user stops the app
     */
    public static void executeShutdownHook() {
        IS_SHUTTING_DOWN = true;
        synchronized (listProcess) {
            System.out.println("UserProcessBuilder ShutdownHook => starting shutdown hook...");
            for (UserProcessBuilder process : listProcess) {
                process.killProcess();
            }
            System.out.println("UserProcessBuilder ShutdownHook => shutdown hook DONE!");
        }
    }

    /**
     * When a websocket is closed we must stop the current executing tasks attached to that
     * websocket and perform cleaning operation
     *
     * @param userUuid the user's uuid which is the owner of the websocket
     */
    public static void onWebSocketClosed(String userUuid) {
        if (!IS_SHUTTING_DOWN) {
            System.out.println("onWebSocketClosed => UserProcessBuilder Executing onWebSocketClosed...");
            for (UserProcessBuilder process : listProcess) {
                if (process.getUserUuid().equals(userUuid)) {
                    System.out.println("onWebSocketClosed => UserProcessBuilder Killing process attached to userUuid : " + userUuid);
                    process.killProcess();
                }
            }
        }
    }

    /**
     * Cancel a process by its Id, this action is performed on user's demand, when he clicks on the cancel button
     *
     * @param processId UserProcessBuilder id
     */
    public static void cancelRunningProcess(String processId, String userUuid) throws IllegalAccessException {
        if (!IS_SHUTTING_DOWN) {
            for (UserProcessBuilder process : listProcess) {
                if (process.getId().equals(processId)) {
                    if (process.getUserUuid().equals(userUuid)) {
                        System.out.println("cancelRunningProcess => UserProcessBuilder Canceling running process : " + processId);
                        process.killProcess(); // Asynchronous
                        break;
                    } else {
                        throw new IllegalAccessException("Illegal access: a process can only be killed by its owner!");
                    }
                }
            }
        }
    }


    /**
     * Cancel all user's processes related to the project editor such as Text Search, build debug/relase apk ...
     * This method is called when the user closes the editor.
     *
     * @param userUuid the owner of processes
     */
    public static void cancelAllUserProcessesOnProjectEditorClose(String userUuid) {
        if (!IS_SHUTTING_DOWN) {
            List<UserProcessBuilder> processes = getUserRunningProcesses(userUuid);
            for (UserProcessBuilder process : processes) {
                if (process.getUserUuid().equals(userUuid)) {
                    EnumerationApkTool.EnumProcessType procType = process.getUserProcessType();
                    if (procType.equals(EnumerationApkTool.EnumProcessType.PACKAGE_NAME_CHANGER)
                            || procType.equals(EnumerationApkTool.EnumProcessType.PACKAGE_RENAMER)
                            || procType.equals(EnumerationApkTool.EnumProcessType.MANIFEST_ENTRIES_RENAMER)
                            || procType.equals(EnumerationApkTool.EnumProcessType.TEXT_SEARCH_AND_REPLACE)
                            || procType.equals(EnumerationApkTool.EnumProcessType.TEXT_SEARCH)
                            || procType.equals(EnumerationApkTool.EnumProcessType.BUILD_DEBUG_APK)
                            || procType.equals(EnumerationApkTool.EnumProcessType.BUILD_RELEASE_APK)
                            || procType.equals(EnumerationApkTool.EnumProcessType.INSTANT_RUN)
                            || procType.equals(EnumerationApkTool.EnumProcessType.ADB_INSTALL)
                            ) {
                        process.killProcess();
                    }
                }
            }
        }
    }



    /**
     * Get list of current executing processes of a certain user
     *
     * @param userUuid user's uuid
     * @return list of processes
     */
    public static List<UserProcessBuilder> getUserRunningProcesses(String userUuid) {
        synchronized (listProcess) {
            List<UserProcessBuilder> userRunningProcesses = new ArrayList<>();
            if (!IS_SHUTTING_DOWN) {
                for (UserProcessBuilder process : listProcess) {
                    if (process.getUserUuid().equals(userUuid)) {
                        userRunningProcesses.add(process);
                    }
                }
            }
            return userRunningProcesses;
        }
    }


    /**
     * Cancel all user's processes
     *
     * @param userUuid the owner of processes
     */
    public static void cancelAllUserProcesses(String userUuid) {
        if (!IS_SHUTTING_DOWN) {
            for (UserProcessBuilder process : listProcess) {
                if (process.getUserUuid().equals(userUuid)) {
                    process.killProcess(); // Asynchronous
                }
            }
        }
    }


    /**
     * This method return the number og current executing processes.
     * Used to warn the user when he tries to exit the app while there are more than one running process
     *
     * @return number of current executing processes
     */
    public static int getRunningProcessCount() {
        synchronized (listProcess) {
            return listProcess.size();
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!UserProcessBuilder.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final UserProcessBuilder other = (UserProcessBuilder) obj;
        return (this.id.equals(other.id));
    }
}

package com.ninjaflip.androidrevenge.core.scrapper;

import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;
import com.ninjaflip.androidrevenge.enums.EnumerationScrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Solitario on 16/06/2017.
 *
 * Manage scrapping processes
 */
public abstract class ScrapperProcessBuilder {
    private static final List<ScrapperProcessBuilder> listProcess = new ArrayList<>();

    private String id;
    private Thread thread;
    private String userUuid;
    private EnumerationScrapper.EnumProcessType processType;
    private static boolean IS_SHUTTING_DOWN = false;

    public ScrapperProcessBuilder(String userUuid, EnumerationScrapper.EnumProcessType processType) {
        id = UUID.randomUUID().toString();
        this.userUuid = userUuid;
        this.processType = processType;
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

    public EnumerationScrapper.EnumProcessType getProcessType() {
        return processType;
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
                    listProcess.add(ScrapperProcessBuilder.this);
                }
                buildProcessLogic();
                System.out.println("execute => process " + id + " finished normally");
                // no catch block because exception are managed inside buildProcessLogic() when overridden
            } finally {
                synchronized (listProcess) {
                    listProcess.remove(ScrapperProcessBuilder.this);
                }
                System.out.println("execute => Finally => process " + id + " finished ==> ScrapperProcessBuilder size : " + listProcess.size());
            }
        });
        int max = Thread.MAX_PRIORITY + 1;
        int min = Thread.NORM_PRIORITY;
        // priority between 5 and 10
        int priority = (int) (Math.random() * (max - min)) + min;
        thread.setPriority(priority);
        thread.start();
    }

    /**
     * Kill the process
     */
    private void killProcess() {
        System.out.println("killProcess => ScrapperProcessBuilder killing process : " + id);
        try {
            // stop  main thread
            thread.interrupt();
            Thread.sleep(1000);
            synchronized (listProcess) {
                listProcess.remove(ScrapperProcessBuilder.this);
            }
        } catch (Exception e1) {
            System.err.println("killProcess => Error while killing process " + id + " : " + e1.getMessage());
            e1.printStackTrace();
        }
        System.out.println("killProcess => process : " + id+ " killed ==> ScrapperProcessBuilder size : " + listProcess.size());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }

        Thread cleaningThread = new Thread(() -> {
            try {
                System.out.println("killProcess => ScrapperProcessBuilder cleaning process : " + id);
                cleanOperation();
                System.out.println("killProcess => ScrapperProcessBuilder process : " + id + " killed and cleaned!");
            } catch (Exception e3) {
                e3.printStackTrace();
                System.err.println("killProcess => Error while cleaning ScrapperProcessBuilder process " + id + " : " + e3.getMessage());
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
            System.out.println("ScrapperProcessBuilder ShutdownHook => starting shutdown hook...");
            for (ScrapperProcessBuilder process : listProcess) {
                process.killProcess();
            }
            System.out.println("ScrapperProcessBuilder ShutdownHook => shutdown hook DONE!");
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
            System.out.println("onWebSocketClosed => ScrapperProcessBuilder Executing onWebSocketClosed...");
            for (ScrapperProcessBuilder process : listProcess) {
                if (process.getUserUuid().equals(userUuid)) {
                    System.out.println("onWebSocketClosed => ScrapperProcessBuilder Killing process attached to userUuid : " + userUuid);
                    process.killProcess();
                }
            }
        }
    }

    /**
     * Cancel a process by its Id, this action is performed on user's demand, when he clicks on the cancel button
     *
     * @param processId ScrapperProcessBuilder id
     */
    public static void cancelRunningProcess(String processId, String userUuid) throws IllegalAccessException {
        if (!IS_SHUTTING_DOWN) {
            for (ScrapperProcessBuilder process : listProcess) {
                if (process.getId().equals(processId)) {
                    if (process.getUserUuid().equals(userUuid)) {
                        System.out.println("cancelRunningProcess => ScrapperProcessBuilder Canceling running process : " + processId);
                        process.killProcess();
                        break;
                    } else {
                        throw new IllegalAccessException("Illegal access: a process can only be killed by its owner!");
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
    public static List<ScrapperProcessBuilder> getUserRunningProcesses(String userUuid) {
        synchronized (listProcess) {
            List<ScrapperProcessBuilder> userRunningProcesses = new ArrayList<>();
            if (!IS_SHUTTING_DOWN) {
                for (ScrapperProcessBuilder process : listProcess) {
                    if (process.getUserUuid().equals(userUuid)) {
                        userRunningProcesses.add(process);
                    }
                }
            }
            return userRunningProcesses;
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
        if (!ScrapperProcessBuilder.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final ScrapperProcessBuilder other = (ScrapperProcessBuilder) obj;
        return (this.id.equals(other.id));
    }
}

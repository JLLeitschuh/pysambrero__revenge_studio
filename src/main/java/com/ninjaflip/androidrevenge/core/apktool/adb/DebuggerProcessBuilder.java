package com.ninjaflip.androidrevenge.core.apktool.adb;

import com.ninjaflip.androidrevenge.core.apktool.UserProcessBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Solitario on 16/06/2017.
 */
public abstract class DebuggerProcessBuilder {
    private static final List<DebuggerProcessBuilder> listActiveDebuggerProcess = new ArrayList<>();

    private String id;
    private Thread thread;
    private String userUuid;
    private static boolean IS_SHUTTING_DOWN = false;
    private List<Process> processes = new ArrayList<>();

    public DebuggerProcessBuilder(String userUuid) {
        id = UUID.randomUUID().toString();
        this.userUuid = userUuid;
    }

    public String getId() {
        return id;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void addProcess(Process process) {
        this.processes.add(process);
    }

    public void setProcesses(List<Process> processes) {
        this.processes = processes;
    }

    public Thread getThread() {
        return thread;
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
                synchronized (listActiveDebuggerProcess) {
                    listActiveDebuggerProcess.add(DebuggerProcessBuilder.this);
                }
                buildProcessLogic();
                synchronized (listActiveDebuggerProcess) {
                    listActiveDebuggerProcess.remove(DebuggerProcessBuilder.this);
                }
                System.out.println("execute => process " + id + " finished normally");
                // no catch block because exception are managed inside buildProcessLogic() when overridden
            } catch (Exception e) {
                synchronized (listActiveDebuggerProcess) {
                    listActiveDebuggerProcess.remove(DebuggerProcessBuilder.this);
                }
                System.out.println("execute => process " + id + " finished with errors ==> DebuggerProcessBuilder size : " + listActiveDebuggerProcess.size());
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
        System.out.println("killProcess => killing DebuggerProcess : " + id);
        try {
            thread.interrupt();
            // destroy running processes
            for (Process proc : processes) {
                if (proc != null) {
                    proc.destroyForcibly();
                }
            }
            Thread.sleep(1000);
            synchronized (listActiveDebuggerProcess) {
                listActiveDebuggerProcess.remove(DebuggerProcessBuilder.this);
            }
        } catch (Exception e1) {
            System.err.println("killProcess => Error while killing process " + id + " : " + e1.getMessage());
            e1.printStackTrace();
            thread.interrupt();
        }
        System.out.println("killProcess => process : " + id + " killed ==> DebuggerProcessBuilder size : " + listActiveDebuggerProcess.size());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }

        Thread cleaningThread = new Thread(() -> {
            try {
                System.out.println("killProcess => DebuggerProcessBuilder cleaning process : " + id);
                cleanOperation();
                System.out.println("killProcess => DebuggerProcessBuilder process : " + id + " killed and cleaned!");
            } catch (Exception e3) {
                e3.printStackTrace();
                System.err.println("killProcess => Error while cleaning DebuggerProcess " + id + " : " + e3.getMessage());
            }
        });
        cleaningThread.start();
    }

    /**
     * logic to execute when user stops the app
     */
    public static void executeShutdownHook() {
        IS_SHUTTING_DOWN = true;
        synchronized (listActiveDebuggerProcess) {
            System.out.println("DebuggerProcessBuilder ShutdownHook => starting shutdown hook...");
            for (DebuggerProcessBuilder process : listActiveDebuggerProcess) {
                process.killProcess();
            }
            System.out.println("DebuggerProcessBuilder ShutdownHook => shutdown hook DONE!");
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
            System.out.println("onWebSocketClosed => DebuggerProcessBuilder Executing onWebSocketClosed...");
            List<DebuggerProcessBuilder> elementsToRemove = new ArrayList<>();
            for (DebuggerProcessBuilder debProcess : listActiveDebuggerProcess) {
                if (debProcess.getUserUuid().equals(userUuid)) {
                    System.out.println("onWebSocketClosed => DebuggerProcessBuilder Killing process attached to userUuid : " + userUuid);
                    debProcess.killProcess();
                    elementsToRemove.add(debProcess);
                }
            }
            listActiveDebuggerProcess.removeAll(elementsToRemove);
        }
    }

    /**
     * Cancel a process by its Id, this action is performed on user's demand, when he clicks on the cancel button
     *
     * @param processId DebuggerProcessBuilder id
     */
    public static void cancelRunningProcess(String processId, String userUuid) throws IllegalAccessException {
        if (!IS_SHUTTING_DOWN) {
            for (DebuggerProcessBuilder process : listActiveDebuggerProcess) {
                if (process.getId().equals(processId)) {
                    if (process.getUserUuid().equals(userUuid)) {
                        System.out.println("cancelRunningProcess => DebuggerProcessBuilder Canceling running process : " + processId);
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
    public static List<DebuggerProcessBuilder> getDebuggerRunningProcesses(String userUuid) {
        synchronized (listActiveDebuggerProcess) {
            List<DebuggerProcessBuilder> debuggerRunningProcesses = new ArrayList<>();
            if (!IS_SHUTTING_DOWN) {
                for (DebuggerProcessBuilder process : listActiveDebuggerProcess) {
                    if (process.getUserUuid().equals(userUuid)) {
                        debuggerRunningProcesses.add(process);
                    }
                }
            }
            return debuggerRunningProcesses;
        }
    }


    /**
     * Stp all logcat adb processes started by user userUuid
     *
     * @param userUuid user identifier
     */
    public static void stopAllUserLogcatProcesses(String userUuid) {
        List<DebuggerProcessBuilder> copy = new ArrayList<>();
        copy.addAll(listActiveDebuggerProcess);
        for (DebuggerProcessBuilder debProcess : copy) {
            if (debProcess.getUserUuid().equals(userUuid)) {
                debProcess.killProcess();
            }
        }
    }


    /**
     * This method return the number og current executing processes.
     * Used to warn the user when he tries to exit the app while there are more than one running process
     *
     * @return number of current executing processes
     */
    public static int getDebuggerRunningProcessCount() {
        synchronized (listActiveDebuggerProcess) {
            return listActiveDebuggerProcess.size();
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!DebuggerProcessBuilder.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final DebuggerProcessBuilder other = (DebuggerProcessBuilder) obj;
        return (this.id.equals(other.id));
    }
}

package com.ninjaflip.androidrevenge.core.apktool.socketstreamer;

import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.enums.EnumerationScrapper;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;

import java.io.IOException;

/**
 * Created by Solitario on 20/06/2017.
 *
 * WWrapper for websocket messaging protocol
 */

public class SocketMessagingProtocol {
    private final static Logger LOGGER = Logger.getLogger(SocketMessagingProtocol.class);
    private static SocketMessagingProtocol INSTANCE;


    private SocketMessagingProtocol() {

    }

    public static SocketMessagingProtocol getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SocketMessagingProtocol();
        }
        return INSTANCE;
    }


    /**
     *
     * Output a log event to websocket
     * @param sessionSocket the corresponding websocket
     * @param logEvent log messsage
     * @param logType the type of log message, if GENERAL then it will be displayed in the virtual terminal, else
     *                it will be displayed on a specific area
     */
    public void sendLogEvent(Session sessionSocket, String logEvent,
                             EnumerationApkTool.EnumLogType logType) {
        JSONObject data = new JSONObject();
        data.put("dataType", "log-event");
        data.put("dataMsg", logEvent);
        data.put("dataLogType", logType.name());

        send(sessionSocket, data.toJSONString());
    }

    /**
     * Output a log event to websocket
     * @param sessionSocket the corresponding websocket
     * @param logEvent log messsage
     */
    public void sendLogEvent(Session sessionSocket, String logEvent) {
        sendLogEvent(sessionSocket, logEvent, EnumerationApkTool.EnumLogType.GENERAL);
    }

    /**
     * Notify process state changed
     * @param sessionSocket the corresponding websocket
     * @param processId the process id
     * @param processState EnumerationApkTool of process state
     * @param processType EnumerationApkTool of process type
     */
    public void sendProcessState(Session sessionSocket, String processId,
                                 EnumerationApkTool.EnumProcessState processState,
                                 EnumerationApkTool.EnumProcessType processType){
        JSONObject data = new JSONObject();
        data.put("dataType", "process-state");
        data.put("dataProcessId", processId);
        data.put("dataState", processState.name());
        data.put("dataProcessType", processType.name());

        send(sessionSocket, data.toJSONString());
    }

    public void sendProcessState(Session sessionSocket, String processId,
                                 EnumerationApkTool.EnumProcessState processState,
                                 EnumerationApkTool.EnumProcessType processType, String message) {
        JSONObject data = new JSONObject();
        data.put("dataType", "process-state");
        data.put("dataProcessId", processId);
        data.put("dataState", processState.name());
        data.put("dataProcessType", processType.name());
        data.put("dataMessage", message);

        send(sessionSocket, data.toJSONString());
    }



    /**
     * Sends ready apk info to client
     * @param sessionSocket the corresponding websocket
     * @param url url to apk file
     * @param buildType debug or release
     * @param packageName app package name
     * @param iconBase64 app icon image as base64 string
     */
    public void sendApkReadyInfo(Session sessionSocket, String url,
                                 EnumerationApkTool.EnumBuildType buildType, String packageName, String iconBase64,
                                 String size_in_MB, String filePath) {
        JSONObject data = new JSONObject();
        data.put("dataType", "apk-ready-url");
        data.put("dataUrl", url);
        data.put("dataBuildType", buildType.name());
        data.put("dataPackageName", packageName);
        data.put("dataIconBase64", iconBase64);
        data.put("size_in_MB", size_in_MB);
        data.put("dataFilePath", filePath);

        send(sessionSocket, data.toJSONString());
    }

    /**
     * Output a log event to weebsocket
     * @param sessionSocket the corresponding websocket
     * @param totalSize total size of all project that belongs to a certain user (current sessionSocket owner)
     */
    public void sendUserProjectsTotalSize(Session sessionSocket, long totalSize) {
        JSONObject data = new JSONObject();
        data.put("dataType", "user-projects-total-size");
        data.put("dataSize", totalSize);
        send(sessionSocket, data.toJSONString());
    }

    /**
     *
     * Output a log event to websocket for scrapper UI
     * @param sessionSocket the corresponding websocket
     * @param logEvent log messsage
     * @param logType the type of log message, if COUNTRY_PROGRESS then it will update the country progress, else
     *                it will be displayed on a specific area
     */
    public void sendScrapperLogEvent(Session sessionSocket, String logEvent,
                                     EnumerationScrapper.EnumScrapperLogType logType) {
        JSONObject data = new JSONObject();
        data.put("dataType", "scrapper-event");
        data.put("dataMsg", logEvent);
        data.put("dataLogType", logType.name());

        send(sessionSocket, data.toJSONString());
    }

    /**
     * Notify scrapper process state changed
     * @param sessionSocket the corresponding websocket
     * @param processState process state
     * @param processType process type
     */
    public void sendScrapperProcessState(Session sessionSocket, String processId, EnumerationScrapper.EnumProcessType processType,
                                         EnumerationScrapper.EnumProcessState processState) {
        JSONObject data = new JSONObject();
        data.put("dataType", "scrapper-proc-state");
        data.put("dataProcessId", processId);
        data.put("dataState", processState.name());
        data.put("dataProcessType", processType.name());

        send(sessionSocket, data.toJSONString());
    }

    public void sendScrapperCompletedWithResultId(Session sessionSocket, String processId, String resultUuid,
                                                  EnumerationScrapper.EnumProcessType processType) throws IOException {
        JSONObject data = new JSONObject();
        data.put("dataType", "scrapper-proc-state");
        data.put("dataProcessId", processId);
        data.put("dataResultUuid", resultUuid);
        data.put("dataState", EnumerationScrapper.EnumProcessState.COMPLETED);
        data.put("dataProcessType", processType.name());

        send(sessionSocket, data.toJSONString());
    }

    public void sendScrapperProcessState(Session sessionSocket, String processId, EnumerationScrapper.EnumProcessType processType,
                                         EnumerationScrapper.EnumProcessState processState, String message) {
        JSONObject data = new JSONObject();
        data.put("dataType", "scrapper-proc-state");
        data.put("dataProcessId", processId);
        data.put("dataState", processState.name());
        data.put("dataProcessType", processType.name());
        data.put("dataMessage", message);

        send(sessionSocket, data.toJSONString());
    }

    private void send(Session sessionSocket, String data){
        if (!Thread.interrupted()) {
            try {
                sessionSocket.getRemote().sendString(data);
            } catch (Exception e) {
                LOGGER.warn(e.getMessage());
            }
        }
    }

}

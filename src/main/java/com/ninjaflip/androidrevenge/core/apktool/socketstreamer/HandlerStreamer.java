package com.ninjaflip.androidrevenge.core.apktool.socketstreamer;

import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by Solitario on 22/05/2017.
 * <p>
 * This logging handler streams the output of a Logger into a web-socket
 * We use it to send execution output directly to browser
 */
public class HandlerStreamer extends Handler {
    private Session webSocketSession;
    private EnumerationApkTool.EnumLogType logType;

    public HandlerStreamer(Session webSocketSession) {
        super();
        this.webSocketSession = webSocketSession;
        this.logType = EnumerationApkTool.EnumLogType.GENERAL;
    }

    public HandlerStreamer(Session webSocketSession, EnumerationApkTool.EnumLogType logType) {
        super();
        this.webSocketSession = webSocketSession;
        this.logType = logType;
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null || record.getMessage() == null) {
            return;
        }

        SocketMessagingProtocol.getInstance()
                .sendLogEvent(webSocketSession, StringUtil.escape(record.getMessage()), logType);

    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}

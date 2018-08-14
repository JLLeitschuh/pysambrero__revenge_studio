package com.ninjaflip.androidrevenge.core.apktool.socketstreamer;

import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;

/**
 * Created by Solitario on 18/06/2017.
 * <p>
 * This logging handler streams the output of a Logger into a web-socket
 * We use it to send execution output directly to browser
 */
public class AppenderStreamer implements Appender {
    private Session webSocketSession;
    private EnumerationApkTool.EnumLogType logType;

    public AppenderStreamer(Session webSocketSession) {
        super();
        this.webSocketSession = webSocketSession;
        this.logType = EnumerationApkTool.EnumLogType.GENERAL;
    }

    public AppenderStreamer(Session webSocketSession, EnumerationApkTool.EnumLogType logType) {
        super();
        this.webSocketSession = webSocketSession;
        this.logType = logType;
    }

    @Override
    public void addFilter(Filter filter) {
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    @Override
    public void clearFilters() {

    }

    @Override
    public void close() {

    }

    @Override
    public void doAppend(LoggingEvent loggingEvent) {
        if (loggingEvent == null || loggingEvent.getRenderedMessage() == null) {
            return;
        }
        SocketMessagingProtocol.getInstance()
                .sendLogEvent(webSocketSession, StringUtil.escape(loggingEvent.getRenderedMessage()), logType);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {

    }

    @Override
    public ErrorHandler getErrorHandler() {
        return null;
    }

    @Override
    public void setLayout(Layout layout) {

    }

    @Override
    public Layout getLayout() {
        return null;
    }

    @Override
    public void setName(String s) {

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}

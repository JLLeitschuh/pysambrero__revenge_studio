package com.ninjaflip.androidrevenge.core.apktool.socketstreamer;

import com.ninjaflip.androidrevenge.enums.EnumerationApkTool;
import com.ninjaflip.androidrevenge.utils.StringUtil;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by Solitario on 26/10/2017.
 * <p>
 * A PrinterStream that can be used to redirect standard outputs to a websocket
 */
public class SocketPrintStream extends PrintStream {
    private Session webSocketSession;
    private EnumerationApkTool.EnumLogType logType;
    private PrintStream originalPrintStream = null;

    public SocketPrintStream(PrintStream out, Session webSocketSession) {
        super(out);
        this.originalPrintStream = out;
        this.webSocketSession = webSocketSession;
        this.logType = EnumerationApkTool.EnumLogType.GENERAL;
    }

    public SocketPrintStream(PrintStream out, Session webSocketSession, EnumerationApkTool.EnumLogType logType) {
        super(out);
        this.webSocketSession = webSocketSession;
        this.logType = logType;
    }

    public void print(final String str) {
        //originalPrintStream.print(str);
        //super.print(str);
        if (str == null) {
            return;
        }

        SocketMessagingProtocol.getInstance()
                .sendLogEvent(webSocketSession, StringUtil.escape(str), logType);

    }

    public void println(final String str) {
        //super.println(str);
        //originalPrintStream.println(str);
        if (str == null) {
            return;
        }

        SocketMessagingProtocol.getInstance()
                .sendLogEvent(webSocketSession, StringUtil.escape(str), logType);

    }
}

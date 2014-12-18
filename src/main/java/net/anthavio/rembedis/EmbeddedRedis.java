package net.anthavio.rembedis;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.anthavio.process.ExternalProcess;
import net.anthavio.process.StartupCheck.SysoutRegexCheck;

/**
 * ./redis-server /etc/redis/6379.conf
 * ./redis-server --port 7777
 * ./redis-server --port 7777 --slaveof 127.0.0.1 8888
 * ./redis-server /etc/myredis.conf --loglevel verbose
 * 
 * @author mvanek
 */
public class EmbeddedRedis implements Closeable {

    private final int port;

    private final List<String> command;

    private ExternalProcess process;

    private OutputStream sysOutStream;

    public EmbeddedRedis() {
        this(getDynamicPort());
    }

    public EmbeddedRedis(int port) {
        this(Arrays.asList("--port", String.valueOf(port)));
    }

    public EmbeddedRedis(List<String> params) {
        File binary = Rembedis.unpack();
        //make copy 
        command = new ArrayList<String>(params);
        command.add(0, binary.getAbsolutePath());
        int portIdx = command.indexOf("--port");
        if (portIdx != -1) {
            //some checks maybe...
            port = Integer.parseInt(command.get(portIdx + 1));
        } else {
            port = getDynamicPort();
            command.add("--port");
            command.add(String.valueOf(port));
        }
    }

    public void setSysOut(OutputStream sysOutStream) {
        this.sysOutStream = sysOutStream;
    }

    /**
     * Start Redis
     * Convenience method with 2 seconds timeout
     */
    public void start() {
        start(2000);
    }

    /**
     * Start Redis
     */
    public void start(int timeoutMs) {
        if (isRunning()) {
            throw new IllegalStateException("Redis already running. Port " + port);
        }
        process = ExternalProcess.Builder().setCommand(command).setRedirectStdErrToStdOut(true).setStdOutStream(sysOutStream)
                .setStartupCheck(new SysoutRegexCheck("The server is now ready to accept connections")).build();
        process.start(timeoutMs);
    }

    public int stop() {
        int exitValue = Integer.MIN_VALUE;
        if (process != null) {
            exitValue = process.stop();
            process = null;
        }
        return exitValue;
    }

    public boolean isRunning() {
        if (process != null) {
            return process.isRunning();
        }
        return false;
    }

    @Override
    public void close() {
        stop();
    }

    public int getPort() {
        return port;
    }

    private static int getDynamicPort() {
        try {
            ServerSocket server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;
        } catch (IOException iox) {
            throw new IllegalStateException(iox);
        }
    }

}

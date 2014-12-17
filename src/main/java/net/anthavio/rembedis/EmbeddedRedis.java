package net.anthavio.rembedis;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author mvanek
 * ./redis-server /etc/redis/6379.conf
 * ./redis-server --port 7777
 * ./redis-server --port 7777 --slaveof 127.0.0.1 8888
 * ./redis-server /etc/myredis.conf --loglevel verbose
 */
public class EmbeddedRedis {

    private final int port;

    private final List<String> command;

    public EmbeddedRedis() {
        this(getDynamicPort());
    }

    public EmbeddedRedis(int port) {
        this(Arrays.asList("--port", String.valueOf(port)));
    }

    public EmbeddedRedis(List<String> params) {
        File binary = Rembedis.unpack();
        //make copy 
        this.command = new ArrayList<String>(params);
        params.add(0, binary.getAbsolutePath());
        int portIdx = params.indexOf("--port");
        if (portIdx != -1) {
            //some checks maybe...
            port = Integer.parseInt(params.get(portIdx + 1));
        } else {
            port = getDynamicPort();
            params.add("--port");
            params.add(String.valueOf(port));
        }
    }

    public void start(int timeoutMs) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        GuardedProcess consumer = new GuardedProcess(process, "The server is now ready to accept connections");
        consumer.start(3000);
    }

    public void stop() {
        Runtime.getRuntime().removeShutdownHook(guarded.getShutdownHook());
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

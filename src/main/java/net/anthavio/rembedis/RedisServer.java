/**
 * Copyright © 2014, Anthavio
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */
package net.anthavio.rembedis;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.anthavio.embed.Unpacker;
import net.anthavio.process.Bit;
import net.anthavio.process.Os;
import net.anthavio.process.OsProcess;
import net.anthavio.process.StartupCheck.SysoutRegexCheck;

/**
 * ./redis-server /etc/redis/6379.conf
 * ./redis-server --port 7777
 * ./redis-server --port 7777 --slaveof 127.0.0.1 8888
 * ./redis-server /etc/myredis.conf --loglevel verbose
 * 
 * @author mvanek
 */
public class RedisServer implements Closeable {

    public static RedisBuilder Builder() {
        return new RedisBuilder();
    }

    public static final String REDIS_VERSION = "2.8.9";

    private static final Unpacker UNPACKER = Unpacker.Builder().//
            setName("redis").setVersion(REDIS_VERSION).//
            addBinary(Os.WINDOWS, Bit.B64, "windows/redis-server.exe").//
            addBinary(Os.MACOS, Bit.B64, "macosx/redis-server").//
            addBinary(Os.LINUX, Bit.B32, "linux86/redis-server").//
            addBinary(Os.LINUX, Bit.B64, "linux64/redis-server").build();

    public static File unpack() {
        return UNPACKER.unpack();
    }

    private final int port;

    private final List<String> command;

    private OsProcess process;

    private OutputStream sysOutStream;

    public RedisServer() {
        this(getDynamicPort());
    }

    public RedisServer(int port) {
        this(Arrays.asList("--port", String.valueOf(port)));
    }

    public RedisServer(List<String> params) {
        this(UNPACKER.unpack(), params);
    }

    public RedisServer(File executable, List<String> params) {
        this(executable, params, null);
    }

    public RedisServer(File executable, List<String> params, OutputStream sysOutStream) {
        if (executable.exists() == false) {
            throw new IllegalArgumentException("Redis executable does not exist: " + executable);
        }
        command = new ArrayList<String>(params);
        command.add(0, executable.getAbsolutePath());
        int portIdx = command.indexOf("--port");
        if (portIdx != -1) {
            //some checks maybe...
            port = Integer.parseInt(command.get(portIdx + 1));
        } else {
            port = getDynamicPort();
            command.add("--port");
            command.add(String.valueOf(port));
        }

        this.sysOutStream = sysOutStream; //nullable

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
        process = OsProcess.Builder().setCommand(command).setRedirectStdErrToStdOut(true).setStdOutStream(sysOutStream)
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

    /**
     * Useful when port is dynamicaly allocated or inside config file
     */
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

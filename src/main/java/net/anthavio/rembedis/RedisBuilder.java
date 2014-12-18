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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;

import net.anthavio.process.StartupException;

/**
 * 
 * @author mvanek
 *
 */
public class RedisBuilder {

    private static String NEW_LINE = System.getProperty("line.separator");

    private File redisBinary;

    private File configFile;

    private StringBuilder configBuilder;

    private Integer port;

    private String slaveofHost;

    private int slaveofPort;

    private String loglevel;

    private OutputStream sysOutStream;

    public RedisBuilder port(int port) {
        this.port = port;
        return this;
    }

    public RedisBuilder slaveof(String slaveofHost, int slaveofPort) {
        this.slaveofHost = slaveofHost;
        this.slaveofPort = slaveofPort;
        return this;
    }

    public RedisBuilder redisBinary(File redisBinary) {
        if (redisBinary.exists() == false || redisBinary.canExecute()) {
            throw new IllegalStateException("Redis binary does not exist or is not executable " + redisBinary);
        }
        this.redisBinary = redisBinary;
        return this;
    }

    public RedisBuilder configFile(File configFile) {
        if (configBuilder != null) {
            throw new IllegalStateException("Configuration is already built by lines");
        }
        if (configFile.exists() == false) {
            throw new IllegalArgumentException("Config file does not exist: " + configFile);
        }
        return this;
    }

    public RedisBuilder configFile(String configFile) {
        return configFile(new File(configFile));
    }

    public RedisBuilder configLine(String configLine) {
        if (configFile != null) {
            throw new IllegalStateException("Configuration is already built by file");
        }
        if (configBuilder == null) {
            configBuilder = new StringBuilder();
        }
        configBuilder.append(configLine).append(NEW_LINE);
        return this;
    }

    /**
     * Use System.out to see redis output in console or any other stream
     */
    public RedisBuilder setStdOutStream(OutputStream sysOutStream) {
        this.sysOutStream = sysOutStream;
        return this;
    }

    public RedisServer build() {
        ArrayList<String> line = new ArrayList<String>();
        if (configBuilder != null) {
            try {
                configFile = File.createTempFile("redis-", ".conf");
                configFile.deleteOnExit();
                write(configBuilder, configFile);
            } catch (IOException iox) {
                throw new StartupException("Cannot create temporary redis config file", iox);
            }

        }
        if (configFile != null) {
            line.add(configFile.getAbsolutePath());
        }
        if (port != null) {
            line.add("--port");
            line.add(String.valueOf(port));
        }
        if (slaveofHost != null) {
            line.add("--slaveof");
            line.add(slaveofHost);
            line.add(String.valueOf(slaveofPort));

        }
        if (loglevel != null) {
            line.add("--loglevel");
            line.add(loglevel);
        }
        if (redisBinary == null) {
            redisBinary = Rembedis.unpack();
        }
        return new RedisServer(redisBinary, line, sysOutStream);
    }

    public RedisServer start() {
        return start(2000);
    }

    public RedisServer start(int timeoutMs) {
        RedisServer redis = build();
        redis.start(timeoutMs);
        return redis;
    }

    private void write(StringBuilder configBuilder, File configFile) throws IOException {
        FileOutputStream stream = new FileOutputStream(configFile);
        try {
            OutputStreamWriter writer = new OutputStreamWriter(stream, Charset.forName("utf-8"));
            writer.write(configBuilder.toString());
            writer.flush();
            writer.close();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

}

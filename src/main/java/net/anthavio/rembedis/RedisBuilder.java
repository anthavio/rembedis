package net.anthavio.rembedis;

import java.util.ArrayList;

/**
 * 
 * @author mvanek
 *
 */
public class RedisBuilder {

    private String configFile;

    private Integer port;

    private String slaveofHost;

    private int slaveofPort;

    private String loglevel;

    public RedisBuilder port(int port) {
        this.port = port;
        return this;
    }

    public RedisBuilder slaveof(String slaveofHost, int slaveofPort) {
        this.slaveofHost = slaveofHost;
        this.slaveofPort = slaveofPort;
        return this;
    }

    public RedisBuilder configFile(String configFile) {
        this.configFile = configFile;
        return this;
    }

    public EmbeddedRedis build() {
        ArrayList<String> line = new ArrayList<String>();
        if (configFile != null) {
            line.add(configFile);
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
        return new EmbeddedRedis(line);
    }
}

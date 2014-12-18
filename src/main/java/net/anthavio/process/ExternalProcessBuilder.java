package net.anthavio.process;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.anthavio.process.ShutdownHook.DefaultShutdownHook;
import net.anthavio.process.StartupCheck.DefaultStartupCheck;

/**
 * 
 * @author mvanek
 *
 */
public class ExternalProcessBuilder {

    private List<String> command;

    private StartupCheck startupCheck = new DefaultStartupCheck();

    private ShutdownHook shutdownHook = new DefaultShutdownHook();

    private Map<String, String> environment;

    private File workingDirectory;

    private boolean redirectStdErrToStdOut = true;

    private OutputStream stdOutStream;

    public ExternalProcessBuilder() {
    }

    public ExternalProcessBuilder(String... command) {
        setCommand(command);
    }

    public ExternalProcessBuilder setCommand(String... command) {
        this.command = new ArrayList<String>(command.length);
        for (String arg : command) {
            this.command.add(arg);
        }
        return this;
    }

    public ExternalProcessBuilder setCommand(List<String> command) {
        this.command = command;
        return this;
    }

    public ExternalProcessBuilder setStartupCheck(StartupCheck startupCheck) {
        this.startupCheck = startupCheck;
        return this;
    }

    public ExternalProcessBuilder setShutdownHook(ShutdownHook shutdownHook) {
        this.shutdownHook = shutdownHook;
        return this;
    }

    public ExternalProcessBuilder addEnvironmentProperty(String name, String value) {
        if (environment == null) {
            environment = new HashMap<String, String>();
        }
        environment.put(name, value);
        return this;
    }

    public ExternalProcessBuilder setRedirectStdErrToStdOut(boolean redirectStdErrToStdOut) {
        this.redirectStdErrToStdOut = redirectStdErrToStdOut;
        return this;
    }

    public ExternalProcessBuilder setStdOutStream(OutputStream stdOutStream) {
        this.stdOutStream = stdOutStream;
        return this;
    }

    public ExternalProcessBuilder setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    public ExternalProcess build() {
        return new ExternalProcess(new ArrayList<String>(command), startupCheck, shutdownHook, workingDirectory, environment, redirectStdErrToStdOut, stdOutStream);
    }

    /**
     * Build and start shortcut
     */
    public ExternalProcess start(int timeoutMs) {
        ExternalProcess process = build();
        process.start(timeoutMs);
        return process;
    }

}

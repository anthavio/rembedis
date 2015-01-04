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
package net.anthavio.process;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.anthavio.process.Shutdown.DefaultShutdown;
import net.anthavio.process.StartupCheck.DefaultStartupCheck;

/**
 * 
 * @author mvanek
 *
 */
public class OsProcessBuilder {

    private List<String> command;

    private StartupCheck startupCheck = new DefaultStartupCheck();

    private Shutdown shutdown = new DefaultShutdown();

    private Map<String, String> environment;

    private File workingDirectory;

    private boolean redirectStdErrToStdOut = true;

    private OutputStream stdOutStream;

    public OsProcessBuilder() {
    }

    public OsProcessBuilder(String... command) {
        setCommand(command);
    }

    public OsProcessBuilder setCommand(String... command) {
        this.command = new ArrayList<String>(command.length);
        for (String arg : command) {
            this.command.add(arg);
        }
        return this;
    }

    public OsProcessBuilder setCommand(List<String> command) {
        this.command = command;
        return this;
    }

    public OsProcessBuilder setStartupCheck(StartupCheck startupCheck) {
        this.startupCheck = startupCheck;
        return this;
    }

    public OsProcessBuilder setShutdown(Shutdown shutdownHook) {
        this.shutdown = shutdownHook;
        return this;
    }

    public OsProcessBuilder addEnvironmentProperty(String name, String value) {
        if (environment == null) {
            environment = new HashMap<String, String>();
        }
        environment.put(name, value);
        return this;
    }

    public OsProcessBuilder setRedirectStdErrToStdOut(boolean redirectStdErrToStdOut) {
        this.redirectStdErrToStdOut = redirectStdErrToStdOut;
        return this;
    }

    public OsProcessBuilder setStdOutStream(OutputStream stdOutStream) {
        this.stdOutStream = stdOutStream;
        return this;
    }

    public OsProcessBuilder setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    public OsProcess build() {
        return new OsProcess(new ArrayList<String>(command), startupCheck, shutdown, workingDirectory, environment, redirectStdErrToStdOut, stdOutStream);
    }

    /**
     * Build and start shortcut
     */
    public OsProcess start(int timeoutMs) {
        OsProcess process = build();
        process.start(timeoutMs);
        return process;
    }

}

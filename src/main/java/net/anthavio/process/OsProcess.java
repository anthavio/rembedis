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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author mvanek
 *
 */
public class OsProcess implements Closeable {

    public static OsProcessBuilder Builder() {
        return new OsProcessBuilder();
    }

    public static OsProcessBuilder Builder(String... command) {
        return new OsProcessBuilder(command);
    }

    private final List<String> sysout = new ArrayList<String>();

    private final Object lock = new Object();

    private Thread readerThread;

    private Thread shutdownThread;

    private final List<String> command;
    private final StartupCheck startupCheck;
    private final File workingDirectory;
    private final Map<String, String> environment;
    private final boolean redirectStdErrToStdOut;
    private final OutputStream stdOutStream;

    private Process process;
    private final Shutdown shutdown;

    private boolean started = false;
    private Exception exception = null;

    public OsProcess(List<String> command, StartupCheck startupCheck, Shutdown shutdownHook, File workingDirectory, Map<String, String> environment,
            boolean redirectStdErrToStdOut, OutputStream stdOutStream) {

        if (command == null || command.size() == 0) {
            throw new IllegalArgumentException("Command is invalid: " + command);
        }
        this.command = command;

        if (startupCheck == null) {
            throw new IllegalArgumentException("Null startupCheck");
        }
        this.startupCheck = startupCheck;

        if (shutdownHook == null) {
            throw new IllegalArgumentException("Null shutdownHook");
        }
        this.shutdown = shutdownHook;

        if (workingDirectory != null && (workingDirectory.exists() == false || workingDirectory.isDirectory() == false)) {
            throw new IllegalArgumentException("Invalid working directory: " + workingDirectory);
        }
        this.workingDirectory = workingDirectory; //can be null;

        if (environment != null) {
            this.environment = new HashMap<String, String>();
            this.environment.putAll(environment);
        } else {
            this.environment = null;
        }

        this.redirectStdErrToStdOut = redirectStdErrToStdOut;
        this.stdOutStream = stdOutStream; //nullable
    }

    /**
     * Closeable contract...
     */
    @Override
    public void close() {
        stop();
    }

    public int stop() {
        int exitValue = Integer.MIN_VALUE;
        if (shutdownThread != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownThread);
            shutdownThread = null;
        }
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
        if (process != null) {
            process.destroy();
            try {
                exitValue = process.waitFor();
            } catch (InterruptedException ix) {
                //ignore...
            }
            process = null;
        }
        started = false;
        return exitValue;
    }

    public boolean isRunning() {
        if (started) {
            try {
                process.exitValue();
            } catch (IllegalThreadStateException x) {
                return true;
            }
        }
        return false;
    }

    public void start(int timeoutMs) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(redirectStdErrToStdOut);
        if (workingDirectory != null) {
            builder.directory(workingDirectory);
        }

        if (this.environment != null) {
            Map<String, String> environment = builder.environment();
            environment.putAll(this.environment);
        }
        try {
            process = builder.start();
        } catch (IOException iox) {
            throw new StartupException("Failed to start " + command.get(0), iox);
        }

        readerThread = new Thread(new SysoutReaderThread());
        readerThread.start();

        boolean timeouted = false;
        synchronized (lock) {
            try {
                lock.wait(timeoutMs);
                timeouted = true;
            } catch (InterruptedException ix) {
                //ignore wakeup 
            }
        }
        //readerThread.interrupt(); don't

        if (exception != null) {
            process.destroy();
            throw new StartupException("Failed to start " + command.get(0), exception);
        } else if (started == false) {
            process.destroy();
            String message;
            if (timeouted) {
                message = command.get(0) + " not started in " + timeoutMs + " ms\nSystem Out\n:" + sysout;
            } else {
                message = "Failed to start " + command.get(0) + "\nSystem Out\n:" + sysout;
            }
            throw new StartupException(message);
        }

        shutdownThread = new ShutdownHookThread(process, shutdown);
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    /**
     * Leak internal Process - use on your own risk!
     */
    public Process getProcess() {
        return process;
    }

    class SysoutReaderThread extends Thread {

        public SysoutReaderThread() {
            setDaemon(true);
            setName("sysout-reader-" + command.get(0));
        }

        @Override
        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (started == false) {
                        //capture and check only until found
                        sysout.add(line);
                        if (startupCheck.isStarted(process, sysout)) {
                            started = true;
                            synchronized (lock) {
                                lock.notifyAll(); //poke timeouter
                            }
                            //sysout.clear(); //empty it as not needed anymore?
                        }
                    }
                    //Do not leave - Good practice of emptying process stdout...  

                    if (stdOutStream != null) {
                        stdOutStream.write(line.getBytes());
                        stdOutStream.write('\n');
                        stdOutStream.flush();
                    }
                }
            } catch (Exception x) {
                exception = x;
            }
        }
    }

    static class ShutdownHookThread extends Thread {

        private final Process process;
        private final Shutdown hook;

        public ShutdownHookThread(Process process, Shutdown hook) {
            setName("ps-destroyer-" + process);
            this.process = process;
            this.hook = hook;

        }

        @Override
        public void run() {
            try {
                hook.shutdown(process);
            } catch (Exception x) {
                //ignore...
            }
        }
    }

}

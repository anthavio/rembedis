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
public class ExternalProcess implements Closeable {

    public static ExternalProcessBuilder Builder() {
        return new ExternalProcessBuilder();
    }

    public static ExternalProcessBuilder Builder(String... command) {
        return new ExternalProcessBuilder(command);
    }

    private final List<String> sysout = new ArrayList<String>();

    private final Object lock = new Object();

    private Thread readerThread;

    private Thread shutdownThread;

    private final List<String> command;
    private final StartupCheck startupCheck;
    private final ShutdownHook shutdownHook;
    private final File workingDirectory;
    private final Map<String, String> environment;
    private final boolean redirectStdErrToStdOut;
    private final OutputStream stdOutStream;

    private Process process;

    private boolean started = false;
    private Exception exception = null;

    public ExternalProcess(List<String> command, StartupCheck startupCheck, ShutdownHook shutdownHook, File workingDirectory, Map<String, String> environment,
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
        this.shutdownHook = shutdownHook;

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

        shutdownThread = new ShutdownHookThread(process, shutdownHook);
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    public Process getProcess() {
        return process;
    }

    public Thread getShutdownHook() {
        return shutdownThread;
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
                        //capture only until found
                        sysout.add(line);
                        if (startupCheck.isStarted(process, sysout)) {
                            started = true;
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                            //sysout.clear(); //empty it as not heeded anymore
                        }
                    }
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
        private final ShutdownHook hook;

        public ShutdownHookThread(Process process, ShutdownHook hook) {
            setName("ps-destroyer-" + hook);
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

    public static interface LineEvaluator {

        public boolean evaluate(String line);
    }

    public static class ContainsEvaluator implements LineEvaluator {

        private String string;

        public ContainsEvaluator(String string) {
            this.string = string;
        }

        @Override
        public boolean evaluate(String line) {
            if (line.contains(string)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return string;
        }
    }
    /*
    class TimeoutThread extends Thread {

        private final int timeout;

        public TimeoutThread(int timeout) {
            this.timeout = timeout;
            setDaemon(true);
            setName("ps-timeout-" + process);
        }

        @Override
        public void run() {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                //ignore wakeup 
            }
            readerThread.interrupt();
        }
    }
    */

}

package net.anthavio.rembedis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

/**
 * 
 * @author mvanek
 *
 */
public class GuardedProcess {

    private final Process process;
    private final LineEvaluator evaluator;
    private final StringBuilder input = new StringBuilder();

    private Thread reader;
    private Thread timeouter;

    private Thread shutdownHook;

    private boolean completed = false;
    private Exception exception = null;

    public GuardedProcess(List<String> command, String successLine) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        this.process = builder.start();
        this.evaluator = new ContainsEvaluator(successLine);
    }

    public GuardedProcess(Process process, String successLine) {
        this.process = process;
        this.evaluator = new ContainsEvaluator(successLine);
    }

    public GuardedProcess(Process process, LineEvaluator evaluator) {
        this.process = process;
        this.evaluator = evaluator;
    }

    public void start(int timeout) {
        reader = new Thread(new ReaderThread());
        reader.start();

        timeouter = new TimeoutThread(timeout);
        timeouter.start();
        try {
            timeouter.join(timeout + 50);
        } catch (InterruptedException e) {
            //ignore
        }

        if (exception != null) {
            process.destroy();
            throw new IllegalStateException(exception);
        } else if (!completed) {
            process.destroy();
            throw new IllegalStateException("Not started in " + timeout + " ms\nExpected: " + evaluator + "\nActual: " + input.toString());
        }

        shutdownHook = new ProcesDestroyThread(process);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public Process getProcess() {
        return process;
    }

    public Thread getShutdownHook() {
        return shutdownHook;
    }

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
            reader.interrupt();
        }
    }

    class ReaderThread extends Thread {

        public ReaderThread() {
            setDaemon(true);
            setName("ps-reader-" + process);
        }

        @Override
        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while ((line = br.readLine()) != null) {
                    input.append(line).append('\n');
                    if (evaluator.evaluate(line)) {
                        completed = true;
                        break;
                    }
                }
            } catch (Exception x) {
                exception = x;
            } finally {
                if (timeouter != null) {
                    timeouter.interrupt();
                }
            }
        }
    }

    class ProcesDestroyThread extends Thread {

        private final Process process;

        public ProcesDestroyThread(Process process) {
            setName("ps-destroyer-" + process);
            this.process = process;
        }

        @Override
        public void run() {
            try {
                process.destroy();
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
}

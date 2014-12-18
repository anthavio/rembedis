package net.anthavio.process;

/**
 * 
 * @author mvanek
 *
 */
public interface ShutdownHook {

    public void shutdown(Process process);

    public static class DefaultShutdownHook implements ShutdownHook {

        @Override
        public void shutdown(Process process) {
            if (process != null) {
                try {
                    process.exitValue();
                    return; //process already ended
                } catch (IllegalThreadStateException itx) {
                    //ok - process is still running
                }
                process.destroy();
            }
        }

    }
}

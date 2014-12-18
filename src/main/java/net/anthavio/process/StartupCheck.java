package net.anthavio.process;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 
 * @author mvanek
 *
 */
public interface StartupCheck {

    public boolean isStarted(Process process, List<String> sysOutErr);

    /**
     * Checks only if process is still alive
     * 
     * @author mvanek
     *
     */
    public static class DefaultStartupCheck implements StartupCheck {

        @Override
        public boolean isStarted(Process process, List<String> sysOutErr) {
            try {
                int exitValue = process.exitValue();
                throw new StartupException("Process already ended with exit value " + exitValue);
            } catch (IllegalThreadStateException itx) {
                //ok - process is still running
            }
            return true;
        }
    }

    /**
     * Check for something in process standard output stream
     *  
     * @author mvanek
     */
    public static class SysoutRegexCheck extends DefaultStartupCheck {

        private final Pattern pattern;

        public SysoutRegexCheck(String regex) {
            this.pattern = Pattern.compile(regex);
        }

        @Override
        public boolean isStarted(Process process, List<String> sysOutErr) {
            // super - check process itself
            super.isStarted(process, sysOutErr);
            // seearch stdout
            for (String line : sysOutErr) {
                if (pattern.matcher(line).find()) {
                    return true;
                }
            }
            return false;
        }
    }
}

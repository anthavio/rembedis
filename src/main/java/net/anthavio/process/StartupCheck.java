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

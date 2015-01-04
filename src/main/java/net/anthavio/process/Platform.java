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
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * http://stackoverflow.com/questions/1803075/crowdsourcing-a-complete-list-of-common-java-system-properties-and-known-values
 * http://lopica.sourceforge.net/os.html
 * https://github.com/xerial/snappy-java/blob/develop/src/main/java/org/xerial/snappy/OSInfo.java
 * https://github.com/twall/jna/blob/master/src/com/sun/jna/Platform.java
 * 
 * @author mvanek
 *
 */
public class Platform {

    private final Os os;

    private final Arch arch;

    private final Bit osBits;

    private final Bit vmBits;

    public Platform(Os os, Arch arch, Bit osBits, Bit vmBits) {
        this.os = os;
        this.arch = arch;
        this.osBits = osBits;
        this.vmBits = vmBits;
    }

    public Os getOs() {
        return os;
    }

    public Arch getArch() {
        return arch;
    }

    public Bit getOsBits() {
        return osBits;
    }

    public Bit getVmBits() {
        return vmBits;
    }

    @Override
    public String toString() {
        return "OsArch [Os=" + os + ", Arch=" + arch + ", OsBit=" + osBits + ", JvmBit=" + vmBits + "]";
    }

    public static class DetectedPlatform extends Platform {

        private final String osName = System.getProperty("os.name");

        private final String osArch = System.getProperty("os.arch");

        private final String sysinfo; //uname or systeminfo on windows

        public DetectedPlatform(Os os, Arch arch, Bit osBits, Bit jvmBits, String uname) {
            super(os, arch, osBits, jvmBits);
            this.sysinfo = uname;
        }

        public String getOsName() {
            return osName;
        }

        public String getOsArch() {
            return osArch;
        }

        public String getSysinfo() {
            return sysinfo;
        }

        @Override
        public String toString() {
            return "DetectedOsArch [Os=" + getOs() + " (" + osName + "), Arch=" + getArch() + " (" + osArch + "), OsBit=" + getOsBits() + ", JvmBit=" + getVmBits() + ", SysInfo='"
                    + sysinfo + "' ]";
        }
    }

    private static DetectedPlatform detected;

    public static synchronized DetectedPlatform detect() {
        if (detected == null) {
            Os os = detectOs();
            Arch arch = detectArch();
            Bit osBits = detectOsBits(os);
            Bit jvmBits = detectJvmBits();
            String sysinfo;
            if (os == Os.WINDOWS) {
                sysinfo = execShell("systeminfo");
            } else {
                sysinfo = execShell("uname -a");
            }
            detected = new DetectedPlatform(os, arch, osBits, jvmBits, sysinfo);
        }
        return detected;
    }

    public static Os detectOs() {
        Os os;
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("linux")) {
            if (System.getProperty("java.vm.vendor", "").toLowerCase().contains("android")) {
                os = Os.ANDROID;
            } else {
                os = Os.LINUX;
            }

        } else if (osName.contains("windows")) {
            os = Os.WINDOWS;

        } else if (osName.contains("mac os") || osName.contains("darwin")) {
            os = Os.MACOS;

        } else if (osName.contains("sunos") || osName.contains("solaris")) {
            os = Os.SOLARIS;

        } else if (osName.contains("freebsd")) {
            os = Os.FREEBSD;

        } else if (osName.contains("opnebsd")) {
            os = Os.OPENBSD;

        } else if (osName.contains("netbsd")) {
            os = Os.NETBSD;

        } else if (osName.contains("hp-ux")) {
            os = Os.HPUX;

        } else if (osName.contains("aix")) {
            os = Os.AIX;
        } else {
            os = Os.UNSET;
        }
        return os;
    }

    /**
     * 
     */
    public static Bit detectOsBits(Os os) {
        //On 64bit MacOS X Java6 -d32 returns 32 here! Check others 
        if (System.getProperty("sun.arch.data.model", "").contains("64") || // Sun/Oracle JVM
                System.getProperty("os.arch", "").contains("64") || // x86_64
                System.getProperty("com.ibm.vm.bitmode", "").contains("64")) {
            return Bit.B64;
        } else if (os == Os.MACOS) {
            // Macosx: uname -m -> i386/x86_64
            String uname = execShell("uname -m");
            if (uname != null && uname.contains("64")) {
                return Bit.B64;
            }
        } else if (os == Os.WINDOWS && System.getenv("ProgramFiles(x86)") != null) {
            return Bit.B64;
            /*
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
            String realArch = arch.endsWith("64")
                  || wow64Arch != null && wow64Arch.endsWith("64")
                      ? "64" : "32";
             */
        }

        return Bit.B32;
        // http://en.wikipedia.org/wiki/Uname
        // https://projects.puppetlabs.com/issues/16506

        // armv7l is 32b
        // HP-UX: getconf KERNEL_BITS -> 32/64
        // AIX: getconf KERNEL_BITMODE -> 32/64
        // Solaris: isainfo -b -> 32/64
        // Linux: uname -m -> i386/x86_64

    }

    /**
     * Be aware that os.arch returns JVM bit related iformations rather than OS
     * And because 32bit JVM can be running on 64bit OS, mistake can happen easily
     */
    public static Bit detectJvmBits() {
        String osArch = System.getProperty("os.arch", "").toLowerCase();
        if (osArch.contains("x86_64") || osArch.contains("amd64")) {
            return Bit.B64;
        } else if (osArch.contains("ia64w") // HP-UX ia64 
                || osArch.contains("ps_risc2.0w") //HP-UX   PA-RISC 
                || osArch.contains("ppc64") //AIX   ppc64
        ) {
            return Bit.B64;
        }
        // HP-UX IA64 & 32bit java = os.arch ia64_32, ia64n
        return Bit.B32;
    }

    public static Arch detectArch() {
        String osArch = System.getProperty("os.arch", "").toLowerCase();
        if (osArch.contains("x86") || osArch.contains("i386") || osArch.contains("i686")) {
            return Arch.X86;
        } else if (osArch.contains("sparc")) {
            return Arch.SPARC;
        } else if (osArch.contains("power") || osArch.contains("ppc")) {
            return Arch.POWER;
        } else if (osArch.contains("pa-risc") || osArch.contains("pa_risc")) {
            return Arch.PARISC;
        } else if (osArch.contains("ia64")) {
            return Arch.IA64;
        } else if (osArch.contains("arm")) {
            return Arch.ARM;
        } else {
            return Arch.UNSET;
        }
    }

    public static String execShell(String command) {
        Process process;
        try {
            process = new ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = br.readLine();
            process.destroy();
            return line;
        } catch (IOException iox) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
        return null;

    }
}

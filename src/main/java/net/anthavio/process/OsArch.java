package net.anthavio.process;

/**
 * http://stackoverflow.com/questions/1803075/crowdsourcing-a-complete-list-of-common-java-system-properties-and-known-values
 * http://lopica.sourceforge.net/os.html
 * https://github.com/xerial/snappy-java/blob/develop/src/main/java/org/xerial/snappy/OSInfo.java
 * https://github.com/twall/jna/blob/master/src/com/sun/jna/Platform.java
 * 
 * @author mvanek
 *
 */
public class OsArch {

    private final Os os;

    private final Arch arch;

    private final Bit osBits;

    private final Bit jvmBits;

    public OsArch(Os os, Arch arch, Bit osBits, Bit jvmBits) {
        this.os = os;
        this.arch = arch;
        this.osBits = osBits;
        this.jvmBits = jvmBits;
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

    public Bit getJvmBits() {
        return jvmBits;
    }

    @Override
    public String toString() {
        return "OsArch [os=" + os + ", arch=" + arch + ", osBits=" + osBits + ", jvmBits=" + jvmBits + "]";
    }

    public static OsArch detect() {
        Os os = detectOs();
        Arch arch = detectArch();
        Bit osBits = detectOsBits();
        Bit jvmBits = detectJvmBits();
        return new OsArch(os, arch, osBits, jvmBits);
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
            os = Os.UNKNOWN;
        }
        return os;
    }

    /**
     * 
     */
    public static Bit detectOsBits() {
        if (System.getProperty("sun.arch.data.model", "").contains("64") || // Sun/Oracle JVM
                System.getProperty("os.arch", "").contains("64") || // x86_64
                System.getProperty("com.ibm.vm.bitmode", "").contains("64")) {
            return Bit.B64;
        } else {
            return Bit.B32;
        }

        // armv7l is 32b
        // HP-UX: getconf KERNEL_BITS
        // AIX: getconf KERNEL_BITMODE
        // Solaris: isainfo -v
        // Macosx: uname -m
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
        if (osArch.contains("x86") || osArch.contains("i386") || osArch.contains("i686")) { //TODO i386, i686
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
            return Arch.UNKNOWN;
        }

    }

}

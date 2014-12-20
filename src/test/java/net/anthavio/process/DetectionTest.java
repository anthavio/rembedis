package net.anthavio.process;

import net.anthavio.process.OsArch.DetectedOsArch;

public class DetectionTest {

    public static void main(String[] args) {
        DetectedOsArch osArch = OsArch.detect();
        //System.getProperties().list(System.out);
        System.out.println(osArch);
        //System.out.println(OsArch.execShell("uname -m"));
    }
}

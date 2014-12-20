package net.anthavio.process;

public class DetectionTest {

    public static void main(String[] args) {
        OsArch osArch = OsArch.detect();
        //System.getProperties().list(System.out);
        System.out.println(osArch);
    }
}

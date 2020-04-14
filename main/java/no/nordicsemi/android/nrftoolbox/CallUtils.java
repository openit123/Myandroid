package no.nordicsemi.android.nrftoolbox;

public class CallUtils {
    static {
        System.loadLibrary("use_ndk_build");
    }
    public static native String callSimpleInfo();
}

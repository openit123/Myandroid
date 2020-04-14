package no.nordicsemi.android.nrftoolbox;

public class CallUtils2 {
    public static native void callSimpleInfo2(int [] arraytest);
        static {
            System.loadLibrary("use_ndk_build2");
        }
}

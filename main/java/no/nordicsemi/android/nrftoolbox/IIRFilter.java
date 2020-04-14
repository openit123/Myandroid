package no.nordicsemi.android.nrftoolbox;

public class IIRFilter {
    public static native void callIIRFilterInfo(double [] arraytest);
        static {
            System.loadLibrary("use_ndk_buildIIR");
        }
}

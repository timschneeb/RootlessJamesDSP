package android.app;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(AppOpsManager.class)
public class AppOpsManagerHidden {
    public static int strOpToOp(String op) {
        throw new RuntimeException("Stub!");
    }

    public static int strDebugOpToOp(String op) {
        throw new RuntimeException("Stub!");
    }

    public static String modeToName(int mode) {
        throw new RuntimeException("Stub!");
    }
}

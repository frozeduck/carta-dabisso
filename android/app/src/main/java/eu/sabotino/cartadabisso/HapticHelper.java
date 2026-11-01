package eu.sabotino.cartadabisso;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class HapticHelper {
    private static Vibrator vibrator;
    private static boolean hapticEnabled = true;

    public static void init(Context context) {
        if (context != null) {
            try {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            } catch (Exception ignored) {
            }
        }
    }

    public static void setHapticEnabled(boolean enabled) {
        hapticEnabled = enabled;
    }

    public static boolean isHapticEnabled() {
        return hapticEnabled;
    }

    public static void toggleHaptic() {
        hapticEnabled = !hapticEnabled;
    }

    public static void lightTap() {
        if (!hapticEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        try {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) {
        }
    }

    public static void heavyImpact() {
        if (!hapticEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        try {
            vibrator.vibrate(VibrationEffect.createOneShot(65, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) {
        }
    }

    public static void victoryFanfare() {
        if (!hapticEnabled || vibrator == null || !vibrator.hasVibrator()) return;
        try {
            long[] timings = new long[]{0, 80, 50, 140};
            int[] amplitudes = new int[]{0, 180, 0, 255};
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
        } catch (Exception ignored) {
        }
    }
}

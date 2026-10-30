package eu.sabotino.cartadabisso;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioHelper {
    private static final int SAMPLE_RATE = 22050;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static volatile boolean soundEnabled = true;

    public static void toggleSound() {
        soundEnabled = !soundEnabled;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    private static void playBuffer(short[] buffer, float volume) {
        if (!soundEnabled || buffer == null || buffer.length == 0) return;
        executor.execute(() -> {
            try {
                AudioTrack track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(buffer.length * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();

                track.setVolume(volume);
                track.write(buffer, 0, buffer.length);
                track.play();

                long durationMs = (long) ((buffer.length * 1000.0) / SAMPLE_RATE) + 30;
                Thread.sleep(durationMs);
                track.stop();
                track.release();
            } catch (Exception ignored) {
            }
        });
    }

    // Strike / Attack impact
    public static void playHit() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.12f);
        short[] buf = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = 220.0 - 140.0 * (t / 0.12);
            double noise = (Math.random() * 2.0 - 1.0) * 0.3;
            double sine = Math.sin(2.0 * Math.PI * freq * t);
            double env = Math.exp(-32.0 * t);
            buf[i] = (short) ((sine * 0.7 + noise) * env * 30000);
        }
        playBuffer(buf, 0.9f);
    }

    // Heavy slam / Boss shockwave
    public static void playHeavyHit() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.28f);
        short[] buf = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = 95.0 - 45.0 * (t / 0.28);
            double noise = (Math.random() * 2.0 - 1.0) * 0.45;
            double sine = Math.sin(2.0 * Math.PI * freq * t);
            double env = Math.exp(-14.0 * t);
            buf[i] = (short) ((sine * 0.6 + noise * 0.4) * env * 32000);
        }
        playBuffer(buf, 1.0f);
    }

    // Tidal surge / Marea AoE
    public static void playWave() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.35f);
        short[] buf = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = 140.0 + 90.0 * Math.sin(Math.PI * t / 0.35);
            double noise = (Math.random() * 2.0 - 1.0) * 0.5;
            double sine = Math.sin(2.0 * Math.PI * freq * t);
            double env = Math.sin(Math.PI * (t / 0.35));
            buf[i] = (short) ((sine * 0.5 + noise * 0.5) * env * 28000);
        }
        playBuffer(buf, 0.85f);
    }

    // Dash / Bubble rush
    public static void playDash() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.16f);
        short[] buf = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = 360.0 - 200.0 * (t / 0.16);
            double sine = Math.sin(2.0 * Math.PI * freq * t);
            double noise = (Math.random() * 2.0 - 1.0) * 0.25;
            double env = Math.exp(-18.0 * t);
            buf[i] = (short) ((sine * 0.75 + noise) * env * 26000);
        }
        playBuffer(buf, 0.75f);
    }

    // Restorative Potion
    public static void playPotion() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.30f);
        short[] buf = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq = 440.0 + 330.0 * (t / 0.30);
            double sine = Math.sin(2.0 * Math.PI * freq * t);
            double env = Math.sin(Math.PI * (t / 0.30));
            buf[i] = (short) (sine * env * 25000);
        }
        playBuffer(buf, 0.8f);
    }

    // Floor loot drop or pickup chime (rarity tier 0..3)
    public static void playLoot(int rarity) {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.24f);
        short[] buf = new short[n];
        double baseFreq = 523.25; // C5
        if (rarity == 1) baseFreq = 659.25; // E5 (Magic)
        else if (rarity == 2) baseFreq = 783.99; // G5 (Rare)
        else if (rarity >= 3) baseFreq = 1046.50; // C6 (Abyssal)

        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double overtone = Math.sin(2.0 * Math.PI * (baseFreq * 1.5) * t) * 0.35;
            double sine = Math.sin(2.0 * Math.PI * baseFreq * t);
            double env = Math.exp(-12.0 * t);
            buf[i] = (short) ((sine + overtone) * env * 28000);
        }
        playBuffer(buf, 0.9f);
    }

    // UI Click or Inventory Equip
    public static void playClick() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.04f);
        short[] buf = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double sine = Math.sin(2.0 * Math.PI * 650.0 * t);
            double env = Math.exp(-60.0 * t);
            buf[i] = (short) (sine * env * 22000);
        }
        playBuffer(buf, 0.6f);
    }

    // Level up fanfare
    public static void playLevelUp() {
        if (!soundEnabled) return;
        int n = (int) (SAMPLE_RATE * 0.50f);
        short[] buf = new short[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / SAMPLE_RATE;
            double freq;
            if (t < 0.12) freq = 440.0;
            else if (t < 0.24) freq = 554.37;
            else if (t < 0.36) freq = 659.25;
            else freq = 880.0;
            double sine = Math.sin(2.0 * Math.PI * freq * t);
            double env = Math.exp(-4.0 * (t % 0.12) / 0.12) * Math.sin(Math.PI * t / 0.50);
            buf[i] = (short) (sine * env * 29000);
        }
        playBuffer(buf, 0.95f);
    }
}


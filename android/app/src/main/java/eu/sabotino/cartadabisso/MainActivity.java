package eu.sabotino.cartadabisso;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity implements Game.SaveDelegate {
    private GameView gameView;
    private long lastBack = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Estendi la superficie su tutto lo schermo (notch / cutout e telefoni ultra-wide come Galaxy Z Flip 7)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        applyImmersive();
        HapticHelper.init(this);

        gameView = new GameView(this);
        gameView.game.setSaveDelegate(this);
        setContentView(gameView);
    }

    private void applyImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyImmersive();
    }

    @Override
    protected void onPause() {
        AudioHelper.stopAmbient();
        if (gameView != null) {
            gameView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AudioHelper.startAmbient();
        if (gameView != null) {
            gameView.resume();
        }
    }

    @Override
    public int getInt(String key, int defVal) {
        return getSharedPreferences("carta_dabisso_save", MODE_PRIVATE).getInt(key, defVal);
    }

    @Override
    public void setInt(String key, int val) {
        getSharedPreferences("carta_dabisso_save", MODE_PRIVATE).edit().putInt(key, val).apply();
    }

    @Override
    public String getString(String key, String defVal) {
        return getSharedPreferences("carta_dabisso_save", MODE_PRIVATE).getString(key, defVal);
    }

    @Override
    public void setString(String key, String val) {
        getSharedPreferences("carta_dabisso_save", MODE_PRIVATE).edit().putString(key, val).apply();
    }

    @Override
    public boolean getBoolean(String key, boolean defVal) {
        return getSharedPreferences("carta_dabisso_save", MODE_PRIVATE).getBoolean(key, defVal);
    }

    @Override
    public void setBoolean(String key, boolean val) {
        getSharedPreferences("carta_dabisso_save", MODE_PRIVATE).edit().putBoolean(key, val).apply();
    }

    @Override
    public void onBackPressed() {
        if (gameView != null) {
            if (gameView.skillTreeOpen) {
                gameView.skillTreeOpen = false;
                return;
            }
            if (gameView.game.merchantShopOpen) {
                gameView.game.merchantShopOpen = false;
                return;
            }
            if (gameView.inventoryOpen) {
                gameView.toggleInventory();
                return;
            }
            if (!gameView.paused && !gameView.game.gameOver && !gameView.game.victory) {
                gameView.togglePause();
                Toast.makeText(this, "Gioco in pausa", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        long now = SystemClock.elapsedRealtime();
        if (lastBack > 0 && now - lastBack < 2000) {
            finish();
            return;
        }
        lastBack = now;
        Toast.makeText(this, "Premi ancora Indietro per uscire", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.pause();
        }
        super.onDestroy();
    }
}

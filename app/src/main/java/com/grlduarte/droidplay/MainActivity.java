package com.grlduarte.droidplay;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private AirPlaySurfaceView airPlaySurfaceView;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.waiting_connection);
        textView.setText("AirPlay discovery enabled");

        airPlaySurfaceView = (AirPlaySurfaceView) findViewById(R.id.airplay_stream);
        airPlaySurfaceView.init(visibilityCallback);
        Log.d(TAG, "AirPlay initialized");
    }

    private AirPlaySurfaceView.VisibilityCallback visibilityCallback =
            new AirPlaySurfaceView.VisibilityCallback() {
                @Override
                void showSurface() {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    airPlaySurfaceView.setVisibility(View.VISIBLE);
                                    textView.setVisibility(View.INVISIBLE);
                                }
                            });
                }

                void hideSurface() {
                    runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    textView.setVisibility(View.VISIBLE);
                                    airPlaySurfaceView.setVisibility(View.GONE);
                                }
                            });
                }
            };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        airPlaySurfaceView.stop();
    }
}

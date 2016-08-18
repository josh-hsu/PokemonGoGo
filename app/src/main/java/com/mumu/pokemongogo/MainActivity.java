package com.mumu.pokemongogo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PokemonGoGo";
    private Button mStartServiceButton;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        mStartServiceButton = (Button)findViewById(R.id.buttonStartService);
        mStartServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startChatHeadService();
            }
        });
    }

    private void startChatHeadService() {
        if (Build.VERSION.SDK_INT >= 23) {
            Toast.makeText(MainActivity.this, R.string.startup_permit_system_alarm, Toast.LENGTH_SHORT).show();
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 10);
            } else {
                Toast.makeText(MainActivity.this, R.string.headservice_how_to_stop, Toast.LENGTH_SHORT).show();
                startService(new Intent(mContext, HeadService.class));
            }
        } else {
            Toast.makeText(MainActivity.this, R.string.headservice_how_to_stop, Toast.LENGTH_SHORT).show();
            startService(new Intent(mContext, HeadService.class));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == 10) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(this)) {
                    // SYSTEM_ALERT_WINDOW permission not granted
                    Toast.makeText(MainActivity.this, R.string.startup_permit_system_alarm_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

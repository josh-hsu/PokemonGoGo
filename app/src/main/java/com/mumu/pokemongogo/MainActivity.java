/*
 * Copyright (C) 2016 The Josh Tool Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mumu.pokemongogo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PokemonGoGo";
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        Button mStartServiceButton;
        mStartServiceButton = (Button)findViewById(R.id.buttonStartService);
        mStartServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startChatHeadService();
            }
        });

        requestPermissions();

        Log.d(TAG, "EX: " + Environment.getExternalStorageDirectory().getPath());
    }

    private void startChatHeadService() {
        if (Build.VERSION.SDK_INT >= 23) {
            Toast.makeText(MainActivity.this, R.string.startup_permit_system_alarm, Toast.LENGTH_SHORT).show();
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 10);
                Log.d(TAG, "No permission for drawing on screen, prompt one.");

            } else {
                Toast.makeText(MainActivity.this, R.string.headservice_how_to_stop, Toast.LENGTH_SHORT).show();
                startService(new Intent(mContext, HeadService.class));
            }
        } else {
            Log.d(TAG, "Permission granted, starting service.");
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

    private void requestPermissions() {
        String[] perms = {"android.permission.WRITE_EXTERNAL_STORAGE"};
        int permsRequestCode = 200;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.d(TAG, "This is device software version above Marshmallow, requesting permission of external storage");
            requestPermissions(perms, permsRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case 200:
                boolean writeAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (writeAccepted) {
                    Log.d(TAG, "User gave us permission to write sdcard");
                } else {
                    Toast.makeText(this, "User didn't give us permission to write sdcard", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "User didn't give us permission to write sdcard");
                }
                break;
            default:
                Toast.makeText(this, "No handle permission grant", Toast.LENGTH_LONG).show();
        }
    }
}

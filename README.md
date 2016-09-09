# PokemonGoGo
Upper layer control app for location spoofing, this app must be signed using platform key. 

## Framework patching

```shell
diff --git a/services/core/java/com/android/server/JoshProperties.java b/services/core/java/com/android/server/JoshProperties.java
new file mode 100644
index 0000000..ad8508f
--- /dev/null
+++ b/services/core/java/com/android/server/JoshProperties.java
@@ -0,0 +1,38 @@
+/*
+ * Josh Property
+ */
+package com.android.server;
+
+import android.os.SystemProperties;
+import android.util.Log;
+
+public class JoshProperties {
+    private static JoshProperties mCurrent = null;
+    public static String mLat = "";
+    public static String mLng = "";
+    public static String mAcc = "";
+    public static String mAlt = "";
+    public static String mEnabled = "0";
+
+    public static JoshProperties getCurrent() {
+        if (mCurrent == null)
+            mCurrent = new JoshProperties();
+
+        return mCurrent;
+    }
+
+    // initialize default location
+    private JoshProperties() {
+        mLat = SystemProperties.get("persist.asus.fakegps.lat");
+        mLng = SystemProperties.get("persist.asus.fakegps.long");
+        mAcc = SystemProperties.get("persist.asus.fakegps.acc");
+        mAlt = SystemProperties.get("persist.asus.fakegps.alt");
+        Log.d("JoshProperties", "initialize with Lat " + mLat + ", Lng = " + mLng
+                + ", Acc = " + mAcc + ", Alt = " + mAlt);
+    }
+
+    public String toString() {
+        return "JoshProperties Enabled = " + mEnabled + "Lat = " + mLat + ", Lng = " + mLng
+                + ", Acc = " + mAcc + ", Alt = " + mAlt;
+    }
+}
diff --git a/services/core/java/com/android/server/am/ActivityManagerService.java b/services/core/java/com/android/server/am/ActivityManagerService.java
index 7eaa494..f4d58f8 100644
--- a/services/core/java/com/android/server/am/ActivityManagerService.java
+++ b/services/core/java/com/android/server/am/ActivityManagerService.java
@@ -98,6 +98,7 @@ import com.android.server.AppOpsService;
 import com.android.server.AttributeCache;
 import com.android.server.DeviceIdleController;
 import com.android.server.IntentResolver;
+import com.android.server.JoshProperties;
 import com.android.server.LocalServices;
 import com.android.server.ServiceThread;
 import com.android.server.SystemService;
@@ -1428,6 +1429,41 @@ public final class ActivityManagerService extends ActivityManagerNative
     final MainHandler mHandler;
     final UiHandler mUiHandler;
 
+    public boolean processIntentHacked(Intent intent) {
+        // Josh Heck +++
+        if (intent.getAction().equals("com.mumu.pokemongogo.action.SETPROP")) {
+            if (intent.hasExtra("lat")) {
+                JoshProperties.getCurrent().mLat = intent.getStringExtra("lat");
+                Log.d("JoshFake", "Receive lat: " + intent.getStringExtra("lat"));
+            }
+
+            if (intent.hasExtra("lng")) {
+                JoshProperties.getCurrent().mLng = intent.getStringExtra("lng");
+                Log.d("JoshFake", "Receive lng: " + intent.getStringExtra("lng"));
+            }
+
+            if (intent.hasExtra("acc")) {
+                JoshProperties.getCurrent().mAcc = intent.getStringExtra("acc");
+                Log.d("JoshFake", "Receive acc: " + intent.getStringExtra("acc"));
+            }
+
+            if (intent.hasExtra("alt")) {
+                JoshProperties.getCurrent().mAlt = intent.getStringExtra("alt");
+                Log.d("JoshFake", "Receive alt: " + intent.getStringExtra("alt"));
+            }
+
+            if (intent.hasExtra("enable")) {
+                JoshProperties.getCurrent().mEnabled = intent.getStringExtra("enable");
+                Log.d("JoshFake", "Receive enable: " + intent.getStringExtra("enable"));
+            }
+
+            return true;
+        }
+
+        return false;
+        // Josh Heck ---
+    }
+
     final class UiHandler extends Handler {
         public UiHandler() {
             super(com.android.server.UiThread.get().getLooper(), null, true);
@@ -16583,6 +16619,9 @@ public final class ActivityManagerService extends ActivityManagerNative
             boolean ordered, boolean sticky, int callingPid, int callingUid, int userId) {
         intent = new Intent(intent);
 
+        if (processIntentHacked(intent))
+            return 0;
+
         // By default broadcasts do not go to stopped apps.
         intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
 
diff --git a/services/core/java/com/android/server/location/GpsLocationProvider.java b/services/core/java/com/android/server/location/GpsLocationProvider.java
index 06327db..87e2bfb 100644
--- a/services/core/java/com/android/server/location/GpsLocationProvider.java
+++ b/services/core/java/com/android/server/location/GpsLocationProvider.java
@@ -25,6 +25,8 @@ import com.android.internal.location.ProviderRequest;
 import com.android.internal.telephony.Phone;
 import com.android.internal.telephony.PhoneConstants;
 
+import com.android.server.JoshProperties;
+
 import android.app.AlarmManager;
 import android.app.AppOpsManager;
 import android.app.PendingIntent;
@@ -372,6 +374,38 @@ public class GpsLocationProvider implements LocationProviderInterface {
     // True if gps should be disabled (used to support battery saver mode in settings).
     private boolean mDisableGps = false;
 
+    // fake gps update lock
+    private static boolean mFakeReportLock = false;
+    // report gps thread
+    private ReportFakeGPSThread mRFGT = null;
+    // report gps thread status
+    private static boolean mFakeGPSThreadStart = false;
+
+    public class ReportFakeGPSThread extends Thread {
+
+        public void run() {
+            while (mFakeGPSThreadStart) {
+
+                if (isFakeLocationEnabled()) {
+                    int fake_flag = 1;
+                    fake_flag |= LOCATION_HAS_LAT_LONG | LOCATION_HAS_ACCURACY;
+                    Date fake_date = new Date();
+                    mFakeReportLock = true;
+                    reportLocation(fake_flag, getFakeLat(), getFakeLong(), getFakeAlt(), 0.3f, 0.0f, getFakeAcc(), fake_date.getTime());
+                } else {
+                    Log.d("JoshFake", "Fake location is not enabled, last info =" + JoshProperties.getCurrent().toString());
+                }
+
+                // report it in next 1 second
+                try {
+                    Thread.sleep(1000);
+                } catch (Exception e) {
+                    e.printStackTrace();
+                }
+            }
+        }
+    }
+
     /**
      * Properties loaded from PROPERTIES_FILE.
      * It must be accessed only inside {@link #mHandler}.
@@ -1397,20 +1431,86 @@ public class GpsLocationProvider implements LocationProviderInterface {
         return ((mEngineCapabilities & capability) != 0);
     }
 
+    /**
+     * fake location enable determine
+     */
+    private String getProperty(String property) {
+        return SystemProperties.get(property, "");
+    }
+
+    private boolean isFakeLocationEnabled() {
+        String asusFakeEnabled = JoshProperties.getCurrent().mEnabled;
+        if (asusFakeEnabled.equals("1")) {
+            return true;
+        } else {
+            return false;
+        }
+    }
+
+    private double getFakeLat() {
+        String propetyValue = JoshProperties.getCurrent().mLat;
+
+        if (!propetyValue.equals("")) {
+            return Double.parseDouble(propetyValue);
+        } else {
+            return 25.0335;
+        }
+    }
+
+    private double getFakeLong() {
+        String propetyValue = JoshProperties.getCurrent().mLng;
+
+        if (!propetyValue.equals("")) {
+            return Double.parseDouble(propetyValue);
+        } else {
+            return 121.5642;
+        }
+    }
+
+    private float getFakeAcc() {
+        String propetyValue = JoshProperties.getCurrent().mAcc;
+
+        if (!propetyValue.equals("")) {
+            return (float)Double.parseDouble(propetyValue);
+        } else {
+            return 6.94f;
+        }
+    }
+
+    private double getFakeAlt() {
+        String propetyValue = JoshProperties.getCurrent().mAlt;
+
+        if (!propetyValue.equals("")) {
+            return Double.parseDouble(propetyValue);
+        } else {
+            return 12.1;
+        }
+    }
 
     /**
      * called from native code to update our position.
      */
     private void reportLocation(int flags, double latitude, double longitude, double altitude,
             float speed, float bearing, float accuracy, long timestamp) {
-        if (VERBOSE) Log.v(TAG, "reportLocation lat: " + latitude + " long: " + longitude +
+        Log.w("JoshFake", "reportLocation lat: " + latitude + " long: " + longitude +
                 " timestamp: " + timestamp);
+        SystemProperties.set("persist.asus.fakegps.lat", ""+latitude);
+        SystemProperties.set("persist.asus.fakegps.long", ""+longitude);
+        SystemProperties.set("persist.asus.fakegps.acc", ""+accuracy);
+        SystemProperties.set("persist.asus.fakegps.alt", ""+altitude);
+
+        // If fake is enabled but sync flag is not enabled, that means this is actual report
+        if (isFakeLocationEnabled() && !mFakeReportLock) {
+            Log.w("JoshFake", "Not sync fake, return it");
+            return;
+        }
 
         synchronized (mLocation) {
             mLocationFlags = flags;
             if ((flags & LOCATION_HAS_LAT_LONG) == LOCATION_HAS_LAT_LONG) {
                 mLocation.setLatitude(latitude);
                 mLocation.setLongitude(longitude);
                 mLocation.setTime(timestamp);
                 // It would be nice to push the elapsed real-time timestamp
                 // further down the stack, but this is still useful
@@ -1447,6 +1547,11 @@ public class GpsLocationProvider implements LocationProviderInterface {
             }
         }
 
+        // unlock the fake report lock
+        if (isFakeLocationEnabled() && mFakeReportLock) {
+            mFakeReportLock = false;
+        }
+
         mLastFixTime = System.currentTimeMillis();
         // report time to first fix
@@ -1492,6 +1597,13 @@ public class GpsLocationProvider implements LocationProviderInterface {
      */
     private void reportStatus(int status) {
         if (DEBUG) Log.v(TAG, "reportStatus status: " + status);
+        Log.w("JoshFake", "Report status");
+
+        if (mRFGT == null) {
+            mRFGT = new ReportFakeGPSThread();
+            mFakeGPSThreadStart = true;
+            mRFGT.start();
+        }
 
         boolean wasNavigating = mNavigating;
         switch (status) {
```

/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.hopenrun.cordova.vuforia;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.Toast;
//import android.R;
//import com.example.hello.R;

import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.DeviceTracker;
import com.vuforia.FUSION_PROVIDER_TYPE;
import com.vuforia.ObjectTracker;
import com.vuforia.PositionalDeviceTracker;
import com.vuforia.State;
import com.vuforia.STORAGE_TYPE;
import com.vuforia.Trackable;
import com.vuforia.TrackableList;
import com.vuforia.Tracker;
import com.vuforia.TrackerManager;
import com.vuforia.Vuforia;

import com.hopenrun.cordova.vuforia.utils.LoadingDialogHandler;
import com.hopenrun.cordova.vuforia.utils.ApplicationGLView;
import com.hopenrun.cordova.vuforia.utils.Texture;

import com.hopenrun.cordova.vuforia.VuforiaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ImageTargets extends Activity implements ApplicationControl {

    private static final String LOGTAG = "ImageTargets";
    private static final String FILE_PROTOCOL = "file://";

    ApplicationSession vuforiaAppSession;

    private DataSet mCurrentDataset;
    private int mCurrentDatasetSelectionIndex = 0;
    private int mStartDatasetsIndex = 0;
    private int mDatasetsNumber = 0;
    private ArrayList<String> mDatasetStrings = new ArrayList<String>();

    // Our OpenGL view:
    private ApplicationGLView mGlView;

    // Our renderer:
    private ImageTargetRenderer mRenderer;

    private GestureDetector mGestureDetector;

    // The textures we will use for rendering:
    private Vector<Texture> mTextures;

    private boolean mSwitchDatasetAsap = false;
    private boolean mFlash = false;
    private boolean mContAutofocus = false;
    private boolean mDeviceTracker = false;

    private View mFlashOptionView;

    private RelativeLayout mUILayout;

    private ActionReceiver vuforiaActionReceiver;

    LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);

    // Alert Dialog used to display SDK errors
    private AlertDialog mErrorDialog;

    boolean mIsDroidDevice = false;

    // Array of target names
    String mTargets = null;

    // Vuforia license key
    String mLicenseKey;

    // Holds the camera configuration to use upon resuming
    private int mCamera = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT;
    private int mType = 0;

    private class ActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context ctx, Intent intent) {

            String receivedAction = intent.getExtras().getString(VuforiaPlugin.PLUGIN_ACTION);

            if (receivedAction.equals(VuforiaPlugin.DISMISS_ACTION)) {
                Vuforia.deinit();
                finish();
            } else if (receivedAction.equals(VuforiaPlugin.PAUSE_ACTION)) {
                doStopTrackers();
            } else if (receivedAction.equals(VuforiaPlugin.RESUME_ACTION)) {
                doStartTrackers();
            } else if (receivedAction.equals(VuforiaPlugin.SETMODE_ACTION)) {
                int type = intent.getIntExtra("SET_MODE", 0);
                setVuforiaType(type);
            } else if (receivedAction.equals(VuforiaPlugin.SETIMAGEPARAM_ACTION)) {
                String option = intent.getStringExtra("ACTION_DATA");
                setVuforiaImageParam(option);
            } else if (receivedAction.equals(VuforiaPlugin.UPDATEMODELPARAM_ACTION)) {
                String option = intent.getStringExtra("ACTION_DATA");
                updateVuforiaModelParam(option);
            } else if (receivedAction.equals(VuforiaPlugin.GETUSEERDEFTARGETSFRAME_ACTION)) {
                getUserDefinedTargetsFrameQuality();
            } else if (receivedAction.equals(VuforiaPlugin.SETUSEERDEFTARGETSMODELPARAM_ACTION)) {
                String option = intent.getStringExtra("ACTION_DATA");
                setUserDefinedTargetsModelParam(option);
            } else if (receivedAction.equals(VuforiaPlugin.CLEANUSERDEFTARGETSFRAME_ACTION)) {
                cleanUserDefinedTargetsFrameQuality();
            }
        }

    }

    // Called when the activity first starts or the user navigates back to an
    // activity.
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(LOGTAG, "onCreate");

        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Force Landscape
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //Grab a reference to our Intent so that we can get the extra data passed into it
        Intent intent = getIntent();

        //Get the vuoria license key that was passed into the plugin
        mLicenseKey = intent.getStringExtra("LICENSE_KEY");

        try {
            vuforiaAppSession = new ApplicationSession(this, mLicenseKey);
        } catch(Exception e) {
            Intent mIntent = new Intent();
            mIntent.putExtra("name", "VUFORIA ERROR");
            setResult(VuforiaPlugin.ERROR_RESULT, mIntent);
            finish();
        }

        mCamera = intent.getIntExtra("CAMERA_INDEX", 0);
        mType = intent.getIntExtra("TYPE_MODE", 0);

        startLoadingAnimation();

        vuforiaAppSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        mGestureDetector = new GestureDetector(this, new GestureListener(this));

        // Load any sample specific textures:
        mTextures = new Vector<Texture>();

        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith("droid");

    }

    // Process Single Tap event to trigger autofocus
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        // Used to set autofocus one second after a manual focus is triggered
        private final Handler autofocusHandler = new Handler();

        private WeakReference<ImageTargets> activityRef;

        private GestureListener(ImageTargets activity) {

            activityRef = new WeakReference<ImageTargets>(activity);

        }

        @Override
        public boolean onDown(MotionEvent e) {

            return true;

        }

        // Process Single Tap event to trigger autofocus
        @Override
        public boolean onSingleTapUp(MotionEvent e) {

            boolean result = CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);
            if (!result)
                Log.e("SingleTapUp", "Unable to trigger focus");

            // Generates a Handler to trigger continuous auto-focus
            // after 1 second
            autofocusHandler.postDelayed(new Runnable() {
                public void run() {
                    if (activityRef.get().mContAutofocus) {
                        final boolean autofocusResult = CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

                        if (!autofocusResult)
                            Log.e("SingleTapUp", "Unable to re-enable continuous auto-focus");
                    }
                }
            }, 1000L);

            return true;
        }

    }

    @Override
    protected void onStart() {

        if (vuforiaActionReceiver == null) {
            vuforiaActionReceiver = new ActionReceiver();
        }

        IntentFilter intentFilter = new IntentFilter(VuforiaPlugin.PLUGIN_ACTION);
        registerReceiver(vuforiaActionReceiver, intentFilter);

        Log.d(LOGTAG, "onStart");

        super.onStart();

    }

    @Override
    protected void onStop() {

        if (vuforiaActionReceiver != null) {
            unregisterReceiver(vuforiaActionReceiver);
        }

        Log.d(LOGTAG, "onStop");

        super.onStop();

    }

    // Called when the activity will start interacting with the user.
    @Override
    protected void onResume() {

        Log.d(LOGTAG, "onResume");

        super.onResume();

        // This is needed for some Droid devices to force landscape
        if (mIsDroidDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        try {
            vuforiaAppSession.resumeAR();
        } catch (ApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        // Resume the GL view:
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }

    }

    // Callback for configuration changes the activity handles itself
    @Override
    public void onConfigurationChanged(Configuration config) {

        Log.d(LOGTAG, "onConfigurationChanged");

        super.onConfigurationChanged(config);

        vuforiaAppSession.onConfigurationChanged();

    }

    // Called when the system is about to start resuming a previous activity.
    @Override
    protected void onPause() {

        Log.d(LOGTAG, "onPause");

        super.onPause();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        // Turn off the flash
        if (mFlashOptionView != null && mFlash) {
            // OnCheckedChangeListener is called upon changing the checked state
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                ((Switch) mFlashOptionView).setChecked(false);
            } else {
                ((CheckBox) mFlashOptionView).setChecked(false);
            }
        }

        try {
            vuforiaAppSession.pauseAR();
        } catch (ApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

    }

    // The final call you receive before your activity is destroyed.
    @Override
    protected void onDestroy() {

        Log.d(LOGTAG, "onDestroy");

        super.onDestroy();

        try {
            vuforiaAppSession.stopAR();
        } catch (ApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        // Unload texture:
        mTextures.clear();
        mTextures = null;

        System.gc();

    }

    // Initializes AR application components.
    private void initApplicationAR() {

        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mGlView = new ApplicationGLView(this);
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new ImageTargetRenderer(this, vuforiaAppSession, mTargets);
        mRenderer.updateTargetStrings(mTargets);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);

    }

    private void startLoadingAnimation() {

        // Get the project's package name and a reference to it's resources
        String package_name = getApplication().getPackageName();
        Resources resources = getApplication().getResources();

        LayoutInflater inflater = LayoutInflater.from(this);

        mUILayout = (RelativeLayout) inflater.inflate(resources.getIdentifier("camera_overlay", "layout", package_name), null, false);

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        // Gets a reference to the loading dialog
        loadingDialogHandler.mLoadingDialogContainer = mUILayout.findViewById(resources.getIdentifier("loading_indicator", "id", package_name));

        // Shows the loading indicator at start
        loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

        // Adds the inflated layout to the view
        addContentView(mUILayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    }

    // Methods to load and destroy tracking data.
    @Override
    public boolean doLoadTrackersData() {

        if (mDatasetStrings.isEmpty())
            return false;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset == null)
            mCurrentDataset = objectTracker.createDataSet();

        if (mCurrentDataset == null)
            return false;

        //Determine the storage type.
        int storage_type;
        String dataFile = mDatasetStrings.get(mCurrentDatasetSelectionIndex);

        if (dataFile.startsWith(FILE_PROTOCOL)) {
            storage_type = STORAGE_TYPE.STORAGE_ABSOLUTE;
            dataFile = dataFile.substring(FILE_PROTOCOL.length(), dataFile.length());
            mDatasetStrings.set(mCurrentDatasetSelectionIndex, dataFile);
            Log.d(LOGTAG, "Reading the absolute path: " + dataFile);
        } else {
            storage_type = STORAGE_TYPE.STORAGE_APPRESOURCE;
            Log.d(LOGTAG, "Reading the path " + dataFile + " from the assets folder.");
        }

        if (!mCurrentDataset.load(mDatasetStrings.get(mCurrentDatasetSelectionIndex), storage_type))
            return false;

        if (!objectTracker.activateDataSet(mCurrentDataset))
            return false;

        TrackableList trackableList = mCurrentDataset.getTrackables();
        for (Trackable trackable : trackableList) {
            String name = "Current Dataset : " + trackable.getName();
            trackable.setUserData(name);
            Log.d(LOGTAG, "UserData:Set the following user data " + trackable.getUserData());
        }

        return true;

    }

    @Override
    public boolean doUnloadTrackersData() {

        // Indicate if the trackers were unloaded correctly
        boolean result = true;

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker == null)
            return false;

        if (mCurrentDataset != null && mCurrentDataset.isActive()) {
            if (objectTracker.getActiveDataSet(0).equals(mCurrentDataset) && !objectTracker.deactivateDataSet(mCurrentDataset)) {
                result = false;
            } else if (!objectTracker.destroyDataSet(mCurrentDataset)) {
                result = false;
            }

            mCurrentDataset = null;
        }

        return result;
    }

    // This callback is called on Vuforia resume
    @Override
    public void onVuforiaResumed() {

        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }

    }

    // This callback is called once Vuforia has been started
    @Override
    public void onVuforiaStarted() {

        mRenderer.updateRenderingPrimitives();
        mRenderer.updateConfiguration();

        if (mContAutofocus) {
            if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO)) {
                // If continuous autofocus mode fails, attempt to set to a different mode
                if (!CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO)) {
                    CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_NORMAL);
                }
            }
        }

    }

    @Override
    public void onInitARDone(ApplicationException exception) {

        if (exception == null) {
            initApplicationAR();

            mRenderer.setActive(true);

            // Now add the GL surface view. It is important
            // that the OpenGL ES surface view gets added
            // BEFORE the camera is started and video
            // background is configured.
            addContentView(mGlView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            // Sets the UILayout to be drawn in front of the camera
            mUILayout.bringToFront();

            // Sets the layout background to transparent
            mUILayout.setBackgroundColor(Color.TRANSPARENT);

            try {
                vuforiaAppSession.startAR(mCamera);
            } catch (ApplicationException e) {
                Log.e(LOGTAG, e.getString());
            }
        } else {
            Log.e(LOGTAG, exception.getString());
            showInitializationErrorMessage(exception.getString());
        }
    }

    // Shows initialization error messages as System dialogs
    public void showInitializationErrorMessage(String message) {

        final String errorMessage = message;

        runOnUiThread(new Runnable() {

            public void run() {
                if (mErrorDialog != null) {
                    mErrorDialog.dismiss();
                }

                String package_name = getApplication().getPackageName();
                Resources resources = getApplication().getResources();

                // Generates an Alert Dialog to show the error message
                AlertDialog.Builder builder = new AlertDialog.Builder(ImageTargets.this);
                builder
                    .setMessage(errorMessage)
                    .setTitle("Error")
                    .setCancelable(false)
                    .setIcon(0)
                    .setPositiveButton(resources.getIdentifier("button_OK", "string", package_name),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });

                mErrorDialog = builder.create();
                mErrorDialog.show();
            }
        });

    }

    @Override
    public void onVuforiaUpdate(State state) {

        if (mSwitchDatasetAsap) {
            mSwitchDatasetAsap = false;
            TrackerManager tm = TrackerManager.getInstance();
            ObjectTracker ot = (ObjectTracker) tm.getTracker(ObjectTracker.getClassType());
            if (ot == null || mCurrentDataset == null || ot.getActiveDataSet(0) == null) {
                Log.d(LOGTAG, "Failed to swap datasets");
                return;
            }

            doUnloadTrackersData();
            doLoadTrackersData();
        }
    }

    @Override
    public boolean doInitTrackers() {

        // Indicate if the trackers were initialized correctly
        boolean result = true;

        // For ImageTargets, the recommended fusion provider mode is
        // the one recommended by the FUSION_OPTIMIZE_IMAGE_TARGETS_AND_VUMARKS enum
        if (!vuforiaAppSession.setFusionProviderType(FUSION_PROVIDER_TYPE.FUSION_OPTIMIZE_IMAGE_TARGETS_AND_VUMARKS)) {
            return false;
        }

        TrackerManager tManager = TrackerManager.getInstance();

        Tracker tracker = tManager.initTracker(ObjectTracker.getClassType());
        if (tracker == null) {
            Log.e(LOGTAG, "Tracker not initialized. Tracker already initialized or the camera is already started");
            result = false;
        } else {
            Log.i(LOGTAG, "Tracker successfully initialized");
        }

        // Initialize the Positional Device Tracker
        DeviceTracker deviceTracker = (PositionalDeviceTracker) tManager.initTracker(PositionalDeviceTracker.getClassType());
        if (deviceTracker != null) {
            Log.i(LOGTAG, "Successfully initialized Device Tracker");
        } else {
            Log.e(LOGTAG, "Failed to initialize Device Tracker");
        }

        return result;

    }

    @Override
    public boolean doStartTrackers() {

        // Indicate if the trackers were started correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());

        if (objectTracker != null && objectTracker.start()) {
            Log.i(LOGTAG, "Successfully started Object Tracker");
        } else {
            Log.e(LOGTAG, "Failed to start Object Tracker");
            result = false;
        }

        if (isDeviceTrackingActive()) {
            PositionalDeviceTracker deviceTracker = (PositionalDeviceTracker) trackerManager.getTracker(PositionalDeviceTracker.getClassType());

            if (deviceTracker != null && deviceTracker.start()) {
                Log.i(LOGTAG, "Successfully started Device Tracker");
            } else {
                Log.e(LOGTAG, "Failed to start Device Tracker");
            }
        }

        return result;

    }

    @Override
    public boolean doStopTrackers() {

        // Indicate if the trackers were stopped correctly
        boolean result = true;

        TrackerManager trackerManager = TrackerManager.getInstance();

        Tracker objectTracker = trackerManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker != null) {
            objectTracker.stop();
            Log.i(LOGTAG, "Successfully stopped object tracker");
        } else {
            Log.e(LOGTAG, "Failed to stop object tracker");
            result = false;
        }

        // Stop the device tracker
        if (isDeviceTrackingActive()) {
            Tracker deviceTracker = trackerManager.getTracker(PositionalDeviceTracker.getClassType());
            if (deviceTracker != null) {
                deviceTracker.stop();
                Log.i(LOGTAG, "Successfully stopped device tracker");
            } else {
                Log.e(LOGTAG, "Could not stop device tracker");
            }
        }

        return result;

    }

    @Override
    public boolean doDeinitTrackers() {

        TrackerManager tManager = TrackerManager.getInstance();

        // Indicate if the trackers were deinitialized correctly
        boolean result = tManager.deinitTracker(ObjectTracker.getClassType());
        tManager.deinitTracker(PositionalDeviceTracker.getClassType());

        return result;

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return mGestureDetector.onTouchEvent(event);

    }

    boolean isDeviceTrackingActive() {

        return mDeviceTracker;

    }

    public void imageFound(String imageName, int status) {

        VuforiaPlugin.sendImageFoundUpdate(imageName, status);

    }

    public boolean setVuforiaType(int type) {

        return true;

    }

    boolean setVuforiaImageParam(String targetList) {

        if (mType == 1)
            return false;

        if (mTextures != null)
            mTextures.clear();

        mDatasetStrings.clear();

        try {
            JSONArray arr = new JSONArray(targetList);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject temp = (JSONObject) arr.get(i);
                String target_file = temp.getString("filepath");
                String target = temp.getString("imageName");
                String matrix = temp.getString("matrix");
                String map = temp.getString("map");

                Log.d(LOGTAG, "MRAY :: VUFORIA RECEIVED FILE: " + target_file);
                Log.d(LOGTAG, "MRAY :: VUTORIA TARGETS: " + target);

                mDatasetStrings.add(target_file);

                if (mTextures != null)
                    mTextures.add(Texture.loadTextureFromApk(map));

                JSONObject targets = new JSONObject(mTargets);
                targets.put("imageName", target);
                mTargets = targets.toString();
            }
        } catch(JSONException e) {
            return false;
        }

        if (mRenderer != null) {
            mRenderer.updateTargetStrings(mTargets);
            mRenderer.setTextures(mTextures);
        }

        mSwitchDatasetAsap = true;

        return true;

    }

    boolean updateVuforiaModelParam(String model) {

        if (mType == 1)
            return false;

        if (mTextures != null)
            mTextures.clear();

        try {
            JSONArray arr = new JSONArray(model);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject temp = (JSONObject) arr.get(i);
                String target_file = temp.getString("filepath");
                String target = temp.getString("imageName");
                String matrix = temp.getString("matrix");
                String map = temp.getString("map");

                Log.d(LOGTAG, "MRAY :: VUFORIA RECEIVED FILE: " + target_file);
                Log.d(LOGTAG, "MRAY :: VUTORIA TARGETS: " + target);

                if (mTextures != null)
                    mTextures.add(Texture.loadTextureFromApk(map));
            }
        } catch(JSONException e) {
            return false;
        }

        if (mRenderer != null) {
            mRenderer.setTextures(mTextures);
        }

        return true;

    }

    boolean getUserDefinedTargetsFrameQuality() {

        if (mType == 0)
            return false;

        return true;

    }

    boolean setUserDefinedTargetsModelParam(String model) {

        if (mType == 0)
            return false;

        try {
            JSONArray arr = new JSONArray(model);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject temp = (JSONObject) arr.get(i);
                String matrix = temp.getString("matrix");
                String map = temp.getString("map");
            }
        } catch(JSONException e) {
            return false;
        }

        return true;

    }

    boolean cleanUserDefinedTargetsFrameQuality() {

        if (mType == 0)
            return false;

        return true;

    }

}

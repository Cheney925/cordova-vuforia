/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.hopenrun.cordova.vuforia;

import java.util.ArrayList;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.RelativeLayout;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hopenrun.cordova.vuforia.utils.LoadingDialogHandler;
import com.hopenrun.cordova.vuforia.utils.ApplicationGLView;
import com.hopenrun.cordova.vuforia.utils.Texture;

public class ImageTargets implements ApplicationControl {

    private static final String LOGTAG = "ImageTargets";
    private static final String FILE_PROTOCOL = "file://";

    private DataSet mCurrentDataset;
    private int mCurrentDatasetSelectionIndex = 0;
    private ArrayList<String> mDatasetStrings = new ArrayList<String>();

    private ApplicationSession vuforiaAppSession;

    // Our OpenGL view:
    private ApplicationGLView mGlView;

    // Our renderer:
    private ImageTargetRenderer mRenderer;

    private Activity mActivity;
    private ViewGroup mRootView;
    private WebView webView;

    // The textures we will use for rendering:
    private Vector<Texture> mTextures;

    private boolean mSwitchDatasetAsap = false;
    private boolean mContAutofocus = false;
    private boolean mDeviceTracker = false;

    private RelativeLayout mUILayout;

    private boolean mIsDroidDevice = false;

    // Array of target names
    private String mTargets = null;

    // Vuforia license key
    private String mLicenseKey;

    // Holds the camera configuration to use upon resuming
    private int mCamera = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT;
    private int mType = 0;

    public ImageTargets(Activity activity) {

        mActivity = activity;

        mRootView = (ViewGroup) mActivity.findViewById(android.R.id.content);
        webView = (WebView) mRootView.getChildAt(0);

        // Load any sample specific textures:
        mTextures = new Vector<Texture>();

        mIsDroidDevice = android.os.Build.MODEL.toLowerCase().startsWith("droid");

    }

    public void onConfigurationChanged(Configuration config) {

        vuforiaAppSession.onConfigurationChanged();

    }

    private int _R(String name, String defType) {

        // Get the project's package name and a reference to it's resources
        return mActivity.getApplication().getResources().getIdentifier(name, defType, mActivity.getApplication().getPackageName());

    }

    // Initializes AR application components.
    private void initApplicationAR() {

        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

//        LayoutInflater inflater = LayoutInflater.from(mActivity.getApplicationContext());
//        mUILayout = (RelativeLayout) inflater.inflate(_R("camera_overlay", "layout"), null, false);

        // Gets a reference to the loading dialog
//        loadingDialogHandler.mLoadingDialogContainer = mUILayout.findViewById(_R("loading_indicator", "id"));

        mUILayout = new RelativeLayout(mActivity.getApplicationContext());
        mUILayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        mUILayout.setVisibility(View.VISIBLE);
        mUILayout.setBackgroundColor(Color.BLACK);

        mGlView = new ApplicationGLView(mActivity.getApplicationContext());
        mGlView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mGlView.init(translucent, depthSize, stencilSize);

        mRenderer = new ImageTargetRenderer(mActivity, this, vuforiaAppSession, mTargets);
        mRenderer.updateTargetStrings(mTargets);
        mRenderer.setTextures(mTextures);
        mGlView.setRenderer(mRenderer);

        mRenderer.setActive(true);

        // Now add the GL surface view. It is important
        // that the OpenGL ES surface view gets added
        // BEFORE the camera is started and video
        // background is configured.
        mUILayout.addView(mGlView);
        mRootView.addView(mUILayout);

        webView.bringToFront();
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

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

        initApplicationAR();

        vuforiaAppSession.startAR(mCamera);

        vuforiaAppSession.onResume();

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

    boolean isDeviceTrackingActive() {

        return mDeviceTracker;

    }

    public void setCameraIsAutofocus(boolean autofocus) {

        mContAutofocus = autofocus;

    }

    //init Vuforia
    public boolean initVuforia(String vuforiaLicense) {

        mLicenseKey = vuforiaLicense;

        return true;

    }

    // Start our Vuforia activities
    public boolean startVuforia(int camera, int type) {

        mCamera = camera;
        mType = type;

        vuforiaAppSession = new ApplicationSession(this, mLicenseKey);

        vuforiaAppSession.initAR(mActivity, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        // This is needed for some Droid devices to force landscape
        if (mIsDroidDevice) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        return true;

    }

    // Stop Vuforia
    public boolean stopVuforia() {

        Vuforia.deinit();

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        vuforiaAppSession.onPause();

        try {
            vuforiaAppSession.stopAR();
        } catch (ApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        mRootView.removeView(mUILayout);

        webView.setBackgroundColor(Color.WHITE);

        // Unload texture:
        mTextures.clear();
        mTextures = null;

        return true;

    }

    // Stop Vuforia trackers
    public boolean pauseVuforia() {

        if (mGlView != null) {
            mGlView.setVisibility(View.INVISIBLE);
            mGlView.onPause();
        }

        vuforiaAppSession.onPause();

        return doStopTrackers();

    }

    // Start Vuforia trackers
    public boolean resumeVuforia() {

        // This is needed for some Droid devices to force landscape
        if (mIsDroidDevice) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        vuforiaAppSession.onResume();

        // Resume the GL view:
        if (mGlView != null) {
            mGlView.setVisibility(View.VISIBLE);
            mGlView.onResume();
        }

        return doStartTrackers();

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

    public void imageFound(String imageName, int status) {

        VuforiaPlugin.sendImageFoundUpdate(imageName, status);

    }

}

/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.hoperun.cordova.vuforia;

import java.util.ArrayList;
import java.util.Vector;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import com.hoperun.cordova.vuforia.utils.ARModelRenderer;
import com.hoperun.cordova.vuforia.utils.ARModelDisplayView;
import com.hoperun.cordova.vuforia.utils.MatrixParam;
import com.vuforia.CameraDevice;
import com.vuforia.DataSet;
import com.vuforia.DeviceTracker;
import com.vuforia.FUSION_PROVIDER_TYPE;
import com.vuforia.ImageTargetBuilder;
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

import com.hoperun.cordova.vuforia.utils.ImageInfo;
import com.hoperun.cordova.vuforia.utils.VuforiaImageInfo;
import com.hoperun.cordova.vuforia.utils.ARVideoDisplayView;
import com.hoperun.cordova.vuforia.utils.Texture;

import static java.lang.Thread.sleep;

public class CordovaVuforia implements ApplicationControl {

    private static final String LOGTAG = "CordovaVuforia";
    private static final String FILE_PROTOCOL = "file://";

    private int mCurrentDatasetSelectionIndex = 0;

    private DataSet mCurrentDataset = null;
    private ArrayList<String> mDatasetStrings = new ArrayList<String>();

    private ApplicationSession vuforiaAppSession;

    // video view
    private RelativeLayout mVideoLayout;
    private ARVideoDisplayView mVideoView;
    private ARVideoRenderer mRenderer;

    private RelativeLayout mVirtualLayout;
    private ARModelDisplayView mVirtualView;
    private ARModelRenderer mVirtualRenderer;

    private VuforiaImageInfo mImageInfo;

    private Activity mActivity;
    private ViewGroup mRootView;
    private WebView webView;

    private int targetBuilderCounter = 1;
    private RefFreeFrame refFreeFrame = null;

    // The textures we will use for rendering:
    private Vector<Texture> mTextures;

    private boolean mSwitchDatasetAsap = false;
    private boolean mContAutofocus = false;
    private boolean mDeviceTracker = false;
    private boolean mIsDroidDevice = false;

    // Internal variables for holding state
    private boolean vuforiaStarted = false;

    // Vuforia license key
    private String mLicenseKey;

    // Holds the camera configuration to use upon resuming
    private int mCamera = CameraDevice.CAMERA_DIRECTION.CAMERA_DIRECTION_DEFAULT;
    public int mType = 0;

    public CordovaVuforia(Activity activity) {

        mActivity = activity;

        mRootView = (ViewGroup) mActivity.findViewById(android.R.id.content);
        webView = (WebView) mRootView.getChildAt(0);

        // Load any sample specific textures:
        mTextures = new Vector<Texture>();

        mImageInfo = new VuforiaImageInfo();

        mImageInfo.total = 20;
        mImageInfo.current = 0;

        mImageInfo.info = new ImageInfo[mImageInfo.total];
        for (int i = 0; i < mImageInfo.total; i++) {
            mImageInfo.info[i] = new ImageInfo();
            mImageInfo.info[i].matrix = new MatrixParam();
        }

        mTextures.clear();
        mDatasetStrings.clear();

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

        // Do application initialization
        refFreeFrame = new RefFreeFrame(mActivity, this);
        refFreeFrame.init();

        // Create OpenGL ES view:
        int depthSize = 16;
        int stencilSize = 0;
        boolean translucent = Vuforia.requiresAlpha();

        mVirtualLayout = new RelativeLayout(mActivity.getApplicationContext());

        mVirtualView = new ARModelDisplayView(mActivity.getApplicationContext());
        mVirtualView.init(depthSize, stencilSize);

        mVirtualRenderer = new ARModelRenderer(mActivity.getApplicationContext());
        mVirtualView.setRenderer(mVirtualRenderer);

        mVideoLayout = new RelativeLayout(mActivity.getApplicationContext());

        mVideoView = new ARVideoDisplayView(mActivity.getApplicationContext());
        mVideoView.init(translucent, depthSize, stencilSize);

        mRenderer = new ARVideoRenderer(mActivity, this, vuforiaAppSession, mImageInfo);
        mVideoView.setRenderer(mRenderer);

        mRenderer.setActive(true);

        // Now add the GL surface view. It is important
        // that the OpenGL ES surface view gets added
        // BEFORE the camera is started and video
        // background is configured.
        mVirtualLayout.addView(mVirtualView);
        mVideoLayout.addView(mVideoView);
        mRootView.addView(mVideoLayout);
        mRootView.addView(mVirtualLayout);

        mVideoLayout.setVisibility(View.VISIBLE);
        mVirtualLayout.setVisibility(View.VISIBLE);

        mVirtualLayout.bringToFront();
        mVirtualLayout.setBackgroundColor(Color.TRANSPARENT);

        webView.bringToFront();
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

    }

    // This callback is called on Vuforia resume
    @Override
    public void onVuforiaResumed() {

        if (mVideoView != null) {
            mVideoView.setVisibility(View.VISIBLE);
            mVideoView.onResume();
        }

        if (mVirtualView != null) {
            mVirtualView.setVisibility(View.VISIBLE);
            mVirtualView.onResume();
        }

    }

    // This callback is called once Vuforia has been started
    @Override
    public void onVuforiaStarted() {

        mRenderer.updateRenderingPrimitives();
        mRenderer.updateConfiguration();

        if (mType == 1) {
            if (!startUserDefinedTargets()) {
                Log.e(LOGTAG, "Failed to start User defined targets");
            }
        }

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
    public void onVuforiaUpdate(State state) {

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker == null) {
            Log.d(LOGTAG, "Failed to swap datasets");
            return;
        }

        if (mSwitchDatasetAsap) {
            mSwitchDatasetAsap = false;

            doUnloadTrackersData();
            doLoadTrackersData();
        }

        if (refFreeFrame.hasNewTrackableSource()) {
            // Deactivate current dataset
            objectTracker.deactivateDataSet(objectTracker.getActiveDataSets().at(0));

            // Clear the oldest target if the dataset is full or the dataset
            // already contains five user-defined targets.
            if (mCurrentDataset.hasReachedTrackableLimit() || mCurrentDataset.getTrackables().size() >= 5) {
                mCurrentDataset.destroy(mCurrentDataset.getTrackables().at(0));
            }

            // Add new trackable source
            mCurrentDataset.createTrackable(refFreeFrame.getNewTrackableSource());

            // Reactivate current dataset
            objectTracker.activateDataSet(mCurrentDataset);
        }

    }

    @Override
    public void onInitARDone(ApplicationException exception) {

        initApplicationAR();

        vuforiaAppSession.startAR(mCamera);

        vuforiaAppSession.onResume();

    }

    @Override
    public boolean doInitTrackers() {

        // Indicate if the trackers were initialized correctly
        boolean result = true;

        // For CordovaVuforia, the recommended fusion provider mode is
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

    // Methods to load and destroy tracking data.
    @Override
    public boolean doLoadTrackersData() {

        TrackerManager tManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) tManager.getTracker(ObjectTracker.getClassType());
        if (objectTracker == null) {
            return false;
        }

        // Create the data set:
        mCurrentDataset = objectTracker.createDataSet();
        if (mCurrentDataset == null) {
            Log.d(LOGTAG, "Failed to create a new tracking data.");
            return false;
        }

        if (!mDatasetStrings.isEmpty()) {
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

            if (!mCurrentDataset.load(mDatasetStrings.get(mCurrentDatasetSelectionIndex), storage_type)) {
                return false;
            }
        }

        if (!objectTracker.activateDataSet(mCurrentDataset)) {
            Log.d(LOGTAG, "Failed to activate data set.");
            return false;
        }

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
        if (objectTracker == null) {
            return false;
        }

        if (mCurrentDataset != null) {
            if (mCurrentDataset.isActive()) {
                if (objectTracker.getActiveDataSets().at(0).equals(mCurrentDataset) && !objectTracker.deactivateDataSet(mCurrentDataset)) {
                    result = false;
                } else if (!objectTracker.destroyDataSet(mCurrentDataset)) {
                    result = false;
                }
            } else {
                if (objectTracker.getActiveDataSets().at(0) != null && !objectTracker.deactivateDataSet(mCurrentDataset)) {
                    result = false;
                }

                if (!objectTracker.destroyDataSet(mCurrentDataset)) {
                    result = false;
                }
            }

            mCurrentDataset = null;
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

        if (refFreeFrame != null) {
            refFreeFrame.deInit();
        }

        TrackerManager tManager = TrackerManager.getInstance();

        // Indicate if the trackers were deinitialized correctly
        boolean result = tManager.deinitTracker(ObjectTracker.getClassType());
        tManager.deinitTracker(PositionalDeviceTracker.getClassType());

        return result;

    }

    // Scan the environment for your User Defined Target
    private boolean startUserDefinedTargets() {

        Log.d(LOGTAG, "startUserDefinedTargets");

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) (trackerManager.getTracker(ObjectTracker.getClassType()));

        if (objectTracker != null) {
            ImageTargetBuilder targetBuilder = objectTracker.getImageTargetBuilder();

            if (targetBuilder != null) {
                // if needed, stop the target builder
                if (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE)
                    targetBuilder.stopScan();

                objectTracker.stop();

                targetBuilder.startScan();
            }
        } else {
            return false;
        }

        return true;

    }

    // Stop scan the environment for your User Defined Target
    private boolean stopUserDefinedTargets() {

        Log.d(LOGTAG, "stopUserDefinedTargets");

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) (trackerManager.getTracker(ObjectTracker.getClassType()));

        if (objectTracker != null) {
            ImageTargetBuilder targetBuilder = objectTracker.getImageTargetBuilder();

            if (targetBuilder != null) {
                // if needed, stop the target builder
                if (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE)
                    targetBuilder.stopScan();

                objectTracker.start();
            }
        } else {
            return false;
        }

        return true;

    }

    // Builds the User Defined Target
    private void startBuild() {

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager.getTracker(ObjectTracker.getClassType());

        if (objectTracker != null) {
            ImageTargetBuilder targetBuilder = objectTracker.getImageTargetBuilder();
            if (targetBuilder != null) {
                if (targetBuilder.getFrameQuality() == ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_LOW) {
                    Log.d(LOGTAG, "The frame quality low!!!");
                }

                String name;

                do {
                    name = "UserTarget-" + targetBuilderCounter;
                    Log.d(LOGTAG, "TRYING " + name);
                    targetBuilderCounter++;
                } while (!targetBuilder.build(name, .32f));

                refFreeFrame.setCreating();
            }
        }
    }

    // Callback function called when the target creation finished
    public void targetCreated() {

        if (refFreeFrame != null) {
            refFreeFrame.reset();
        }

    }

    public void render() {

        if (refFreeFrame != null) {
            refFreeFrame.render();
        }

    }

    public void updateRendering() {

        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        refFreeFrame.initGL(metrics.widthPixels, metrics.heightPixels);

    }

    public boolean isUserDefinedTargetsRunning() {

        TrackerManager trackerManager = TrackerManager.getInstance();
        ObjectTracker objectTracker = (ObjectTracker) trackerManager.getTracker(ObjectTracker.getClassType());

        if (objectTracker != null) {
            ImageTargetBuilder targetBuilder = objectTracker.getImageTargetBuilder();
            if (targetBuilder != null) {
                return (targetBuilder.getFrameQuality() != ImageTargetBuilder.FRAME_QUALITY.FRAME_QUALITY_NONE) ? true : false;
            }
        }

        return false;

    }

    public boolean isDeviceTrackingActive() {

        return mDeviceTracker;

    }

    public void setCameraIsAutofocus(boolean autofocus) {

        mContAutofocus = autofocus;

    }

    public void imageFound(String imageName, int status) {

        CordovaVuforiaPlugin.sendImageFoundUpdate(imageName, status);

    }

    // init Vuforia
    public synchronized boolean initVuforia(String vuforiaLicense) {

        mLicenseKey = vuforiaLicense;

        return true;

    }

    // Start our Vuforia activities
    public synchronized boolean startVuforia(int camera, int type) {

        if (vuforiaStarted) return false;

        mCamera = camera;
        mType = type;

        vuforiaAppSession = new ApplicationSession(this, mLicenseKey);

        vuforiaAppSession.initAR(mActivity, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);

        // This is needed for some Droid devices to force landscape
        if (mIsDroidDevice) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        vuforiaStarted = true;

        return true;

    }

    // Stop Vuforia
    public synchronized boolean stopVuforia() {

        if (!vuforiaStarted) return false;

        if (mVirtualRenderer != null) mVirtualRenderer.clearTextures();
        if (mVideoView != null) mVideoView.onPause();
        if (mVirtualView != null) mVirtualView.onPause();

        vuforiaAppSession.onPause();

        try {
            vuforiaAppSession.stopAR();
        } catch (ApplicationException e) {
            Log.e(LOGTAG, e.getString());
        }

        try {
            mRootView.removeView(mVideoLayout);
        } catch (Exception e) {}

        webView.setBackgroundColor(Color.WHITE);

        // Unload texture:
        mDatasetStrings.clear();
        mImageInfo.current = 0;

        Vuforia.deinit();

        System.gc();

        try {
            sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        vuforiaStarted = false;

        return true;

    }

    // Stop Vuforia trackers
    public synchronized boolean pauseVuforia() {

        if (mVideoView != null) {
            mVideoView.onPause();
        }

        if (mVirtualView != null) {
            mVirtualView.onPause();
        }

        vuforiaAppSession.onPause();

        return doStopTrackers();

    }

    // Start Vuforia trackers
    public synchronized boolean resumeVuforia() {

        // This is needed for some Droid devices to force landscape
        if (mIsDroidDevice) {
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        vuforiaAppSession.onResume();

        // Resume the GL view:
        if (mVideoView != null) {
            mVideoView.onResume();
        }

        if (mVirtualView != null) {
            mVirtualView.onResume();
        }

        return doStartTrackers();

    }

    public synchronized boolean setVuforiaType(int type) {

        mType = type;

        if (mType == 0) {
            doUnloadTrackersData();
            mDatasetStrings.clear();

            mImageInfo.current = 0;

            if (mRenderer != null) mRenderer.updateImageInfo(mImageInfo);
            if (mVirtualRenderer != null) mVirtualRenderer.clearTextures();

            if (!stopUserDefinedTargets()) Log.e(LOGTAG, "Failed to stop User defined targets");
        } else if (mType == 1) {
            doUnloadTrackersData();
            mDatasetStrings.clear();
            doLoadTrackersData();

            mImageInfo.current = 0;

            if (mRenderer != null) mRenderer.updateImageInfo(mImageInfo);
            if (mVirtualRenderer != null) mVirtualRenderer.clearTextures();

            if (!startUserDefinedTargets()) Log.e(LOGTAG, "Failed to start User defined targets");
        } else {
            return false;
        }

        return true;

    }

    public synchronized boolean setVuforiaImageParam(String targetList) {

        if (mType != 0) return false;
        if (targetList == null) return false;

        try {
            JSONArray arr = new JSONArray(targetList);

            if (arr.length() > 0) {
                if (mVirtualRenderer != null) mVirtualRenderer.clearTextures();

                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mTextures.clear();
                mDatasetStrings.clear();
                mImageInfo.current = 0;
            }

            for (int i = 0; i < arr.length(); i++) {
                JSONObject temp = (JSONObject) arr.get(i);
                String filepath = temp.optString("filepath");
                String imageName = temp.optString("imageName");
                String map = temp.optString("map");

                JSONObject matrix = temp.optJSONObject("matrix");
                float posX = matrix.optInt("posX");
                float posY = matrix.optInt("posY");
                float posZ = matrix.optInt("posZ");

                float scaleX = matrix.optInt("scaleX");
                float scaleY = matrix.optInt("scaleY");
                float scaleZ = matrix.optInt("scaleZ");

                float rotate = matrix.optInt("rotate");
                if (rotate < -360 || rotate > 360) {
                    mImageInfo.current = 0;
                    return false;
                }

                Log.d(LOGTAG, "MRAY :: VUFORIA FILEPATH: " + filepath);
                Log.d(LOGTAG, "MRAY :: VUTORIA IMAGENAME: " + imageName);
                Log.d(LOGTAG, "MRAY :: VUTORIA MAP: " + map);
                Log.d(LOGTAG, "MRAY :: VUTORIA POSX: " + posX);
                Log.d(LOGTAG, "MRAY :: VUTORIA POSY: " + posY);
                Log.d(LOGTAG, "MRAY :: VUTORIA POSZ: " + posZ);
                Log.d(LOGTAG, "MRAY :: VUTORIA SCALEX: " + scaleX);
                Log.d(LOGTAG, "MRAY :: VUTORIA SCALEY: " + scaleY);
                Log.d(LOGTAG, "MRAY :: VUTORIA SCALEZ: " + scaleZ);
                Log.d(LOGTAG, "MRAY :: VUTORIA ROTATE: " + rotate);

                if (filepath != null && imageName != null) {
                    if (mImageInfo.current == 0) {
                        mDatasetStrings.add(filepath);
                    } else {
                        for (int j = 0; j < mImageInfo.current; ++j) {
                            if (!mImageInfo.info[j].filepath.contains(filepath)) {
                                mDatasetStrings.add(filepath);
                            }
                        }
                    }

                    Texture texture = Texture.loadTextureFromApk(map);
                    if (texture != null) {
                        mTextures.add(texture);
                    }

                    if (mImageInfo.current < mImageInfo.total) {
                        mImageInfo.info[mImageInfo.current].filepath = filepath;
                        mImageInfo.info[mImageInfo.current].imageName = imageName;
                        mImageInfo.info[mImageInfo.current].map = map;

                        mImageInfo.info[mImageInfo.current].matrix.posX = posX;
                        mImageInfo.info[mImageInfo.current].matrix.posY = posY;
                        mImageInfo.info[mImageInfo.current].matrix.posZ = posZ;

                        mImageInfo.info[mImageInfo.current].matrix.scaleX = scaleX;
                        mImageInfo.info[mImageInfo.current].matrix.scaleY = scaleY;
                        mImageInfo.info[mImageInfo.current].matrix.scaleZ = scaleZ;

                        mImageInfo.info[mImageInfo.current].matrix.rotate = rotate;

                        mImageInfo.current++;
                    }
                }
            }
        } catch(JSONException e) {
            return false;
        }

        if (mRenderer != null) mRenderer.updateImageInfo(mImageInfo);
        if (mVirtualRenderer != null) mVirtualRenderer.setTextures(mTextures);

        mSwitchDatasetAsap = true;

        return true;

    }

    public synchronized boolean updateVuforiaModelParam(String model) {

        if (mType != 0) return false;
        if (model == null) return false;

        try {
            JSONArray arr = new JSONArray(model);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject temp = (JSONObject) arr.get(i);
                String filepath = temp.optString("filepath");
                String imageName = temp.optString("imageName");
                String map = temp.optString("map");

                JSONObject matrix = temp.optJSONObject("matrix");
                float posX = matrix.optInt("posX");
                float posY = matrix.optInt("posY");
                float posZ = matrix.optInt("posZ");

                float scaleX = matrix.optInt("scaleX");
                float scaleY = matrix.optInt("scaleY");
                float scaleZ = matrix.optInt("scaleZ");

                float rotate = matrix.optInt("rotate");
                if (rotate < -360 || rotate > 360) {
                    mImageInfo.current = 0;
                    return false;
                }

                Log.d(LOGTAG, "MRAY :: VUFORIA FILEPATH: " + filepath);
                Log.d(LOGTAG, "MRAY :: VUTORIA IMAGENAME: " + imageName);
                Log.d(LOGTAG, "MRAY :: VUTORIA MAP: " + map);
                Log.d(LOGTAG, "MRAY :: VUTORIA POSX: " + posX);
                Log.d(LOGTAG, "MRAY :: VUTORIA POSY: " + posY);
                Log.d(LOGTAG, "MRAY :: VUTORIA POSZ: " + posZ);
                Log.d(LOGTAG, "MRAY :: VUTORIA SCALEX: " + scaleX);
                Log.d(LOGTAG, "MRAY :: VUTORIA SCALEY: " + scaleY);
                Log.d(LOGTAG, "MRAY :: VUTORIA SCALEZ: " + scaleZ);
                Log.d(LOGTAG, "MRAY :: VUTORIA ROTATE: " + rotate);

                if (filepath != null  && imageName != null) {
                    for (int j = 0; j < mImageInfo.current; ++j) {
                        if (mImageInfo.info[j].filepath.contains(filepath) && mImageInfo.info[j].imageName.contains(imageName)) {
                            mImageInfo.info[j].map = map;

                            mImageInfo.info[mImageInfo.current].matrix.posX = posX;
                            mImageInfo.info[mImageInfo.current].matrix.posY = posY;
                            mImageInfo.info[mImageInfo.current].matrix.posZ = posZ;

                            mImageInfo.info[mImageInfo.current].matrix.scaleX = scaleX;
                            mImageInfo.info[mImageInfo.current].matrix.scaleY = scaleY;
                            mImageInfo.info[mImageInfo.current].matrix.scaleZ = scaleZ;

                            mImageInfo.info[mImageInfo.current].matrix.rotate = rotate;
                        }
                    }
                }
            }

            if (mVirtualRenderer != null) mVirtualRenderer.clearTextures();

            try {
                sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mTextures.clear();

            for (int j = 0; j < mImageInfo.current; ++j) {
                Texture texture = Texture.loadTextureFromApk(mImageInfo.info[j].map);
                if (texture != null) {
                    mTextures.add(texture);
                }
            }
        } catch(JSONException e) {
            return false;
        }

        if (mRenderer != null) mRenderer.updateImageInfo(mImageInfo);
        if (mVirtualRenderer != null) mVirtualRenderer.setTextures(mTextures);

        return true;

    }

    public synchronized boolean getUserDefinedTargetsFrameQuality() {

        int cn = 0;

        if (mType != 1) return false;

        while (!isUserDefinedTargetsRunning()) {
            try {
                sleep(600);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            cn++;

            if (cn >= 5) return false;
        }

        // Builds the new target
        startBuild();

        return true;

    }

    public synchronized boolean setUserDefinedTargetsModelParam(String model) {

        if (mType != 1) return false;
        if (model == null) return false;

        try {
            JSONArray arr = new JSONArray(model);

            if (arr.length() > 0) {
                if (mVirtualRenderer != null) mVirtualRenderer.clearTextures();

                try {
                    sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mTextures.clear();
                mImageInfo.current = 0;
            }

            for (int i = 0; i < arr.length(); i++) {
                JSONObject temp = (JSONObject) arr.get(i);
                String map = temp.optString("map");

                JSONObject matrix = temp.optJSONObject("matrix");
                float posX = matrix.optInt("posX");
                float posY = matrix.optInt("posY");
                float posZ = matrix.optInt("posZ");

                float scaleX = matrix.optInt("scaleX");
                float scaleY = matrix.optInt("scaleY");
                float scaleZ = matrix.optInt("scaleZ");

                float rotate = matrix.optInt("rotate");
                if (rotate < -360 || rotate > 360) {
                    mImageInfo.current = 0;
                    return false;
                }

                Log.d(LOGTAG, "MRAY :: VUTORIA MAP: " + map);
                Log.d(LOGTAG, "MRAY :: VUTORIA POSX: " + posX);
                Log.d(LOGTAG, "MRAY :: VUTORIA POSY: " + posY);
                Log.d(LOGTAG, "MRAY :: VUTORIA POSZ: " + posZ);
                Log.d(LOGTAG, "MRAY :: VUTORIA SCALEX: " + scaleX);
                Log.d(LOGTAG, "MRAY :: VUTORIA SCALEY: " + scaleY);
                Log.d(LOGTAG, "MRAY :: VUTORIA SCALEZ: " + scaleZ);
                Log.d(LOGTAG, "MRAY :: VUTORIA ROTATE: " + rotate);

                Texture texture = Texture.loadTextureFromApk(map);
                if (texture != null) {
                    mTextures.add(texture);
                }

                if (mImageInfo.current < mImageInfo.total) {
                    mImageInfo.info[mImageInfo.current].map = map;

                    mImageInfo.info[mImageInfo.current].matrix.posX = posX;
                    mImageInfo.info[mImageInfo.current].matrix.posY = posY;
                    mImageInfo.info[mImageInfo.current].matrix.posZ = posZ;

                    mImageInfo.info[mImageInfo.current].matrix.scaleX = scaleX;
                    mImageInfo.info[mImageInfo.current].matrix.scaleY = scaleY;
                    mImageInfo.info[mImageInfo.current].matrix.scaleZ = scaleZ;

                    mImageInfo.info[mImageInfo.current].matrix.rotate = rotate;

                    mImageInfo.current++;
                }
            }
        } catch(JSONException e) {
            return false;
        }

        if (mRenderer != null) mRenderer.updateImageInfo(mImageInfo);
        if (mVirtualRenderer != null) mVirtualRenderer.setTextures(mTextures);

        return true;

    }

    public synchronized boolean cleanUserDefinedTargetsFrameQuality() {

        if (mType != 1) return false;

        doUnloadTrackersData();

        mImageInfo.current = 0;

        if (mRenderer != null) mRenderer.updateImageInfo(mImageInfo);
        if (mVirtualRenderer != null) mVirtualRenderer.clearTextures();

        return true;

    }

}

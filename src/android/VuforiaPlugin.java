package com.hopenrun.cordova.vuforia;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.util.Log;
import android.Manifest;
import android.view.ViewGroup;
import android.webkit.WebView;

public class VuforiaPlugin extends CordovaPlugin {

    private static final String LOGTAG = "CordovaVuforiaPlugin";

    // Some public static variables used to communicate state
    public static final String CAMERA = Manifest.permission.CAMERA;

    // What access to the camera do we require?
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

    // Save a copy of our starting vuforia context so that we can start reference it later is needs be
    private static CallbackContext persistantVuforiaStartCallback;

    // Some internal variables for storing state across methods
    private static String ACTION;
    private static JSONArray ARGS;

    private static CordovaWebView cordovaWebView = null;
    private static Activity activity = null;

    private ImageTargets mImageTargets;

    // Internal variables for holding state
    private boolean vuforiaStarted = false;

    public VuforiaPlugin() {

    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {

        super.initialize(cordova, webView);

        cordovaWebView = webView;
        activity = cordova.getActivity();

        mImageTargets = new ImageTargets(cordova.getActivity());
        mImageTargets.setCameraIsAutofocus(false);

        Log.d(LOGTAG, "Plugin initialized.");

    }

    @Override
    public void onConfigurationChanged(Configuration config) {

        mImageTargets.onConfigurationChanged(config);

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        // Handle all expected actions
        if (action.equals("initVuforia")) {
            return initVuforia(action, args, callbackContext);
        } else if (action.equals("startVuforia")) {
            return startVuforia(action, args, callbackContext);
        } else if (action.equals("stopVuforia")) {
            return stopVuforia(action, args, callbackContext);
        } else if (action.equals("pauseVuforia")) {
            return pauseVuforia(action, args, callbackContext);
        } else if (action.equals("resumeVuforia")) {
            return resumeVuforia(action, args, callbackContext);
        } else if (action.equals("setVuforiaType")) {
            return setVuforiaType(action, args, callbackContext);
        } else if (action.equals("setVuforiaImageParam")) {
            return setVuforiaImageParam(action, args, callbackContext);
        } else if (action.equals("updateVuforiaModelParam")) {
            return updateVuforiaModelParam(action, args, callbackContext);
        } else if (action.equals("getUserDefinedTargetsFrameQuality")) {
            return getUserDefinedTargetsFrameQuality(action, args, callbackContext);
        } else if (action.equals("setUserDefinedTargetsModelParam")) {
            return setUserDefinedTargetsModelParam(action, args, callbackContext);
        } else if (action.equals("cleanUserDefinedTargetsFrameQuality")) {
            return cleanUserDefinedTargetsFrameQuality(action, args, callbackContext);
        }

        return false;

    }

    //init Vuforia
    public boolean initVuforia(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        String option = args.getString(0);

        JSONObject jsonObject = new JSONObject(option);

        String vuforiaLicense = jsonObject.optString("vuforiaLicense");

        boolean result = mImageTargets.initVuforia(vuforiaLicense);

        if (result)
            pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
        else
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

        return result;

    }

    // Start our Vuforia activities
    public boolean startVuforia(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        ACTION = action;
        ARGS = args;

        // If we are starting Vuforia, set the public variable referencing our start callback for later use
        VuforiaPlugin.persistantVuforiaStartCallback = callbackContext;

        // Check to see if we have permission to access the camera
        if (cordova.hasPermission(CAMERA)) {
            String option = args.getString(0);

            JSONObject jsonObject = new JSONObject(option);

            // Get all of our ARGS out and into local variables
            int camera = jsonObject.optInt("camera");
            int type = jsonObject.optInt("type");

            // Launch a new activity with Vuforia in it. Expect it to return a result.
            boolean result = mImageTargets.startVuforia(camera, type);

            if (result)
                pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
            else
                pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

            vuforiaStarted = true;

            return result;
        } else {
            // Request the camera permission and handle the outcome.
            cordova.requestPermission(this, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE, CAMERA);

            return false;
        }

    }

    // Stop Vuforia
    public boolean stopVuforia(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        // Is Vuforia sterts?
        if (vuforiaStarted) {
            Log.d(LOGTAG, "Stopping plugin");

            // Stop Vuforia
            boolean result = mImageTargets.stopVuforia();

            if (result)
                pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
            else
                pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

            vuforiaStarted = false;

            return result;
        } else {
            Log.d(LOGTAG, "Cannot stop the plugin because it wasn't started");

            pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

            return true;
        }

    }

    // Stop Vuforia trackers
    public boolean pauseVuforia(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.d(LOGTAG, "Pausing trackers");

        boolean result = mImageTargets.pauseVuforia();

        if (result)
            pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
        else
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

        return result;

    }

    // Start Vuforia trackers
    public boolean resumeVuforia(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.d(LOGTAG, "Resuming trackers");

        boolean result = mImageTargets.resumeVuforia();

        if (result)
            pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
        else
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

        return result;

    }

    //set Vuforia work mode
    public boolean setVuforiaType(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.d(LOGTAG, "ARGS: " + args);

        int type = args.getInt(0);

        if (type < 0 || type > 8) {
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);
            return false;
        }

        boolean result = mImageTargets.setVuforiaType(type);

        if (result)
            pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
        else
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

        return result;

    }

    //set param
    public boolean setVuforiaImageParam(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        String option = args.getJSONArray(0).toString();

        boolean result = mImageTargets.setVuforiaImageParam(option);

        if (result)
            pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
        else
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

        return result;

    }

    // Start Vuforia trackers
    public boolean updateVuforiaModelParam(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        String option = args.getJSONArray(0).toString();

        boolean result = mImageTargets.updateVuforiaModelParam(option);

        if (result)
            pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
        else
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

        return result;

    }

    //get user defined targets
    public boolean getUserDefinedTargetsFrameQuality(String action, JSONArray args, CallbackContext callbackContext) throws JSONException  {

        boolean result = mImageTargets.getUserDefinedTargetsFrameQuality();

        if (result)
            pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
        else
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

        return result;

    }

    //set user defined targets model param
    public boolean setUserDefinedTargetsModelParam(String action, JSONArray args, CallbackContext callbackContext) throws JSONException  {

        String option = args.getJSONArray(0).toString();

        boolean result = mImageTargets.setUserDefinedTargetsModelParam(option);

        if (result)
            pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
        else
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

        return result;

    }

    //clean user defined targets
    public boolean cleanUserDefinedTargetsFrameQuality(String action, JSONArray args, CallbackContext callbackContext) throws JSONException  {

        boolean result = mImageTargets.cleanUserDefinedTargetsFrameQuality();

        if (result)
            pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);
        else
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);

        return result;

    }

    // Handle the results of our permissions request
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {

        for (int r:grantResults) {
            // Is the permission denied for our video request?
            if (r == PackageManager.PERMISSION_DENIED && requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
                // Send a plugin error
                pluginResultCallback(PluginResult.Status.ERROR, "status", 0, persistantVuforiaStartCallback);
                return;
            }
        }

        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE)
            execute(ACTION, ARGS, persistantVuforiaStartCallback); // Re-call execute with all the same values as before (will re-check for permissions)

    }

    private void pluginResultCallback(PluginResult.Status status, String name, Object value, CallbackContext callbackContext) {

        try {
            JSONObject json = new JSONObject();
            json.put(name, value);

            PluginResult mPlugin = new PluginResult(status, json);
            callbackContext.sendPluginResult(mPlugin);
        } catch(JSONException e) {
            Log.d(LOGTAG, "JSON ERROR: " + e);
        }

    }

    // Send an asynchronous update when an image is found and Vuforia is set to stay open.
    public static void sendImageFoundUpdate(String imageName, int status) {

        // Create an object to hold our response
        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put("status", status);
            jsonObj.put("imageName", imageName);
        } catch (JSONException e) {
            Log.d(LOGTAG, "JSON ERROR: " + e);
        }

        if (activity != null) {
            final String jsStr = String.format("window.VuforiaPlugin.onNetStatusChange(%s)", jsonObj.toString());
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (cordovaWebView != null)
                        cordovaWebView.loadUrl("javascript:" + jsStr);
                }
            });
        }

    }

}

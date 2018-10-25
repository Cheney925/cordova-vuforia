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
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.Manifest;

import com.hopenrun.cordova.vuforia.ImageTargets;

public class VuforiaPlugin extends CordovaPlugin {

    static final String LOGTAG = "CordovaVuforiaPlugin";

    // Some public static variables used to communicate state
    public static final String CAMERA = Manifest.permission.CAMERA;
    public static final String PLUGIN_ACTION = "org.cordova.plugin.vuforia.action";
    public static final String DISMISS_ACTION = "dismiss";
    public static final String PAUSE_ACTION = "pause";
    public static final String RESUME_ACTION = "resume";
    public static final String SETMODE_ACTION = "setMode";
    public static final String SETIMAGEPARAM_ACTION = "setImageParam";
    public static final String UPDATEMODELPARAM_ACTION = "updateModelParam";
    public static final String GETUSEERDEFTARGETSFRAME_ACTION = "getUserDefTargetsFrame";
    public static final String SETUSEERDEFTARGETSMODELPARAM_ACTION = "setUserDefTargetsModelParam";
    public static final String CLEANUSERDEFTARGETSFRAME_ACTION = "cleanUserDefTargetsFrame";

    // Save some ENUM values to describe plugin results
    public static final int ERROR_RESULT = 0;

    // What access to the camera do we require?
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;

    // Save a copy of our starting vuforia context so that we can start reference it later is needs be
    private static CallbackContext persistantVuforiaStartCallback;

    // Some internal variables for storing state across methods
    private static String ACTION;
    private static JSONArray ARGS;

    private static String vuforiaLicense;

    private static CordovaWebView cordovaWebView = null;
    private static Activity activity = null;

    static final int IMAGE_REC_REQUEST = 1;

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

        Log.d(LOGTAG, "Plugin initialized.");

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

        vuforiaLicense = args.getString(0);

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;
    }

    // Start our Vuforia activities
    public boolean startVuforia(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        // If we are starting Vuforia, set the public variable referencing our start callback for later use
        VuforiaPlugin.persistantVuforiaStartCallback = callbackContext;

        ACTION = action;
        ARGS = args;

        // Get all of our ARGS out and into local variables
        int camera = args.getInt(0);
        int type = args.getInt(1);

        Context context =  cordova.getActivity().getApplicationContext();

        // Create a new intent to pass data to Vuforia
        Intent intent = new Intent(context, ImageTargets.class);

        intent.putExtra("CAMERA_INDEX", camera);
        intent.putExtra("TYPE_MODE", type);
        intent.putExtra("LICENSE_KEY", vuforiaLicense);

        // Check to see if we have permission to access the camera
        if (cordova.hasPermission(CAMERA)) {
            // Launch a new activity with Vuforia in it. Expect it to return a result.
            cordova.startActivityForResult(this, intent, IMAGE_REC_REQUEST);
            vuforiaStarted = true;
        } else {
            // Request the camera permission and handle the outcome.
            cordova.requestPermission(this, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE, CAMERA);
        }

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;

    }

    // Stop Vuforia
    public boolean stopVuforia(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        // Is Vuforia sterts?
        if (vuforiaStarted) {
            Log.d(LOGTAG, "Stopping plugin");

            // Stop Vuforia
            sendAction(DISMISS_ACTION);
            vuforiaStarted = false;
        } else {
            Log.d(LOGTAG, "Cannot stop the plugin because it wasn't started");
        }

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;

    }

    // Stop Vuforia trackers
    public boolean pauseVuforia(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.d(LOGTAG, "Pausing trackers");

        sendAction(PAUSE_ACTION);

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;

    }

    // Start Vuforia trackers
    public boolean resumeVuforia(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.d(LOGTAG, "Resuming trackers");

        sendAction(RESUME_ACTION);

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;

    }

    //set Vuforia work mode
    public boolean setVuforiaType(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.d(LOGTAG, "ARGS: " + args);

        int type = args.getInt(0);

        if (type < 0 || type > 8) {
            pluginResultCallback(PluginResult.Status.ERROR, "status", 0, callbackContext);
            return false;
        }

        Intent intent = new Intent(PLUGIN_ACTION);
        intent.putExtra(PLUGIN_ACTION, SETMODE_ACTION);
        intent.putExtra("SET_MODE", type);

        this.cordova.getActivity().sendBroadcast(intent);

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;

    }

    //set param
    public boolean setVuforiaImageParam(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        String option = args.getJSONArray(0).toString();

        sendAction(SETIMAGEPARAM_ACTION, option);

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;

    }

    // Start Vuforia trackers
    public boolean updateVuforiaModelParam(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        String option = args.getJSONArray(0).toString();

        sendAction(UPDATEMODELPARAM_ACTION, option);

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;

    }

    //get user defined targets
    public boolean getUserDefinedTargetsFrameQuality(String action, JSONArray args, CallbackContext callbackContext) throws JSONException  {

        sendAction(GETUSEERDEFTARGETSFRAME_ACTION);

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;

    }

    //set user defined targets model param
    public boolean setUserDefinedTargetsModelParam(String action, JSONArray args, CallbackContext callbackContext) throws JSONException  {

        String option = args.getJSONArray(0).toString();

        sendAction(SETUSEERDEFTARGETSMODELPARAM_ACTION, option);

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;

    }

    //clean user defined targets
    public boolean cleanUserDefinedTargetsFrameQuality(String action, JSONArray args, CallbackContext callbackContext) throws JSONException  {

        sendAction(CLEANUSERDEFTARGETSFRAME_ACTION);

        pluginResultCallback(PluginResult.Status.OK, "status", 1, callbackContext);

        return true;

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

    // Called when we receive a response from an activity we've launched
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        String name;

        // If we get to this point with no data  then we've likely got an error. Or the activity closed because of an error.
        if (data == null) {
            name = "ERROR";
        } else {
            name = data.getStringExtra("name");
        }

        Log.d(LOGTAG, "Plugin received '" + name + "' from Vuforia.");

        // Check which request we're responding to
        if (requestCode == IMAGE_REC_REQUEST) {
            // Check what result code we received
            switch(resultCode){
                case ERROR_RESULT: // We've received an image (hopefully)
                    // Attempt to build and send a result back to Cordova.
                    // Send a result specifically to our PERSISTANT callback i.e. the callback given to startVuforia.
                    // This allows us to receive other messages from start/stop tracker events without losing this particular callback.
                    persistantVuforiaStartCallback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, name));
                    break;
                default:
                    Log.d(LOGTAG, "Error - received unexpected code on Activity close: " + resultCode);
            }
        }

        // Mark Vuforia as closed
        vuforiaStarted = false;

    }

    // Send a broadcast to our open activity (probably Vuforia)
    private void sendAction(String action) {

        Intent resumeIntent = new Intent(PLUGIN_ACTION);
        resumeIntent.putExtra(PLUGIN_ACTION, action);

        this.cordova.getActivity().sendBroadcast(resumeIntent);

    }

    // Send a broadcast to our open activity (probably Vuforia)
    private void sendAction(String action, String data) {

        Intent resumeIntent = new Intent(PLUGIN_ACTION);
        resumeIntent.putExtra(PLUGIN_ACTION, action);
        resumeIntent.putExtra("ACTION_DATA", data);

        this.cordova.getActivity().sendBroadcast(resumeIntent);

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

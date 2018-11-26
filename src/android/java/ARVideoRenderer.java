/*===============================================================================
Copyright (c) 2016-2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.hoperun.cordova.vuforia;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.hoperun.cordova.vuforia.utils.Math;
import com.hoperun.cordova.vuforia.utils.Utils;
import com.hoperun.cordova.vuforia.utils.VuforiaImageInfo;
import com.vuforia.Device;
import com.vuforia.ImageTargetResult;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.TrackableResultList;
import com.vuforia.Vuforia;
import java.lang.ref.WeakReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * The renderer class for the UserDefinedTargets sample.
 *
 * In the renderFrame() function you can render augmentations to display over the Target
 */
public class ARVideoRenderer implements GLSurfaceView.Renderer, ApplicationRendererControl {

    private static final String LOGTAG = "UserDefinedRenderer";

    private static int mTextureIndex = 0;

    private static final float OBJECT_SCALE_FLOAT = 92.0f;
    private static final float kObjectScale = 0.05f;

    private final WeakReference<CordovaVuforia> mCordovaVuforiaRef;
    private final ApplicationSession vuforiaAppSession;
    private final ApplicationRenderer mAppRenderer;

    private float mRotate = 0.0f;

    private VuforiaImageInfo mImageInfo;

    private Activity mActivity;

    private static float[] mMVPMatrix = new float[16];

    private boolean mIsActive = false;

    private static boolean mTrackableResult = false;

    public ARVideoRenderer(Activity activity, CordovaVuforia cordovaVuforia, ApplicationSession session, VuforiaImageInfo imageInfo) {

        mCordovaVuforiaRef = new WeakReference<CordovaVuforia>(cordovaVuforia);
        vuforiaAppSession = session;

        mImageInfo = imageInfo;

        mActivity = activity;

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mAppRenderer = new ApplicationRenderer(this, mActivity, Device.MODE.MODE_AR, false, 0.01f , 600f);

    }

    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();

        mAppRenderer.onSurfaceCreated();

    }

    // Called when the surface changes size.++
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        GLES20.glViewport(0, 0, width, height);

        // Call function to update rendering when render surface
        // parameters have changed:
        mCordovaVuforiaRef.get().updateRendering();

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mAppRenderer.onConfigurationChanged(mIsActive);

        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);

        android.content.res.Configuration config = mActivity.getResources().getConfiguration();

        if (config.orientation == config.ORIENTATION_LANDSCAPE) {
            if (mCordovaVuforiaRef.get().mType == 0) {
                mRotate = 0.0f;
            } else if (mCordovaVuforiaRef.get().mType == 1) {
                mRotate = 90.0f;
            }
        } else if (config.orientation == config.ORIENTATION_PORTRAIT) {
            mRotate = 0.0f;
        }

    }

    @Override
    public void onDrawFrame(GL10 gl) {

        if (!mIsActive) return;

        // Call our function to render content from ApplicationRenderer class
        mAppRenderer.render();

    }

    public void updateConfiguration() {

        mAppRenderer.onConfigurationChanged(mIsActive);

    }

    public void setActive(boolean active) {

        mIsActive = active;

        if (mIsActive) mAppRenderer.configureVideoBackground();

    }

    // The render function.
    // This function is called from the ApplicationRenderer by using the RenderingPrimitives views.
    // The state is owned by ApplicationRenderer which is controlling its lifecycle.
    // NOTE: State should not be cached outside this method.
    public void renderFrame(State state, float[] projectionMatrix) {

        boolean mTrackableState = true;

        // Renders video background replacing Renderer.DrawVideoBackground()
        mAppRenderer.renderVideoBackground(state);

        // Set the device pose matrix as identity
        Matrix44F devicePoseMattix = Math.Matrix44FIdentity();
        Matrix44F modelMatrix;

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // Render the RefFree UI elements depending on the current state
        mCordovaVuforiaRef.get().render();

        // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
        if (state.getDeviceTrackableResult() != null && state.getDeviceTrackableResult().getStatus() != TrackableResult.STATUS.NO_POSE) {
            modelMatrix = Tool.convertPose2GLMatrix(state.getDeviceTrackableResult().getPose());

            // We transpose here because Matrix44FInverse returns a transposed matrix
            devicePoseMattix = Math.Matrix44FTranspose(Math.Matrix44FInverse(modelMatrix));
        }

        // Did we find any trackables this frame?
        TrackableResultList trackableResultList = state.getTrackableResults();

        for (TrackableResult trackableResult : trackableResultList) {
            Trackable trackable = trackableResult.getTrackable();

            if (trackableResult.isOfType(ImageTargetResult.getClassType())) {
                mTrackableState = false;

                mCordovaVuforiaRef.get().imageFound(trackable.getName(), 1);

                int textureIndex = -1;

                for (int i = 0; i < mImageInfo.current; ++i) {
                    if (trackable.getName().equalsIgnoreCase(mImageInfo.info[i].imageName)) {
                        textureIndex = i;
                        break;
                    }
                }

                modelMatrix = Tool.convertPose2GLMatrix(trackableResult.getPose());

                float[] mMatrix = modelMatrix.getData();
                float[] viewMatrix = devicePoseMattix.getData();

                float posX = 0.0f, posY = 0.0f, posZ = 0.003f, scaleX = 0.0f, scaleY = 0.0f, scaleZ = 0.0f, rotate = 0.0f;

                if (mCordovaVuforiaRef.get().mType == 1 && mImageInfo.current > 0) textureIndex = 0;

                if (textureIndex >= 0) {
                    if (mCordovaVuforiaRef.get().mType == 0) {
                        posX = mImageInfo.info[textureIndex].matrix.posX / 1000;
                        posY = mImageInfo.info[textureIndex].matrix.posY / 1000;
                        posZ = mImageInfo.info[textureIndex].matrix.posZ / 1000 + 0.003f;
                        scaleX = mImageInfo.info[textureIndex].matrix.scaleX + OBJECT_SCALE_FLOAT;
                        scaleY = mImageInfo.info[textureIndex].matrix.scaleY + OBJECT_SCALE_FLOAT;
                        scaleZ = mImageInfo.info[textureIndex].matrix.scaleZ + OBJECT_SCALE_FLOAT;
                        rotate = mImageInfo.info[textureIndex].matrix.rotate;
                    } else if (mCordovaVuforiaRef.get().mType == 1) {
                        posX = mImageInfo.info[textureIndex].matrix.posX / 1000;
                        posY = mImageInfo.info[textureIndex].matrix.posY / 1000;
                        posZ = mImageInfo.info[textureIndex].matrix.posZ / 1000 + 0.003f;
                        scaleX = mImageInfo.info[textureIndex].matrix.scaleX / 100 + kObjectScale;
                        scaleY = mImageInfo.info[textureIndex].matrix.scaleY / 100 + kObjectScale;
                        scaleZ = mImageInfo.info[textureIndex].matrix.scaleZ / 100 + kObjectScale;
                        rotate = mImageInfo.info[textureIndex].matrix.rotate;
                    }
                } else {
                    if (mCordovaVuforiaRef.get().mType == 0) {
                        posX = 0.0f;
                        posY = 0.0f;
                        posZ = 0.003f;
                        scaleX = OBJECT_SCALE_FLOAT;
                        scaleY = OBJECT_SCALE_FLOAT;
                        scaleZ = OBJECT_SCALE_FLOAT;
                        rotate = 0.0f;
                    } else if (mCordovaVuforiaRef.get().mType == 1) {
                        posX = 0.0f;
                        posY = 0.0f;
                        posZ = 0.003f;
                        scaleX = kObjectScale;
                        scaleY = kObjectScale;
                        scaleZ = kObjectScale;
                        rotate = 0.0f;
                    }
                }

                // Apply local transformation to our model
                if (mCordovaVuforiaRef.get().mType == 0) {
                    Matrix.translateM(mMatrix, 0, posX, posY, posZ);
                    Matrix.scaleM(mMatrix, 0, scaleX, scaleY, scaleZ);
                    Matrix.rotateM(mMatrix, 0, 90.0f + mRotate + rotate, 0.0f, 0.0f, 1.0f);
                } else if (mCordovaVuforiaRef.get().mType == 1) {
                    Matrix.translateM(mMatrix, 0, posX, posY, posZ);
                    Matrix.scaleM(mMatrix, 0, scaleX, scaleY, scaleZ);
                    Matrix.rotateM(mMatrix, 0, 90.0f + mRotate + rotate, 0.0f, 0.0f, 1.0f);
                }

                // Combine device pose (view matrix) with model matrix
                Matrix.multiplyMM(mMatrix, 0, viewMatrix, 0, mMatrix, 0);

                // Do the final combination with the projection matrix
                Matrix.multiplyMM(mMVPMatrix, 0, projectionMatrix, 0, mMatrix, 0);

                mTrackableResult = true;
                mTextureIndex = textureIndex;

                Utils.checkGLError("UserDefinedTargets renderFrame");
            }
        }

        if (mTrackableResult && mTrackableState) {
            mTrackableResult = false;
            mCordovaVuforiaRef.get().imageFound("", 0);
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        Renderer.getInstance().end();

    }

    public void updateRenderingPrimitives() {

        mAppRenderer.updateRenderingPrimitives();

    }

    public void updateImageInfo(VuforiaImageInfo imageInfo) {

        mImageInfo = imageInfo;

    }

    public static int getTextureIndex() {

        return mTextureIndex;

    }

    public static float[] getMVPMatrix() {

        return mMVPMatrix;

    }

    public synchronized static boolean getTrackableResult() {

        return mTrackableResult;

    }

}

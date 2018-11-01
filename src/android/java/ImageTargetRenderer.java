/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

package com.hopenrun.cordova.vuforia;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.Log;
import com.hopenrun.cordova.vuforia.utils.SampleMath;
import com.hopenrun.cordova.vuforia.utils.SampleUtils;
import com.hopenrun.cordova.vuforia.utils.Texture;
import com.hopenrun.cordova.vuforia.utils.VuforiaImageInfo;
import com.vuforia.Device;
import com.vuforia.ImageTargetResult;
import com.vuforia.Matrix44F;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.Trackable;
import com.vuforia.TrackableResult;
import com.vuforia.TrackableResultList;
import com.vuforia.TrackerManager;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.Vuforia;

import com.hopenrun.cordova.vuforia.utils.LoadingDialogHandler;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

// The renderer class for the ImageTargets sample.
public class ImageTargetRenderer implements GLSurfaceView.Renderer, AppRendererControl {

    private static final String LOGTAG = "ImageTargetRenderer";

    private ApplicationSession vuforiaAppSession;
    private final WeakReference<ImageTargets> mImageTargetsRef;
    private final AppRenderer mAppRenderer;

    /** buffer holding the texture coordinates */
    private FloatBuffer textureBuffer;

    /** buffer holding the vertices */
    private FloatBuffer vertexBuffer;

    private static final float BUILDING_SCALE = 0.012f;

    private Vector<Texture> mTextures;
    private boolean mTexturesUpdateTag = false;

    private boolean mIsActive = false;

    private VuforiaImageInfo mImageInfo;

    private float texture[] = {
            // Mapping coordinates for the vertices
            0.0f, 1.0f, // top left (V2)
            0.0f, 0.0f, // bottom left (V1)
            1.0f, 1.0f, // top right (V4)
            1.0f, 0.0f // bottom right (V3)
    };

    private float vertices[] = {
            -1.0f, -1.0f, 0.0f, // V1 - bottom left
            -1.0f, 1.0f, 0.0f, // V2 - top left
            1.0f, -1.0f, 0.0f, // V3 - bottom right
            1.0f, 1.0f, 0.0f // V4 - top right
    };

    public ImageTargetRenderer(Activity activity, ImageTargets imageTargets, ApplicationSession session, VuforiaImageInfo imageInfo) {

        mImageTargetsRef = new WeakReference<ImageTargets>(imageTargets);
        vuforiaAppSession = session;
        mImageInfo = imageInfo;

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        vertexBuffer = byteBuffer.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        textureBuffer = byteBuffer.asFloatBuffer();
        textureBuffer.put(texture);
        textureBuffer.position(0);

        // SampleAppRenderer used to encapsulate the use of RenderingPrimitives setting
        // the device mode AR/VR and stereo mode
        mAppRenderer = new AppRenderer(this, activity, Device.MODE.MODE_AR, false, 0.01f , 5f);

    }

    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl) {

        if (!mIsActive) return;

        // Call our function to render content
        mAppRenderer.render();

    }

    public void updateRenderingPrimitives() {

        mAppRenderer.updateRenderingPrimitives();

    }

    public void setActive(boolean active) {

        mIsActive = active;

        if (mIsActive)
            mAppRenderer.configureVideoBackground();

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

    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call function to update rendering when render surface
        // parameters have changed:
        mImageTargetsRef.get().updateRendering();

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);

        // RenderingPrimitives to be updated when some rendering change is done
        mAppRenderer.onConfigurationChanged(mIsActive);

        // Viewport
        GLES10.glMatrixMode(GL10.GL_PROJECTION); // Select The Projection Matrix
        GLES10.glLoadIdentity(); // Reset The Projection Matrix

        // Calculate The Aspect Ratio Of The Window
        GLU.gluPerspective(gl, 45.0f, (float) width / (float) height, 0.1f, 100.0f);

        GLES10.glMatrixMode(GL10.GL_MODELVIEW); // Select The Modelview Matrix
        GLES10.glLoadIdentity();

        initRendering();

    }

    // Function for initializing the renderer.
    private void initRendering() {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);

        GLES10.glEnable(GL10.GL_TEXTURE_2D); // Enable Texture Mapping ( NEW )
        GLES10.glShadeModel(GL10.GL_SMOOTH); // Enable Smooth Shading
        GLES10.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // Black Background
        GLES10.glClearDepthf(1.0f); // Depth Buffer Setup
        GLES10.glEnable(GL10.GL_DEPTH_TEST); // Enables Depth Testing
        GLES10.glDepthFunc(GL10.GL_LEQUAL); // The Type Of Depth Testing To Do

        GLES10.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);

    }

    public void updateConfiguration() {

        mAppRenderer.onConfigurationChanged(mIsActive);

    }

    // The render function.
    public void renderFrame(State state, float[] projectionMatrix) {

        // Renders video background replacing Renderer.DrawVideoBackground()
        mAppRenderer.renderVideoBackground(state);

        // Set the device pose matrix as identity
        Matrix44F devicePoseMattix = SampleMath.Matrix44FIdentity();
        Matrix44F modelMatrix;

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // Render the RefFree UI elements depending on the current state
        mImageTargetsRef.get().render();

        // Read device pose from the state and create a corresponding view matrix (inverse of the device pose)
        if (state.getDeviceTrackableResult() != null && state.getDeviceTrackableResult().getStatus() != TrackableResult.STATUS.NO_POSE) {
            modelMatrix = Tool.convertPose2GLMatrix(state.getDeviceTrackableResult().getPose());

            // We transpose here because Matrix44FInverse returns a transposed matrix
            devicePoseMattix = SampleMath.Matrix44FTranspose(SampleMath.Matrix44FInverse(modelMatrix));
        }

        // Did we find any trackables this frame?
        TrackableResultList trackableResultList = state.getTrackableResults();
        for (TrackableResult result : trackableResultList) {
            Trackable trackable = result.getTrackable();

            if (result.isOfType(ImageTargetResult.getClassType())) {
                int textureIndex = 0;
                modelMatrix = Tool.convertPose2GLMatrix(result.getPose());

                /**
                 * Our targets array has been flattened to a string so will equal something like: ["one", "two"]
                 * So, to stop weak matches such as 'two' within ["onetwothree", "two"] we wrap the term in
                 * speech marks such as '"two"'
                 **/
                for (int i = 0; i < mImageInfo.current; i++) {
                    if (trackable.getName().equalsIgnoreCase(mImageInfo.info[i].imageName)) {
                        textureIndex = i;
                        break;
                    }
                }

                renderModel(projectionMatrix, devicePoseMattix.getData(), modelMatrix.getData(), textureIndex, trackable.getName());

                SampleUtils.checkGLError("Image Targets renderFrame");
            }
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

    }

    private void renderModel(float[] projectionMatrix, float[] viewMatrix, float[] modelMatrix, int textureIndex, String obj_name) {

        float[] modelViewProjection = new float[16];

        if (mTexturesUpdateTag) {
            mTexturesUpdateTag = false;

            for (Texture t : mTextures) {
                GLES20.glGenTextures(1, t.mTextureID, 0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, t.mWidth, t.mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, t.mData);
            }
        }

        if (textureIndex < 0) {
            mImageTargetsRef.get().imageFound(obj_name, 0);
            return;
        }

        mImageTargetsRef.get().imageFound(obj_name, 1);

        // Apply local transformation to our model
        if (mImageTargetsRef.get().isDeviceTrackingActive()) {
            Matrix.translateM(modelMatrix, 0, 0, -0.06f, 0);
            Matrix.rotateM(modelMatrix, 0, 90.0f, 1.0f, 0, 0);
            Matrix.scaleM(modelMatrix, 0, BUILDING_SCALE, BUILDING_SCALE, BUILDING_SCALE);
        }

        // Combine device pose (view matrix) with model matrix
        Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0);

        // Do the final combination with the projection matrix
        Matrix.multiplyMM(modelViewProjection, 0, projectionMatrix, 0, modelMatrix, 0);

        GLES10.glLoadIdentity();
        GLES10.glTranslatef(0.0f, 0.0f, -5.0f); // move 5 units INTO the screen is

        // bind the previously generated texture
        GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);

        // Point to our buffers
        GLES10.glEnableClientState(GLES10.GL_VERTEX_ARRAY);
        GLES10.glEnableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);

        // Set the face rotation
        GLES10.glFrontFace(GLES10.GL_CW);

        // Point to our vertex buffer
        GLES10.glVertexPointer(3, GLES10.GL_FLOAT, 0, vertexBuffer);
        GLES10.glTexCoordPointer(2, GLES10.GL_FLOAT, 0, textureBuffer);

        // Draw the vertices as triangle strip
        GLES10.glDrawArrays(GLES10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);

        // Disable the client state before leaving
        GLES10.glDisableClientState(GLES10.GL_VERTEX_ARRAY);
        GLES10.glDisableClientState(GLES10.GL_TEXTURE_COORD_ARRAY);

    }

    public void setTextures(Vector<Texture> textures) {

        mTextures = textures;

        mTexturesUpdateTag = true;

    }

    public void updateImageInfo(VuforiaImageInfo imageInfo) {

        mImageInfo = imageInfo;

    }

}

/*
 *  ARRenderer.java
 *  ARToolKit5
 *
 *  This file is part of ARToolKit.
 *
 *  ARToolKit is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  ARToolKit is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with ARToolKit.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  As a special exception, the copyright holders of this library give you
 *  permission to link this library with independent modules to produce an
 *  executable, regardless of the license terms of these independent modules, and to
 *  copy and distribute the resulting executable under terms of your choice,
 *  provided that you also meet, for each linked independent module, the terms and
 *  conditions of the license of that module. An independent module is a module
 *  which is neither derived from nor based on this library. If you modify this
 *  library, you may extend this exception to your version of the library, but you
 *  are not obligated to do so. If you do not wish to do so, delete this exception
 *  statement from your version.
 *
 *  Copyright 2015 Daqri, LLC.
 *  Copyright 2011-2015 ARToolworks, Inc.
 *
 *  Author(s): Julian Looser, Philip Lamb
 *
 */

package com.hoperun.cordova.vuforia.utils;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import com.hoperun.cordova.vuforia.ARVideoRenderer;
import com.hoperun.cordova.vuforia.LoadOBJAPI;

import java.nio.FloatBuffer;
import java.util.Vector;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class ARModelRenderer implements GLSurfaceView.Renderer {

    private final static String LOGTAG = "ARModelRenderer";

    private int mProgram;
    private int glHPosition;
    private int glHCoordinate;
    private int glHTexture;
    private int glHMatrix;

    private Vector<Texture> mTextures;

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;

    private boolean mTexturesUpdateTag = false;
    private boolean mUpdateDisplayTag = false;
    private boolean mClearTexturesTag = false;

    private static final String VERTEX_SHADER =
            "attribute vec4 vPosition;                 \n"+
            "attribute vec2 vCoordinate;               \n"+
            "uniform mat4 uMVPMatrix;                  \n"+
            "varying vec2 aCoordinate;                 \n"+
            "void main() {                             \n"+
            "   gl_Position = uMVPMatrix * vPosition;  \n"+
            "   aCoordinate = vCoordinate;             \n"+
            "}                                         \n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;                             \n"+
            "uniform sampler2D vTexture;                          \n"+
            "varying vec2 aCoordinate;                            \n"+
            "void main() {                                        \n"+
            "    vec4 nColor = texture2D(vTexture, aCoordinate);  \n"+
            "    gl_FragColor = nColor;                           \n"+
            "}                                                    \n";

    private final float[] vertices = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f,
    };

    private final float[] texture = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    public ARModelRenderer(Context context) {

        vertexBuffer = GlUtil.createFloatBuffer(vertices);
        textureBuffer = GlUtil.createFloatBuffer(texture);

    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        // Transparent background
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.f);

        GLES20.glEnable(GLES20.GL_TEXTURE_2D);

        mProgram = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        glHPosition = GLES20.glGetAttribLocation(mProgram,"vPosition");
        glHCoordinate = GLES20.glGetAttribLocation(mProgram,"vCoordinate");
        glHTexture = GLES20.glGetUniformLocation(mProgram,"vTexture");
        glHMatrix = GLES20.glGetUniformLocation(mProgram,"uMVPMatrix");

        LoadOBJAPI.getInstance().arwSurfaceCreatedModle();

    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        GLES20.glViewport(0, 0, width, height);

        LoadOBJAPI.getInstance().arwSurfaceChangedModle(width, height);

    }

    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        if (mTexturesUpdateTag) {
            mTexturesUpdateTag = false;
            mUpdateDisplayTag = true;

            initRendering();
        }

        if (mClearTexturesTag) {
            mUpdateDisplayTag = false;
            mClearTexturesTag = false;

            if (mTextures != null) mTextures.clear();

            LoadOBJAPI.getInstance().arwSurfaceDestroyedModle();
        }

        if (mUpdateDisplayTag) {
            renderModel(ARVideoRenderer.getMVPMatrix(), ARVideoRenderer.getTextureIndex());
        }

    }

    public void setTextures(Vector<Texture> textures) {

        mTextures = textures;

        mTexturesUpdateTag = true;

    }

    public void clearTextures() {

        mClearTexturesTag = true;

    }

    private void initRendering() {

        Log.d(LOGTAG, "initRendering");

        for (Texture t : mTextures) {
            if (t.mModelPath == null) {
                // 生成纹理
                GLES20.glGenTextures(1, t.mTextureID, 0);

                // 生成纹理
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);

                //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);

                //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

                //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);

                //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
                GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

                //根据以上指定的参数，生成一个2D纹理
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, t.mWidth, t.mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, t.mData);
            } else {
                LoadOBJAPI.getInstance().arwAddModle(new String(t.mModelPath));
            }
        }

    }

    private void renderModel(float[] matrix, int textureIndex) {

        if (ARVideoRenderer.getTrackableResult() && textureIndex >= 0) {
            if (mTextures.get(textureIndex).mModelPath == null) {

                GLES20.glUseProgram(mProgram);

                GLES20.glUniformMatrix4fv(glHMatrix, 1, false, matrix, 0);

                GLES20.glEnableVertexAttribArray(glHPosition);
                GLES20.glEnableVertexAttribArray(glHCoordinate);

                GLES20.glUniform1i(glHTexture, 0);

                GLES20.glVertexAttribPointer(glHPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
                GLES20.glVertexAttribPointer(glHCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures.get(textureIndex).mTextureID[0]);

                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            } else {
                LoadOBJAPI.getInstance().arwDrawFrameModle(matrix, textureIndex);
            }
        }

    }

}

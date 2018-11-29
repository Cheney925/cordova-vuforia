/*===============================================================================
Copyright (c) 2016-2018 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.hoperun.cordova.vuforia.utils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.os.Environment;
import android.util.Log;

import com.vuforia.Image;

import org.obj2opengl.v3.Obj2OpenGL;
import org.obj2opengl.v3.model.OpenGLModelData;
import org.obj2opengl.v3.model.RawOpenGLModel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Support class for the Vuforia sample applications
 * Exposes functionality for loading a texture from the APK.
 */
public class Texture {

    private static final String LOGTAG = "Vuforia_Texture";

    public final int[] mTextureID = new int[1];
    public int mWidth;               // The width of the texture.
    public int mHeight;              // The height of the texture.
    public ByteBuffer mData = null;  // The pixel data.
    public Bitmap mBitmap = null;
    private int mChannels;           // The number of channels.

    public int mVertexCount;

    public FloatBuffer mVertexBuffer;
    public FloatBuffer mTexureBuffer;
    public FloatBuffer mNormalBuffer;

    public String mFileName = null;

    /** buffer holding the texture coordinates */
    private FloatBuffer textureBuffer;

    /** buffer holding the vertices */
    private FloatBuffer vertexBuffer;

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

    public Texture() {

        vertexBuffer = GlUtil.createFloatBuffer(vertices);
        textureBuffer = GlUtil.createFloatBuffer(texture);

    }

    public void draw() {

        // bind the previously generated texture
        GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, mTextureID[0]);

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

    public static Texture loadTextureFromApk(String fileName, AssetManager assets) {

        Bitmap bitMap = null;
        InputStream inputStream;

        try {
            if (fileName.endsWith(".obj")) {
                inputStream = assets.open(fileName, AssetManager.ACCESS_BUFFER);

                RawOpenGLModel openGLModel = new Obj2OpenGL().convert(inputStream);
                OpenGLModelData modelData = openGLModel.normalize().center().getDataForGLDrawArrays();

                String name = new String(fileName);

                try {
                    InputStream stream = assets.open(name.replace(".obj", ".png"), AssetManager.ACCESS_BUFFER);
                    BufferedInputStream bufferedStream = new BufferedInputStream(stream);
                    bitMap = BitmapFactory.decodeStream(bufferedStream);
                } catch (IOException e) {}

                return loadTextureFromObj(fileName, modelData, bitMap);
            } else {
                inputStream = assets.open(fileName, AssetManager.ACCESS_BUFFER);

                BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
                bitMap = BitmapFactory.decodeStream(bufferedStream);

                return loadTextureFromBitmap(fileName, bitMap);
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to log texture '" + fileName + "' from APK");
            Log.i(LOGTAG, e.getMessage());
            return null;
        }

    }

    /* Factory function to load a texture from the APK. */
    public static Texture loadTextureFromApk(String fileName) {

        Bitmap bitMap = null;

        try {
            if (fileName.endsWith(".obj")) {
                RawOpenGLModel openGLModel = new Obj2OpenGL().convert(new FileInputStream(fileName));
                OpenGLModelData modelData = openGLModel.normalize().center().getDataForGLDrawArrays();

                String name = new String(fileName);

                try {
                    BufferedInputStream bufferedStream = new BufferedInputStream(new FileInputStream(name.replace(".obj", ".png")));
                    bitMap = BitmapFactory.decodeStream(bufferedStream);
                } catch (IOException e) {}

                return loadTextureFromObj(fileName, modelData, bitMap);
            } else {
                BufferedInputStream bufferedStream = new BufferedInputStream(new FileInputStream(fileName));
                bitMap = BitmapFactory.decodeStream(bufferedStream);

                return loadTextureFromBitmap(fileName, bitMap);
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to log texture '" + fileName + "' from APK");
            Log.i(LOGTAG, e.getMessage());
            return null;
        }

    }

    private static Texture loadTextureFromObj(String fileName, OpenGLModelData modelData, Bitmap bitmap) {

        Texture texture = new Texture();

        texture.mFileName = fileName;
        texture.mBitmap = bitmap;

        float vertices[] = modelData.getVertices();

        texture.mVertexCount = vertices.length / 3;

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        texture.mVertexBuffer = vbb.asFloatBuffer();
        texture.mVertexBuffer.put(vertices);
        texture.mVertexBuffer.position(0);

        float texures[] = modelData.getTextureCoordinates();

        ByteBuffer vbb2 = ByteBuffer.allocateDirect(texures.length * 4);
        vbb2.order(ByteOrder.nativeOrder());

        texture.mTexureBuffer = vbb2.asFloatBuffer();
        texture.mTexureBuffer.put(texures);
        texture.mTexureBuffer.position(0);

        float normals[] = modelData.getNormals();

        ByteBuffer vbb3 = ByteBuffer.allocateDirect(normals.length * 4);
        vbb3.order(ByteOrder.nativeOrder());

        texture.mNormalBuffer = vbb3.asFloatBuffer();
        texture.mNormalBuffer.put(normals);
        texture.mNormalBuffer.position(0);

        return texture;

    }

    private static Texture loadTextureFromBitmap(String fileName, Bitmap bitmap) {

        Texture texture = new Texture();

        texture.mFileName = fileName;
        texture.mBitmap = bitmap;

        return texture;

    }

    private static Texture loadTextureFromIntBuffer(String fileName, int[] data, int width, int height) {

        // Convert:
        int numPixels = width * height;
        byte[] dataBytes = new byte[numPixels * 4];
        
        for (int p = 0; p < numPixels; ++p) {
            int colour = data[p];
            dataBytes[p * 4] = (byte) (colour >>> 16); // R
            dataBytes[p * 4 + 1] = (byte) (colour >>> 8); // G
            dataBytes[p * 4 + 2] = (byte) colour; // B
            dataBytes[p * 4 + 3] = (byte) (colour >>> 24); // A
        }

        Texture texture = new Texture();
        texture.mWidth = width;
        texture.mHeight = height;
        texture.mChannels = 4;

        texture.mFileName = fileName;

        texture.mData = ByteBuffer.allocateDirect(dataBytes.length).order(ByteOrder.nativeOrder());
        int rowSize = texture.mWidth * texture.mChannels;
        for (int r = 0; r < texture.mHeight; r++)
            texture.mData.put(dataBytes, rowSize * (texture.mHeight - 1 - r), rowSize);
        
        texture.mData.rewind();

        return texture;

    }

    public static Texture loadTextureFromImage(Image image) {

        Texture texture = new Texture();
        texture.mWidth = image.getWidth();
        texture.mHeight = image.getHeight();
        texture.mChannels = 4;
        texture.mData = image.getPixels();

        texture.mData.rewind();

        return texture;

    }

}

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

    public String mModelPath = null;

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

        InputStream inputStream;

        try {
            if (fileName.contains(".obj")) {
                Texture texture = new Texture();

                texture.mModelPath = fileName;

                return texture;
            } else {
                inputStream = assets.open(fileName, AssetManager.ACCESS_BUFFER);

                BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
                Bitmap bitMap = BitmapFactory.decodeStream(bufferedStream);

                int[] data = new int[bitMap.getWidth() * bitMap.getHeight()];
                bitMap.getPixels(data, 0, bitMap.getWidth(), 0, 0, bitMap.getWidth(), bitMap.getHeight());

                return loadTextureFromIntBuffer(bitMap, data, bitMap.getWidth(), bitMap.getHeight());
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to log texture '" + fileName + "' from APK");
            Log.i(LOGTAG, e.getMessage());
            return null;
        }

    }

    /* Factory function to load a texture from the APK. */
    public static Texture loadTextureFromApk(String fileName) {

        try {
            if (fileName.equalsIgnoreCase(".obj")) {
                Texture texture = new Texture();

                texture.mModelPath = fileName;

                return texture;
            } else {
                BufferedInputStream bufferedStream = new BufferedInputStream(new FileInputStream(fileName));
                Bitmap bitMap = BitmapFactory.decodeStream(bufferedStream);

                int[] data = new int[bitMap.getWidth() * bitMap.getHeight()];
                bitMap.getPixels(data, 0, bitMap.getWidth(), 0, 0, bitMap.getWidth(), bitMap.getHeight());

                return loadTextureFromIntBuffer(bitMap, data, bitMap.getWidth(), bitMap.getHeight());
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to log texture '" + fileName + "' from APK");
            Log.i(LOGTAG, e.getMessage());
            return null;
        }

    }

    private static Texture loadTextureFromIntBuffer(Bitmap bitmap, int[] data, int width, int height) {

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

        texture.mBitmap = bitmap;

        texture.mData = ByteBuffer.allocateDirect(dataBytes.length).order(ByteOrder.nativeOrder());
        int rowSize = texture.mWidth * texture.mChannels;
        for (int r = 0; r < texture.mHeight; r++)
            texture.mData.put(dataBytes, rowSize * (texture.mHeight - 1 - r), rowSize);
        
        texture.mData.rewind();

        texture.mModelPath = null;

        return texture;

    }

    public static Texture loadTextureFromImage(Image image) {

        Texture texture = new Texture();
        texture.mWidth = image.getWidth();
        texture.mHeight = image.getHeight();
        texture.mChannels = 4;
        texture.mData = image.getPixels();

        texture.mData.rewind();

        texture.mModelPath = null;

        return texture;

    }

}

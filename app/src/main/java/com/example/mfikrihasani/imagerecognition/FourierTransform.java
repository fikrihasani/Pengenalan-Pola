package com.example.mfikrihasani.imagerecognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.example.mfikrihasani.imagerecognition.Control.FFTRosettaCode;
import com.example.mfikrihasani.imagerecognition.Control.PublicUsage;

import java.io.IOException;

import static com.example.mfikrihasani.imagerecognition.ThinningActivity.PICK_IMAGE;
import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.hypot;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class FourierTransform extends AppCompatActivity {
    ImageView img;
    Uri imageURI;
    Bitmap bitmap, scaledBitmap, processedImg;
    int width;
    int height;
    int[][] g; /** original image */
    double[][] GReal; /** Fourier real component */
    double[][] GImaginer; /** Fourier imaginary component */
    double[][] GMagnitude; /** Fourier Magnitude component*/
    double[][] GAngle; /** Fourier Phase component */
    double[][] gReal; /** Inverse Fourier real component */
    double[][] gImaginer; /** Inverse Fourier imaginary component */
    double[][] gMagnitude; /** InverseFourier Magnitude component*/
    double[][] gAngle; /** Inverse Fourier Phase component */
    Bitmap spatial;
    Bitmap freq;
    Bitmap freqR; //Real
    Bitmap freqI; //Imaginary
    Bitmap freqA; //angle
    Bitmap hat; //magnitude
    Bitmap hatR; //Real
    Bitmap hatI; //Imaginary
    Bitmap hatA; //angle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fourier_transform);
        img = findViewById(R.id.img);
    }

    public void openGallery(View view) {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        gallery.setType("image/*");
        startActivityForResult(gallery, PICK_IMAGE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1) {
            // Load Image File
            imageURI = data.getData();
            bitmap = null;
            scaledBitmap = null;
            processedImg = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageURI);
                if (bitmap.getWidth() > 2000 || bitmap.getHeight() > 2000) {
                    scaledBitmap = getResizedBitmap(bitmap, bitmap.getHeight(), bitmap.getWidth(), bitmap.getHeight() / 8, bitmap.getWidth() / 8);
                } else if (bitmap.getWidth() > 1000 || bitmap.getHeight() > 1000) {
                    scaledBitmap = getResizedBitmap(bitmap, bitmap.getHeight(), bitmap.getWidth(), bitmap.getHeight() / 6, bitmap.getWidth() / 6);
                } else if (bitmap.getHeight() > 800 || bitmap.getWidth() > 800) {
                    scaledBitmap = getResizedBitmap(bitmap, bitmap.getHeight(), bitmap.getWidth(), bitmap.getHeight() / 3, bitmap.getWidth() / 3);
                } else if (bitmap.getHeight() >= 500|| bitmap.getWidth() >= 500){
                    scaledBitmap = getResizedBitmap(bitmap, bitmap.getHeight(), bitmap.getWidth(), bitmap.getHeight() / 2, bitmap.getWidth() / 2);

                }
                Bitmap grayBit = grayscale(scaledBitmap);
                img.setImageBitmap(grayBit);

                //search face

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap grayscale(Bitmap bitmap){
        Bitmap grayBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        for(int i=0; i<height; i++){
            for(int j=0; j<width; j++){
                int color = bitmap.getPixel(j,i);
                int redVal = Color.red(color);
                int greenVal = Color.green(color);
                int blueVal = Color.blue(color);
                int bwVal = (redVal+greenVal+blueVal)/3;
                int bwColor = 0xFF000000 | (bwVal<<16 | bwVal<<8 | bwVal);

                grayBitmap.setPixel(j,i,bwColor);

            }
        }

        return grayBitmap;
    }
    public Bitmap getResizedBitmap(Bitmap bm, int height, int width, int newHeight, int newWidth) {
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // crate matrix for manipulation
        Matrix matrix = new Matrix();
//        // Resize the bitmap
        matrix.postScale(scaleWidth, scaleHeight);
//        // recreate the new bitmap;
        Bitmap newBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();

        return newBitmap;
    }

    public void doDFT(View view){
        spatial = ((BitmapDrawable) img.getDrawable()).getBitmap();
        freq = spatial.copy(Bitmap.Config.ARGB_8888, true); //magnitude
        freqR = spatial.copy(Bitmap.Config.ARGB_8888, true); //Real
        freqI = spatial.copy(Bitmap.Config.ARGB_8888, true); //Imaginary
        freqA = spatial.copy(Bitmap.Config.ARGB_8888, true); //angle
        hat = spatial.copy(Bitmap.Config.ARGB_8888, true); //magnitude
        hatR = spatial.copy(Bitmap.Config.ARGB_8888, true); //Real
        hatI = spatial.copy(Bitmap.Config.ARGB_8888, true); //Imaginary
        hatA = spatial.copy(Bitmap.Config.ARGB_8888, true); //angle
        width = spatial.getWidth();
        height = spatial.getHeight();
        g = new int[height][width]; /** original image */
        GReal = new double[height][width]; /** Fourier real component */
        GImaginer = new double[height][width]; /** Fourier imaginary component */
        GMagnitude = new double[height][width]; /** Fourier Magnitude component*/
        GAngle = new double[height][width]; /** Fourier Phase component */
        gReal = new double[height][width]; /** Inverse Fourier real component */
        gImaginer = new double[height][width]; /** Inverse Fourier imaginary component */
        gMagnitude = new double[height][width]; /** InverseFourier Magnitude component*/
        gAngle = new double[height][width]; /** Inverse Fourier Phase component */

        int midX = width/2;
        int midY = height/2;
        double r = 100;
        /** Storing Image into regular Array */
        for(int p=0; p<height; p++){
            for(int q=0; q<width; q++){
                int color = spatial.getPixel(q,p);
                int sVal = Color.red(color);
                g[p][q] = sVal;
            }
        }

        /** DFT */
        double cekIsInsideCirle = 0;
        for(int p=0; p<height; p++){
            for(int q=0; q<width; q++){

                for(int x=0; x<width; x++){
                    GReal[p][q] += g[p][x]*cos(2*PI*x*q/width);
                    GImaginer[p][q] += g[p][x]*sin(2*PI*x*q/width);
                }
                GReal[p][q] /= width; /** Scaling, only in Fourier not in inverse*/
                GImaginer[p][q] /= width;
                cekIsInsideCirle = sqrt(Math.pow(midX - q,2) + Math.pow(midY - p,2));
                if (cekIsInsideCirle > r){
                    GReal[p][q] = 0;
                    GImaginer[p][q] = 0;
                }

                //GMagnitude[p][q] = sqrt(GReal[p][q]*GReal[p][q] + GImaginer[p][q]*GImaginer[p][q]);
                //GMagnitude[p][q] = GReal[p][q]*GReal[p][q] + GImaginer[p][q]*GImaginer[p][q];
                GMagnitude[p][q] = hypot(GReal[p][q],GImaginer[p][q])*width;
                GAngle[p][q] = atan(GImaginer[p][q]/GReal[p][q]);

                int GVal = (int)GMagnitude[p][q];
                int GColor = 0xFF000000 | (GVal<<16 | GVal<<8 | GVal);
                freq.setPixel(q,p,GColor);
//
//                int GRVal = (int)GReal[p][q];
//                int GRColor = 0xFF000000 | (GRVal<<16 | GRVal<<8 | GRVal);
//                freqR.setPixel(q,p,GRColor);
//
//                int GIVal = (int)GImaginer[p][q];
//                int GIColor = 0xFF000000 | (GIVal<<16 | GIVal<<8 | GIVal);
//                freqI.setPixel(q,p,GIColor);

                int GAVal = (int)((GAngle[p][q] + PI/2)*255/PI); /** scaling for display */
                int GAColor = 0xFF000000 | (GAVal<<16 | GAVal<<8 | GAVal);
                freqA.setPixel(q,p,GAColor);
            }
        }
        ImageView imgFourier = findViewById(R.id.imgFourier);
        imgFourier.setImageBitmap(freq);
//
//        ImageView imgFourierReal = findViewById(R.id.imgFourierReal);
//        imgFourierReal.setImageBitmap(freqR);
//
//        ImageView imgFourierImaginer = findViewById(R.id.imgFourierImaginer);
//        imgFourierImaginer.setImageBitmap(freqI);

        ImageView imgFourierAngle = findViewById(R.id.imgFourierAngle);
        imgFourierAngle.setImageBitmap(freqA);

        /** Copy atas, ubah sinnya jadi minus, ingat Real = real*real - imag*imag dan Imag = real*imag + imag*real
         * Karena kan hasil fourier ada real dan imag
         * Ubah XML nya juga */
    }

    public void doIDFT(View view){
        /** Inverse DFT */
        for(int p=0; p<height; p++){
            for(int q=0; q<width; q++){

                for(int x=0; x<width; x++){
                    gReal[p][q] += GReal[p][x]*cos(2*PI*x*q/width) + GImaginer[p][x]*sin(2*PI*x*q/width);
                    gImaginer[p][q] += GImaginer[p][x]*cos(2*PI*x*q/width) - GReal[p][x]*sin(2*PI*x*q/width);
                }
                //gReal[p][q] /= width;
                //gImaginer[p][q] /= width;
                //GMagnitude[p][q] = sqrt(GReal[p][q]*GReal[p][q] + GImaginer[p][q]*GImaginer[p][q]);
                //GMagnitude[p][q] = GReal[p][q]*GReal[p][q] + GImaginer[p][q]*GImaginer[p][q];
                gMagnitude[p][q] = hypot(gReal[p][q],gImaginer[p][q]);
                gAngle[p][q] = atan(gImaginer[p][q]/gReal[p][q]);

                int gVal = (int)gMagnitude[p][q];
                int gColor = 0xFF000000 | (gVal<<16 | gVal<<8 | gVal);
                hat.setPixel(q,p,gColor);

//                int gRVal = (int)gReal[p][q];
//                int gRColor = 0xFF000000 | (gRVal<<16 | gRVal<<8 | gRVal);
//                hatR.setPixel(q,p,gRColor);
//
//                int gIVal = (int)gImaginer[p][q];
//                int gIColor = 0xFF000000 | (gIVal<<16 | gIVal<<8 | gIVal);
//                hatI.setPixel(q,p,gIColor);
//
//                int gAVal = (int)((gAngle[p][q] + PI/2)*255/PI); /** scaling for display */
//                int gAColor = 0xFF000000 | (gAVal<<16 | gAVal<<8 | gAVal);
//                hatA.setPixel(q,p,gAColor);
            }
        }
        ImageView imgInverseMagnitude = findViewById(R.id.imgInverseMagnitude);
        imgInverseMagnitude.setImageBitmap(hat);
//
//        ImageView imgInverseReal = findViewById(R.id.imgInverseReal);
//        imgInverseReal.setImageBitmap(hatR);
//
//        ImageView imgInverseImaginer = findViewById(R.id.imgInverseImaginer);
//        imgInverseImaginer.setImageBitmap(hatI);
//
//        ImageView imgInverseAngle = findViewById(R.id.imgInverseAngle);
//        imgInverseAngle.setImageBitmap(hatA);
    }

}

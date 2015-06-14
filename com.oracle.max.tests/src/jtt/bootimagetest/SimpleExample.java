package jtt.bootimagetest;

import java.io.*;
import java.util.*;

/**
 * Created by andyn on 12/06/15.
 * Simple example to demonstrate convolution of an image
 * Apologies, it's a bit dirty in places, hardcoded to use
 * lena.raw ... this needs to be a 512x512 image .. 
 * I used the scipy tools to extract the raw image ...
 * lena.raw needs to be in the maxine-tester/jtt-c1xc1x directory, see the python viewer.py
 * file in the same directory, it displays lena, then after quitting that window it 
 * displays the blurred image.
 *
 * The blurring uses a 5x5 gaussian filter to implement processing that is similar to the
 * bilateral_filter in the  SLAMBench sources.
 */

public class SimpleExample {

    private static final String INPUT_FILE_NAME = "lena.raw";
    private static final String OUTPUT_FILE_NAME = "lenablurred.raw";
    private boolean useDouble = false;
    private int imageSize;
    private int filterSize;
    private static float inputD[] = new float [262144];
    private static float outputD[] = new float [262144];
    private static float filterD[] = new float[262144];

    public SimpleExample() {
        useDouble = true;
	debugme();
    }
    public static  void debugme() {
	byte []x = new byte [2];
	x[0] = -1;
	x[1] = 1;
	inputD[0] = (int) x[0];
	inputD[1] = (int) x[1];
	
    }
    private void readImage() {
        byte[] bytes = read(INPUT_FILE_NAME);
        assert bytes.length == imageSize * imageSize;
        for (int i = 0; i < bytes.length; i++) {
	    int x;
	    x =  ((int)bytes[i]);
	    inputD[i] =(float)x;

	}
    }

    private void writeImage() {
        byte output[] = new byte[imageSize*imageSize];
        for(int i = 0; i < imageSize*imageSize; i++) {
	    int x = (int) outputD[i];
            output[i] = (byte) x;
        }
        write(output, OUTPUT_FILE_NAME);
    }

    /**
     * Write a byte array to the given file.
     * Writing binary data is significantly simpler than reading it.
     */
    private void write(byte[] aInput, String aOutputFileName) {
        try {
            OutputStream output = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream(aOutputFileName));
                output.write(aInput);
            } finally {
                output.close();
            }
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    byte[] read(String aInputFileName) {

        File file = new File(aInputFileName);
        byte[] result = new byte[(int) file.length()];
        try {
            InputStream input = null;
            try {
                int totalBytesRead = 0;
                input = new BufferedInputStream(new FileInputStream(file));
                while (totalBytesRead < result.length) {
                    int bytesRemaining = result.length - totalBytesRead;
                    //input.read() returns -1, 0, or more :
                    int bytesRead = input.read(result, totalBytesRead, bytesRemaining);
                    if (bytesRead > 0) {
                        totalBytesRead = totalBytesRead + bytesRead;
                    }
                }
        /*
         the above style is a bit tricky: it places bytes into the 'result' array; 
         'result' is an output parameter;
         the while loop usually has a single iteration only.
        */
            } finally {
                input.close();
            }
        } catch (FileNotFoundException ex) {
            System.err.println("FILE NOT FOUND!!!!");
            System.err.println(ex);
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return result;
    }

    public static void convolveD(float[] input, float[] filter,
                                 float[] output, int iW, int iH, int fW,
                                 int fH) {
        int x, y, u, v;

        final int filterX2 = fW / 2;
        int filterY2 = fH / 2;

        for (y = 0; y < iH; y++) {
            // int yLeft = y;
            for (x = 0; x < iW; x++) {
                float sum = 0.0f;

                int pos = x + y * iW; // point to be generated


                for (v = -filterX2; v <= filterX2; v++) {
                    for (u = -filterY2; u <= filterY2; u++) {

                        // border condition
                        int deltax, deltay;
                        deltax = x;
                        deltay = y;
                        if ((x - fW) < 0) deltax = x + fW;
                        if ((y - fH) < 0) deltay = y + fH;
                        if ((x + fW) > iW - 1) deltax = iW - fW;
                        if ((y + fH) > iH - 1) deltay = iH - fH;


                        // filter
                        sum += filter[(v + filterX2) * fH + u + filterY2]
                                * input[((deltay - filterY2 + v) * iW)
                                + (deltax - filterX2 + u)];

                    }
                }

                output[(y * iW) + x] = sum;
            }
        }
    }


    private static void createImage(float[] image, int width, int height) {
        Random rand = new Random();
        for (int x = 0; x < height; x++)
            for (int y = 0; y < width; y++)
                image[(y * width) + x] = rand.nextInt(256);
    }

    private static void createFilter(float[] filter, int width, int height) {
	// set standard deviation to 1.0
        float sigma = 1.0f;
        float r, s = 2.0f * sigma * sigma;
        int fw = (width - 1) / 2;
        int fh = (height - 1) / 2;

        // sum is for normalization
        float sum = 0.0f;

        // generate 5x5 kernel
        for (int x = -fw; x <= fw; x++) {
            for (int y = -fh; y <= fh; y++) {
                r = (float)Math.sqrt(x * x + y * y);
                filter[width * (x + 2) + y + 2] = ((float)Math.exp(-(r * r) / s)) / (3.14159f * s);
                sum += filter[width * (x + 2) + y + 2];
            }
        }
        // normalize the Kernel
        for (int i = 0; i < 5; ++i)
            for (int j = 0; j < 5; ++j) {
                filter[i * 5 + j] /= sum;
	    }
    }


    protected void parseCommandLine(int args) {
        imageSize = 512;
        filterSize = 5;
        if (args == 0) {
            useDouble = true;
        } else {
            useDouble = true;
        }
    }

    protected void setupBenchmark() {

        readImage();
        createFilter(filterD, filterSize, filterSize);



    }

    protected void runBenchmark() {
        convolveD(inputD, filterD, outputD, imageSize, imageSize,
                filterSize, filterSize);
        writeImage();
    }

    protected void printResults() {
        int row;
        int col;
        for (int i = 0; i < imageSize * imageSize; i++) {
            row = i / imageSize;
            col = i % imageSize;
        }
    }

    public static void main(String[] args) {

        SimpleExample bm = new SimpleExample();

        bm.parseCommandLine(args.length);
        try {
            bm.setupBenchmark();

            bm.runBenchmark();
            bm.printResults();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static int test(int args) {

        SimpleExample bm = new SimpleExample();

        bm.parseCommandLine(args);
        try {
            bm.setupBenchmark();

            bm.runBenchmark();
            bm.printResults();

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;

    }
}

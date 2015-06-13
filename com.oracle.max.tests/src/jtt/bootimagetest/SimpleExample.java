package jtt.bootimagetest;

import java.io.*;
import java.util.*;

/**
 * Created by andyn on 12/06/15.
 * Simple example to demonstrate convolution of an image
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
    }

    private void readImage() {
        byte[] bytes = read(INPUT_FILE_NAME);
        assert bytes.length == imageSize * imageSize;
        for (int i = 0; i < bytes.length; i++) {
	    int x;
	    float y;
            inputD[i] = (float)((int) bytes[i]);
	    System.out.println((int)inputD[i]);
	    System.out.println(bytes[i]);
	    x =  ((int)bytes[i]);
	    inputD[i] =(float)x;
	    y = inputD[i];

	    com.sun.max.vm.Log.println("from array" + (int)inputD[i]);
            System.out.println("from scalr " + (int)x + " " + y);
            com.sun.max.vm.Log.println("fLOGrom scalr " + (int)x + " " + y);
	}
    }

    private void writeImage() {
        byte output[] = new byte[imageSize*imageSize];
	com.sun.max.vm.Log.println("OUTPUTS");
        for(int i = 0; i < imageSize*imageSize; i++) {
	    com.sun.max.vm.Log.println(outputD[i]);
            output[i] = (byte) outputD[i];
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
			//System.out.println("FILTER "+ ((v + filterX2) * fH + u + filterY2) + "X "+ (deltax-filterX2+u)     + " Y " +  (deltay - filterY2 + v));

                    }
                }

                // int outIndex = y * outputImageWidth + x;
                output[(y * iW) + x] = sum;
                //output[(x * iW) + y] = input[(x*iW)+y];
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
	com.sun.max.vm.Log.println("FILTER");
        // normalize the Kernel
        for (int i = 0; i < 5; ++i)
            for (int j = 0; j < 5; ++j) {
                filter[i * 5 + j] /= sum;
		com.sun.max.vm.Log.println((int)(100.0*filter[i*5+j]));
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
        //imageSize = Integer.parseInt(line.getOptionValue("image-size", "2048"));
        //filterSize = Integer.parseInt(line.getOptionValue("filter-size", "5"));
    }

    protected void setupBenchmark() {

        //inputD = new float[imageSize * imageSize];
        //outputD = new float[imageSize * imageSize];
        //filterD = new float[filterSize * filterSize];
	System.out.println("ARRAYS CREATED");
        readImage();
	System.out.println("READ IMAGE");
        //¬createImage(inputD, imageSize, imageSize);
        createFilter(filterD, filterSize, filterSize);
	System.out.println("FILTER CREATED");



    }

    protected void runBenchmark() {
        convolveD(inputD, filterD, outputD, imageSize, imageSize,
                filterSize, filterSize);
	System.out.println("CONVOLVED");
        writeImage();
	System.out.println("WRITTEN IMAGE");
    }

    protected void printResults() {
        int row;
        int col;
        for (int i = 0; i < imageSize * imageSize; i++) {
            row = i / imageSize;
            col = i % imageSize;
            //System.out.print(row + " " + col);
            //System.out.println(" " + outputD[i]);
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
/*
package jtt.bootimagetest;

import java.util.Random;


public class SimpleExample  {

    private boolean     useDouble = false;

    private int			imageSize;
    private int			filterSize;

    private float[]		inputF;
    private float[]		outputF;
    private float[]		outputFR;
    private float[]		filterF;

    private float[]	inputD;
    private float[]	outputD;
    private float[]	outputDR;
    private float[]	filterD;

    public SimpleExample() {
        useDouble = true;
    }
    public static void convolveF(float[] input,  float[] filter,
                                 float[] output,  int iW,  int iH,  int fW,
                                 int fH) {
        int x, y, u, v;

        final int filterX2 = fW / 2;
        int filterY2 = fH / 2;

        for (y = 0; y < iH; y++) {
            // int yLeft = y;
            for (x = 0; x < iW; x++) {
                float sum = 0.0f;
                for (v = 0; v < fH; v++) {
                    for (u = 0; u < fW; u++) {

                        if (y - filterY2 + v >= 0 && y + v < iH) {
                            if (x - filterX2 + u >= 0 && x + u < iW) {
                                sum += filter[(v * fW) + u]
                                        * input[((y - filterY2 + v) * iW)
                                        + (x - filterX2 + u)];
                            }
                        }
                    }
                }

                // int outIndex = y * outputImageWidth + x;
                output[(y * iW) + x] = sum;
            }
        }
    }


    public static void convolveD(float[] input, float[] filter,
                                 float[] output,  int iW,  int iH,  int fW,
                                 int fH) {
        int x, y, u, v;

        final int filterX2 = fW / 2;
        int filterY2 = fH / 2;

        for (y = 0; y < iH; y++) {
            // int yLeft = y;
            for (x = 0; x < iW; x++) {
                float sum = 0.0f;
                for (v = 0; v < fH; v++) {
                    for (u = 0; u < fW; u++) {

                        if (y - filterY2 + v >= 0 && y + v < iH) {
                            if (x - filterX2 + u >= 0 && x + u < iW) {
                                sum += filter[(v * fW) + u]
                                        * input[((y - filterY2 + v) * iW)
                                        + (x - filterX2 + u)];
                            }
                        }
                    }
                }

                // int outIndex = y * outputImageWidth + x;
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
        float filterSum = 0.0f;
        Random rand = new Random();

        for (int x = 0; x < height; x++)
            for (int y = 0; y < width; y++) {
                float f = rand.nextFloat();
                filterSum += f;
                filter[(y * width) + x] = f;
            }

        for (int x = 0; x < height; x++)
            for (int y = 0; y < width; y++)
                filter[(y * width) + x] /= filterSum;

    }

    private static void createImage(float[] image, int width, int height) {
        Random rand = new Random();
        for (int x = 0; x < height; x++)
            for (int y = 0; y < width; y++)	{
                image[(y * width) + x] = rand.nextInt(256);
                //image[(y * width) + x] = y*x;
	    }
    }

    private static void createFilter(float[] filter, int width, int height) {
        float filterSum = 0.0f;
        Random rand = new Random();

        for (int x = 0; x < height; x++)
            for (int y = 0; y < width; y++) {
                float f = rand.nextFloat();
                //float f =  (x)*(y);
                filterSum += f;
                filter[(y * width) + x] = f;
            }

        for (int x = 0; x < height; x++)
            for (int y = 0; y < width; y++) {
                filter[(y * width) + x] /= filterSum;
	    }

    }


    protected void parseCommandLine(int args) {
        imageSize = 512;
        filterSize = 5;
        if(args == 0) {
            useDouble = true;
        } else {
            useDouble = false;
        }
        //imageSize = Integer.parseInt(line.getOptionValue("image-size", "2048"));
        //filterSize = Integer.parseInt(line.getOptionValue("filter-size", "5"));
    }

    protected void setupBenchmark()  {

        if (useDouble) {
            inputD = new float[imageSize * imageSize];
            outputD = new float[imageSize * imageSize];
            outputDR = new float[imageSize * imageSize];
            filterD = new float[filterSize * filterSize];

            createImage(inputD, imageSize, imageSize);
            createFilter(filterD, filterSize, filterSize);

            //convolveD(inputD, filterD, outputD, imageSize,
                    //imageSize, filterSize, filterSize);

        } else {
            inputF = new float[imageSize * imageSize];
            outputF = new float[imageSize * imageSize];
            outputFR = new float[imageSize * imageSize];
            filterF = new float[filterSize * filterSize];

            createImage(inputF, imageSize, imageSize);
            createFilter(filterF, filterSize, filterSize);

            //convolveF(inputF, filterF, outputF, imageSize,
                    //imageSize, filterSize, filterSize);
        }




    }

    protected void runBenchmark() {
        if (useDouble)
            convolveD(inputD, filterD, outputD, imageSize, imageSize,
                    filterSize, filterSize);
        else
            convolveF(inputF, filterF, outputF, imageSize, imageSize,
                    filterSize, filterSize);
    }

    protected void printResults() {
        int row;
        int col;
        for(int i = 0; i < imageSize*imageSize;i++) {
            row = i / imageSize;
            col = i % imageSize;
            System.out.print(row + " " + col);
            if(useDouble) {
                System.out.println(" " + (int)outputD[i]);
		com.sun.max.vm.Log.println(outputD[i]);
            } else {
                System.out.println(" " + (int)outputF[i]);
		com.sun.max.vm.Log.println(outputF[i]);

            }
        }
    }

    public static void main(String[] args) {

        SimpleExample bm = new SimpleExample();

        bm.parseCommandLine(Integer.parseInt(args[0]));
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
*/

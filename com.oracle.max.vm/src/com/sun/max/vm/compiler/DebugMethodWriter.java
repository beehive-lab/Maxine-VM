package com.sun.max.vm.compiler;

import java.io.*;
import java.util.concurrent.atomic.*;

public class DebugMethodWriter {

    private final AtomicInteger methodCounter;
    private final Object fileLock;
    private final File file;
    private StringBuffer buffer;

    public DebugMethodWriter(String prefix) {
        methodCounter = new AtomicInteger(536870912);
        fileLock = new Object();
        file = initDebugMethods(prefix + "_method_id.txt");
        buffer = new StringBuffer();
    }

    public File initDebugMethods(String fileName) {
        File f;
        if ((f = new File(getDebugMethodsPath() + fileName)).exists()) {
            f.delete();
        }
        f = new File(getDebugMethodsPath() + fileName);
        return f;
    }

    public void appendDebugMethod(String name, int index) {
        buffer.append(index + " " + name + "\n");
    }

    public void flushDebugMethod() throws Exception {
        synchronized (fileLock) {
            try {
                FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(buffer.toString());
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getDebugMethodsPath() {
        return System.getenv("MAXINE_HOME") + "/maxine-tester/junit-tests/";
    }

    public int getNextID() {
        return methodCounter.incrementAndGet();
    }
}

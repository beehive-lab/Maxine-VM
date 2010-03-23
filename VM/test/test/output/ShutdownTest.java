/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package test.output;

public class ShutdownTest extends Thread {
    public static void main(String[] args) {
        boolean exit = false;
        for (String arg : args) {
            if (arg.equals("exit")) {
                exit = true;
            }
        }
        final Thread nonDaemon = new ShutdownTest();
        Runtime.getRuntime().addShutdownHook(new Thread(new MyHook()));
        nonDaemon.start();
        System.out.println(exit ? "explicit exit" : " implicit exit");
        if (exit) {
            System.exit(1);
        }
    }

    @Override
    public void run() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
        }
    }

    static class MyHook implements Runnable {
        public void run() {
            System.out.println("MyHook running");
        }
    }

}

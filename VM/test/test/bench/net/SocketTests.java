/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package test.bench.net;

/**
 *
 * @author Puneeet Lakhina
 */
public class SocketTests {

    /**
     * @param args
     */
    public static void main(String[] args)throws Exception {
        if (args.length > 0) {
            for (String op : args) {
                if ("all".equalsIgnoreCase(op)) {
                    NewSocket.testall();
                } else if ("open".equalsIgnoreCase(op)) {
                    System.out.println("Starting open");
                    NewSocket.testOpen();
                    System.out.println("Completed open");
                    Thread.sleep(2000);
                } else if ("close".equalsIgnoreCase(op)) {
                    System.out.println("Starting close");
                    NewSocket.testClose();
                    System.out.println("Completed close");
                    Thread.sleep(2000);
                }
            }
        } else {
            NewSocket.test();
        }
    }

}

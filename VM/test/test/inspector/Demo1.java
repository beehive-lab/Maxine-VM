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
package test.inspector;


/**
 * Simple Inspector demonstration class, with focus on interaction of watchpoints and GC.
 *
 * @author Michael Van De Vanter
 */
public class Demo1 {

    /**
     * @param args
     */
    public static void main(String[] args) {
        SimpleObject aSimpleObject;

        aSimpleObject = makeObject1();
        aSimpleObject = makeObject2();
        System.gc();
        System.out.println("Object=" + aSimpleObject);
        System.out.println("Demo1 end");
    }

    private static SimpleObject makeObject1() {
        return new SimpleObject("this object will be collected");
    }

    private static SimpleObject makeObject2() {
        return new SimpleObject("this object will not be collected");
    }
    private static class SimpleObject {

        public SimpleObject(String text) {
            this.string = text;
        }

        public String string;
    }
}

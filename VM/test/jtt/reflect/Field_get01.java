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
package jtt.reflect;

/*
 * @Harness: java
 * @Runs: 0=true; 1=true; 2=true; 3=true; 4=true; 5=true; 6=true; 7=true; 8=false;
 */
public class Field_get01 {
    public static final byte byteField = 11;
    public static final short shortField = 12;
    public static final char charField = 13;
    public static final int intField = 14;
    public static final long longField = 15;
    public static final float floatField = 16;
    public static final double doubleField = 17;
    public static final boolean booleanField = true;

    public static boolean test(int arg) throws NoSuchFieldException, IllegalAccessException {
        if (arg == 0) {
            return Field_get01.class.getField("byteField").get(null).equals(byteField);
        } else if (arg == 1) {
            return Field_get01.class.getField("shortField").get(null).equals(shortField);
        } else if (arg == 2) {
            return Field_get01.class.getField("charField").get(null).equals(charField);
        } else if (arg == 3) {
            return Field_get01.class.getField("intField").get(null).equals(intField);
        } else if (arg == 4) {
            return Field_get01.class.getField("longField").get(null).equals(longField);
        } else if (arg == 5) {
            return Field_get01.class.getField("floatField").get(null).equals(floatField);
        } else if (arg == 6) {
            return Field_get01.class.getField("doubleField").get(null).equals(doubleField);
        } else if (arg == 7) {
            return Field_get01.class.getField("booleanField").get(null).equals(booleanField);
        }
        return false;
    }
}

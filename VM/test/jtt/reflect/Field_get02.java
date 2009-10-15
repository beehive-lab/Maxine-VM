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
 * @Runs: 0=true; 1=true; 2=true; 3=true; 4=true; 5=true; 6=true; 7=true; 8=false
 */
public class Field_get02 {

    private static final Field_get02 object = new Field_get02();

    public final byte byteField = 11;
    public final short shortField = 12;
    public final char charField = 13;
    public final int intField = 14;
    public final long longField = 15;
    public final float floatField = 16;
    public final double doubleField = 17;
    public final boolean booleanField = true;

    public static boolean test(int arg) throws NoSuchFieldException, IllegalAccessException {
        if (arg == 0) {
            return Field_get02.class.getField("byteField").get(object).equals(object.byteField);
        } else if (arg == 1) {
            return Field_get02.class.getField("shortField").get(object).equals(object.shortField);
        } else if (arg == 2) {
            return Field_get02.class.getField("charField").get(object).equals(object.charField);
        } else if (arg == 3) {
            return Field_get02.class.getField("intField").get(object).equals(object.intField);
        } else if (arg == 4) {
            return Field_get02.class.getField("longField").get(object).equals(object.longField);
        } else if (arg == 5) {
            return Field_get02.class.getField("floatField").get(object).equals(object.floatField);
        } else if (arg == 6) {
            return Field_get02.class.getField("doubleField").get(object).equals(object.doubleField);
        } else if (arg == 7) {
            return Field_get02.class.getField("booleanField").get(object).equals(object.booleanField);
        }
        return false;
    }
}

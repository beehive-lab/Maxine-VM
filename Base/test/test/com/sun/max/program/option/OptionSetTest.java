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
package test.com.sun.max.program.option;

import java.util.*;

import junit.framework.*;

import com.sun.max.lang.*;
import com.sun.max.program.option.*;

/**
 * Test cases for the number options.
 *
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public class OptionSetTest extends TestCase {

    public OptionSetTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(OptionSetTest.class);
    }

    private static Number parseScaledLong(String number) {
        final OptionSet options = new OptionSet(true);
        final Option<Long> option = options.newOption("l", 0L, OptionTypes.SCALED_LONG_TYPE, OptionSet.Syntax.EQUALS_OR_BLANK, "");
        options.setValue("l", number);
        return option.getValue();
    }

    private static final long K = 1024;
    private static final long M = 1024 * 1024;
    private static final long G = 1024 * 1024 * 1024;

    private static void assertNumberEquals(String number, long value) {
        final long parsedValue = parseScaledLong(number).longValue();
        assertTrue(number + " != " + value, parsedValue == value);
    }

    private static void testFloat(float value) {
        final OptionSet options = new OptionSet();
        final Option<Float> option = options.newFloatOption("f", 0.0f, "");
        if (!Float.isInfinite(value) && !Float.isNaN(value)) {
            for (String suffix : new String[]{"f", "F", "d", "D"}) {
                final String number = value + suffix;
                options.setValue("f", number);
                final float parsedValue = option.getValue();
                assertTrue(number + " != " + value, parsedValue == value);
            }
        }
    }

    private static void testDouble(double value) {
        final OptionSet options = new OptionSet();
        final Option<Double> option = options.newDoubleOption("d", 0.0d, "");
        if (!Double.isInfinite(value) && !Double.isNaN(value)) {
            for (String suffix : new String[]{"d", "D"}) {
                final String number = value + suffix;
                options.setValue("d", number);
                final double parsedValue = option.getValue();
                assertTrue(number + " != " + value, parsedValue == value);
            }
        }
    }

    private static void testIllegal(String number) {
        try {
            final Number value = parseScaledLong(number);
            fail(value + " should be illegal");
        } catch (Option.Error e) {
        }
    }

    public void test_NumberProgramOption() {

        final long[] nums = {Long.MIN_VALUE, Long.MIN_VALUE + 1, Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0, 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE};

        for (long n : nums) {
            assertNumberEquals(n + "", n);
            if (Ints.VALUE_RANGE.contains(n)) {
                assertNumberEquals(n + "k", n * K);
                assertNumberEquals(n + "K", n * K);
                assertNumberEquals(n + "m", n * M);
                assertNumberEquals(n + "M", n * M);
                assertNumberEquals(n + "g", n * G);
                assertNumberEquals(n + "G", n * G);
            }
        }

        // TODO: test illegal doubles, hexadecimal constants

        testIllegal(Long.MIN_VALUE + "k");
        testIllegal(Long.MIN_VALUE + "0");
        testIllegal(Long.MAX_VALUE + "k");
        testIllegal(Long.MAX_VALUE + "0");

        final float[] floats = {Float.MIN_NORMAL, Float.MIN_VALUE, Float.MAX_VALUE, -1.0f, 0f, 1.0f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN};
        for (float f : floats) {
            testFloat(f);
        }

        final double[] doubles = {Double.MIN_NORMAL, Double.MIN_VALUE, Double.MAX_VALUE, -1.0d, 0d, 1.0d, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN};
        for (double f : doubles) {
            testDouble(f);
        }
    }

    /**
     * Test class for instance option.
     */
    public static class InstanceTestClassA implements Runnable {
        public void run() {
        }
    }

    /**
     * Test class for instance options.
     */
    public static class InstanceTestClassB implements Runnable {
        public void run() {
        }
    }

    /**
     * Test method for the option that has a list of instances of a given type as value.
     */
    public static void test_InstanceListOption() {
        final String prefix = "test";
        final char separator = ',';
        final OptionSet options = new OptionSet();
        final Option<List<Runnable>> testOption = options.newListInstanceOption(prefix, null, Runnable.class, separator, "");

        // Test no value given
        assertEquals(null, testOption.getDefaultValue());

        // Test single class
        final Class<InstanceTestClassA> klassA = InstanceTestClassA.class;
        options.setValue(prefix, klassA.getName());
        final List<Runnable> result = testOption.getValue();
        assertEquals(1, result.size());
        assertTrue(result.get(0).getClass().equals(klassA));

        // Test two classes
        final Class<InstanceTestClassB> klassB = InstanceTestClassB.class;
        options.setValue(prefix, klassA.getName() + separator + klassB.getName());
        final List<Runnable> result2 = testOption.getValue();
        assertEquals(2, result2.size());
        final boolean order1 = result2.get(0).getClass().equals(klassA) && result2.get(1).getClass().equals(klassB);
        final boolean order2 = result2.get(1).getClass().equals(klassA) && result2.get(0).getClass().equals(klassB);
        assertTrue(order1 || order2);
    }

    /**
     * Test method for the option that has an instance of a given type as value.
     */
    public static void test_InstanceOption() {
        final String prefix = "test";
        final OptionSet options = new OptionSet();
        final Option<Runnable> testOption = options.newInstanceOption(prefix, Runnable.class, null, "");

        // Test no value given
        assertEquals(null, testOption.getDefaultValue());

        // Test single class
        final Class<InstanceTestClassA> klass = InstanceTestClassA.class;
        options.setValue(prefix, klass.getName());
        final Runnable result = testOption.getValue();
        assertTrue(result.getClass().equals(klass));

    }
}

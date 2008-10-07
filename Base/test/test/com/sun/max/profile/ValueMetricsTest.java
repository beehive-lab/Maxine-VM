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
package test.com.sun.max.profile;

import junit.framework.*;

import com.sun.max.profile.*;
import com.sun.max.profile.ValueMetrics.*;

public class ValueMetricsTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ValueMetricsTest.class);
    }

    public void test_ValueMetrics_newDistribution() {
        //assertTrue(ValueMetrics.newIntegerDistribution("test", ValueMetrics.EXACT) != null);
        assertTrue(ValueMetrics.newIntegerDistribution("test", new ValueMetrics.IntegerRangeApproximation(0, 1)) != null);
        assertTrue(ValueMetrics.newIntegerDistribution("test", new ValueMetrics.FixedApproximation(0, 1, 7)) != null);
        assertTrue(ValueMetrics.newIntegerDistribution("test", 0, 1) != null);
        assertTrue(ValueMetrics.newIntegerDistribution("test", new int[] {0, 1, 7}) != null);
        assertTrue(ValueMetrics.newObjectDistribution("test", new ValueMetrics.FixedApproximation(0, 1)) != null);
        assertTrue(ValueMetrics.newObjectDistribution("test", new ValueMetrics.FixedApproximation(0, 1, 7)) != null);
    }

    public void test_recordIntegerRange1() {
        final IntegerDistribution distribution = ValueMetrics.newIntegerDistribution(null, 0, 10);
        for (int i = -3; i < 300; i++) {
            distribution.record(i);
        }
        assertTrue(distribution.getCount(-1) == -1);
        assertTrue(distribution.getCount(0) == 1);
        assertTrue(distribution.getCount(1) == 1);
        assertTrue(distribution.getCount(2) == 1);
        assertTrue(distribution.getCount(3) == 1);
        assertTrue(distribution.getCount(9) == 1);
        assertTrue(distribution.getCount(10) == -1);
        assertTrue(distribution.getTotal() == 303);
    }

    public void test_recordIntegerRange2() {
        final IntegerDistribution distribution = ValueMetrics.newIntegerDistribution(null, -2, 2);
        for (int i = -3; i < 15; i++) {
            distribution.record(i);
            distribution.record(i);
            distribution.record(i);
        }
        assertTrue(distribution.getCount(-3) == -1);
        assertTrue(distribution.getCount(-2) == 3);
        assertTrue(distribution.getCount(-1) == 3);
        assertTrue(distribution.getCount(0) == 3);
        assertTrue(distribution.getCount(1) == 3);
        assertTrue(distribution.getCount(2) == -1);
        assertTrue(distribution.getTotal() == 3 * 18);
    }

    public void test_recordIntegerSet1() {
        final IntegerDistribution distribution = ValueMetrics.newIntegerDistribution(null, new int[] {0, 3, 10});
        for (int i = -3; i < 300; i++) {
            distribution.record(i);
        }
        assertTrue(distribution.getCount(-1) == -1);
        assertTrue(distribution.getCount(0) == 1);
        assertTrue(distribution.getCount(1) == -1);
        assertTrue(distribution.getCount(2) == -1);
        assertTrue(distribution.getCount(3) == 1);
        assertTrue(distribution.getCount(9) == -1);
        assertTrue(distribution.getCount(10) == 1);
        assertTrue(distribution.getTotal() == 303);
    }

    public void test_recordIntegerSet2() {
        final IntegerDistribution distribution = ValueMetrics.newIntegerDistribution(null, new int[]{-2, 2});
        for (int i = -3; i < 15; i++) {
            distribution.record(i);
            distribution.record(i);
            distribution.record(i);
        }
        assertTrue(distribution.getCount(-3) == -1);
        assertTrue(distribution.getCount(-2) == 3);
        assertTrue(distribution.getCount(-1) == -1);
        assertTrue(distribution.getCount(0) == -1);
        assertTrue(distribution.getCount(1) == -1);
        assertTrue(distribution.getCount(2) == 3);
        assertTrue(distribution.getTotal() == 3 * 18);
    }

    public void test_recordObjects1() {
        final Object obj1 = new Object();
        final Object obj2 = new Object();
        final Object obj3 = new Object();
        final Object obj4 = new Object();
        final ObjectDistribution<Object> distribution = ValueMetrics.newObjectDistribution(null);
        for (int i = 0; i < 300; i++) {
            distribution.record(obj1);
            distribution.record(obj2);
            distribution.record(obj3);
        }
        assertTrue(distribution.getCount(obj1) == 300);
        assertTrue(distribution.getCount(obj2) == 300);
        assertTrue(distribution.getCount(obj3) == 300);
        assertTrue(distribution.getCount(obj4) == 0);
        assertTrue(distribution.getTotal() == 900);
    }

    public void test_recordObjectSet1() {
        final Object obj1 = new Object();
        final Object obj2 = new Object();
        final Object obj3 = new Object();
        final Object obj4 = new Object();
        final ObjectDistribution<Object> distribution = ValueMetrics.newObjectDistribution(null, new Object[] {obj1, obj2, obj3});
        for (int i = 0; i < 300; i++) {
            distribution.record(obj1);
            distribution.record(obj2);
            distribution.record(obj3);
        }
        assertTrue(distribution.getCount(obj1) == 300);
        assertTrue(distribution.getCount(obj2) == 300);
        assertTrue(distribution.getCount(obj3) == 300);
        assertTrue(distribution.getCount(obj4) == 0);
        assertTrue(distribution.getTotal() == 900);
    }

    public void test_recordObjectSet2() {
        final Object obj1 = new Object();
        final Object obj2 = new Object();
        final Object obj3 = new Object();
        final Object obj4 = new Object();
        final ObjectDistribution<Object> distribution = ValueMetrics.newObjectDistribution(null, new Object[] {obj1, obj3});
        for (int i = 0; i < 300; i++) {
            distribution.record(obj1);
            distribution.record(obj2);
            distribution.record(obj3);
        }
        assertTrue(distribution.getCount(obj1) == 300);
        assertTrue(distribution.getCount(obj2) == -1);
        assertTrue(distribution.getCount(obj3) == 300);
        assertTrue(distribution.getCount(obj4) == -1);
        assertTrue(distribution.getTotal() == 900);
    }

    public void test_recordIntegerTrace1() {
        test_recordIntegerTrace(1);
        test_recordIntegerTrace(5);
        test_recordIntegerTrace(10);
        test_recordIntegerTrace(16);
        test_recordIntegerTrace(100);
    }

    public void test_recordIntegerTrace(int bufferSize) {
        final IntegerDistribution distribution = ValueMetrics.newIntegerDistribution(null, new ValueMetrics.IntegerTraceApproximation(bufferSize));

        for (int loop = 0; loop < 5; loop++) {
            for (int i = -3; i < 15; i++) {
                distribution.record(i);
                distribution.record(i);
                distribution.record(i);
            }
        }
        assertTrue(distribution.getCount(-3) == 15);
        assertTrue(distribution.getCount(-2) == 15);
        assertTrue(distribution.getCount(-1) == 15);
        assertTrue(distribution.getCount(0) == 15);
        assertTrue(distribution.getCount(1) == 15);
        assertTrue(distribution.getCount(2) == 15);
        assertTrue(distribution.getTotal() == 15 * 18);
    }
}

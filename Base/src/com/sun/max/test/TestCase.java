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
/**
 *
 */
package com.sun.max.test;

import java.io.*;
import java.util.*;

/**
 * The {@code TestCase} class represents a basic test case in the testing framework.
 * @author "Ben L. Titzer"
 */
public abstract class TestCase {

    public final File file;
    public final Properties props;
    public final TestHarness harness;
    public int testNumber;
    public long startTime;
    public long endTime;
    public Throwable thrown;
    public TestResult result;

    protected TestCase(TestHarness harness, File file, Properties props) {
        this.harness = harness;
        this.file = file;
        this.props = props;
    }

    public void test() {
        try {
            startTime = System.currentTimeMillis();
            run();
        } catch (Throwable t) {
            thrown = t;
        } finally {
            endTime = System.currentTimeMillis();
        }
    }

    public abstract void run() throws Throwable;
}

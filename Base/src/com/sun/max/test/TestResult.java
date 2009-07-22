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



/**
 * The {@code TestResult} class represents the result of running a test.
 * The result is computed by the harness associated with the test.
 * @author Ben L. Titzer
 */
public abstract class TestResult {

    public static final Success SUCCESS = new Success();
    public static final Failure FAILURE = new Failure();

    public abstract boolean isSuccess();

    public static class Success extends TestResult {
        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    public static class Failure extends TestResult {
        @Override
        public boolean isSuccess() {
            return false;
        }
        @Override
        public String failureMessage(TestCase testCase) {
            return testCase.file + " failed";
        }
    }

    public String failureMessage(TestCase tc) {
        return "";
    }

    public static class UnexpectedException extends Failure {
        public final String message;
        public final Throwable thrown;
        public UnexpectedException(Throwable thrown) {
            this.message = "Unexpected exception";
            this.thrown = thrown;
        }
        public UnexpectedException(String message, Throwable thrown) {
            this.message = message;
            this.thrown = thrown;
        }

        @Override
        public String failureMessage(TestCase testCase) {
            return "unexpected exception: " + thrown.toString();
        }
    }
}

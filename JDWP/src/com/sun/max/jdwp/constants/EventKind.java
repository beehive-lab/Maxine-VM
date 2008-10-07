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
/*VCSID=b1fd1fd8-1a12-48cf-929a-1856063e2934*/

// WARNING: Generated file, do not modify!

// To use the original JDWP specification as a basis, some styleguides have to be turned off
// Checkstyle: stop field name check

package com.sun.max.jdwp.constants;

public final class EventKind {
    public static final int SINGLE_STEP = 1;
    public static final int BREAKPOINT = 2;
    public static final int FRAME_POP = 3;
    public static final int EXCEPTION = 4;
    public static final int USER_DEFINED = 5;
    public static final int THREAD_START = 6;
    public static final int THREAD_DEATH = 7;
    public static final int THREAD_END = 7;
    public static final int CLASS_PREPARE = 8;
    public static final int CLASS_UNLOAD = 9;
    public static final int CLASS_LOAD = 10;
    public static final int FIELD_ACCESS = 20;
    public static final int FIELD_MODIFICATION = 21;
    public static final int EXCEPTION_CATCH = 30;
    public static final int METHOD_ENTRY = 40;
    public static final int METHOD_EXIT = 41;
    public static final int METHOD_EXIT_WITH_RETURN_VALUE = 42;
    public static final int MONITOR_CONTENDED_ENTER = 43;
    public static final int MONITOR_CONTENDED_ENTERED = 44;
    public static final int MONITOR_WAIT = 45;
    public static final int MONITOR_WAITED = 46;
    public static final int VM_START = 90;
    public static final int VM_INIT = 90;
    public static final int VM_DEATH = 99;
    public static final int VM_DISCONNECTED = 100;
}

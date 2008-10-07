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
/*VCSID=23381f56-498b-4926-a377-7509750c3a5e*/
package com.sun.max.vm;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;

/**
 * Implements the "-verbose[:class|gc|jni|comp]" VM option.
 *
 * @author Doug Simon
 */
public class VerboseVMOption extends VMOption {

    private static boolean _verboseClass;
    private static boolean _verboseCompilation;
    private static boolean _verboseGC;
    private boolean _timeGC;
    private static boolean _verboseJNI;
    private static final VerboseVMOption _verboseOption = new VerboseVMOption();

    public VerboseVMOption() {
        super("-verbose", "Enables verbose output.", MaxineVM.Phase.PRISTINE);
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        if (CString.equals(optionValue, "")) {
            _verboseClass = true;
            _verboseGC = true;
            _verboseJNI = true;
            _verboseCompilation = true;
        } else if (CString.equals(optionValue, ":gc")) {
            _verboseGC = true;
        } else if (CString.equals(optionValue, ":class")) {
            _verboseClass = true;
        } else if (CString.equals(optionValue, ":jni")) {
            _verboseJNI = true;
        } else if (CString.equals(optionValue, ":comp")) {
            _verboseCompilation = true;
        } else {
            return false;
        }
        return true;
    }

    /**
     * Determines if information should be displayed about each garbage collection event.
     */
    public static boolean verboseGC() {
        return _verboseGC || Heap.traceGC();
    }

    public static boolean verboseTimeGC() {
        return _verboseOption._timeGC;
    }



    /**
     * Determines if information should be displayed about use of native methods and other Java Native Interface activity.
     */
    public static boolean verboseJNI() {
        return _verboseJNI || ClassMethodActor.traceJNI();
    }

    /**
     * Determines if information should be displayed about each class loaded.
     */
    public static boolean verboseClassLoading() {
        return _verboseClass;
    }

    /**
     * Determines if information should be displayed about each compiler event.
     */
    public static boolean verboseCompilation() {
        return _verboseCompilation;
    }

    @Override
    public void printHelp() {
        VMOptions.printHelpForOption("-verbose[:class|gc|jni|comp]", "", _help);
    }
}

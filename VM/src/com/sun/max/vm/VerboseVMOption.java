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
package com.sun.max.vm;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * Implements the "-verbose[:class|gc|jni|comp]" VM option.
 *
 * @author Doug Simon
 */
public class VerboseVMOption extends VMOption {

    /**
     * Determines if information should be displayed about each class loaded.
     */
    public boolean verboseClass;

    /**
     * Determines if information should be displayed about each compiler event.
     */
    public boolean verboseCompilation;

    /**
     * Determines if information should be displayed about each garbage collection event.
     */
    public boolean verboseGC;

    /**
     * Determines if information should be displayed about the {@linkplain System#getProperties() system properties} when
     * they initialized during VM startup.
     */
    public boolean verboseProperties;

    /**
     * Determines if information should be displayed about use of native methods and other Java Native Interface activity.
     */
    public boolean verboseJNI;

    @HOSTED_ONLY
    public VerboseVMOption() {
        super("-verbose", "Enables verbose output. ");
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        if (CString.length(optionValue).isZero()) {
            verboseClass = true;
            verboseGC = true;
            verboseJNI = true;
            verboseCompilation = true;
            verboseProperties = true;
        } else if (CString.equals(optionValue, ":gc")) {
            verboseGC = true;
        } else if (CString.equals(optionValue, ":class")) {
            verboseClass = true;
        } else if (CString.equals(optionValue, ":jni")) {
            verboseJNI = true;
        } else if (CString.equals(optionValue, ":comp")) {
            verboseCompilation = true;
        } else if (CString.equals(optionValue, ":props")) {
            verboseProperties = true;
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(category(), "-verbose[:class|gc|jni|comp|props]", "", help);
    }
}

/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * Implements the VM options controlling assertions.
 *
 * @author Doug Simon
 */
public class AssertionsVMOption extends VMOption {

    public static AssertionsVMOption ASSERTIONS = register(new AssertionsVMOption(), MaxineVM.Phase.STARTING);

    @HOSTED_ONLY
    public AssertionsVMOption() {
        super("-ea", "");
    }

    @Override
    public boolean matches(Pointer arg) {
        return
            CString.startsWith(arg, "-ea") || CString.startsWith(arg, "-enableassertions") ||
            CString.startsWith(arg, "-da") || CString.startsWith(arg, "-disableassertions") ||
            CString.equals(arg, "-esa") || CString.equals(arg, "-enablesystemassertions") ||
            CString.equals(arg, "-dsa") || CString.equals(arg, "-disablesystemassertions");
    }

    @Override
    public boolean parseValue(Pointer optionValue) {
        // TODO: Implement this!
        return true;
    }

    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(category(), "-ea[:<packagename>...|:<classname>]", "", null);
        VMOptions.printHelpForOption(category(), "-enableassertions[:<packagename>...|:<classname>]", "", "enable assertions");
        VMOptions.printHelpForOption(category(), "-da[:<packagename>...|:<classname>]", "", null);
        VMOptions.printHelpForOption(category(), "-disableassertions[:<packagename>...|:<classname>]", "", "disable assertions");
        VMOptions.printHelpForOption(category(), "-esa | -enablesystemassertions", "", "enable system assertions");
        VMOptions.printHelpForOption(category(), "-dsa | -disablesystemassertions", "", "disable system assertions");
    }
}

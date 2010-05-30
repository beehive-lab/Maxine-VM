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
package com.sun.max.vm.tele;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import static com.sun.max.vm.VMOptions.*;

/**
 * Holder for magic word that communicates whether this VM is being inspected and possibly
 * other flags.
 *
 * @author Michael Van De Vanter
 */
public final class Inspectable {

    private Inspectable() {
    }

    /**
     * Option to make the VM run inspectable when VM not created by Inspector.
     */
    public static final VMBooleanXXOption makeInspectable = register(new VMBooleanXXOption("-XX:-MakeInspectable", "Make it possible for Inspector to attach to running VM"), MaxineVM.Phase.PRISTINE);

    /**
     * Constant denoting that the VM process is being inspected.
     */
    public static final int INSPECTED = 0x0000001;

    /**
    /**
     * If a non-zero value is put here remotely, or by command line option, then the
     * additional steps to facilitate inspection should be activated.
     */
    @INSPECTED
    private static int flags = INSPECTED;  // temp hack

    private static boolean optionChecked;

    /**
     * Determines if the VM process is being inspected.
     */
    public static boolean isVmInspected() {
//        Log.print("isInspected is "); Log.print((flags & INSPECTED) != 0); Log.println();
        if ((flags & INSPECTED) != 0) {
            return true;
        }
        if (optionChecked) {
            return false;
        }
        if (makeInspectable.getValue()) {
            flags = INSPECTED;
        }
        optionChecked = true;
//        Log.print("isInspected is "); Log.print((flags & INSPECTED) != 0); Log.println();
        return (flags & INSPECTED) != 0;
    }

}

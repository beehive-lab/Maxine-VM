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
package com.sun.max.vm.run.newJava;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.run.*;
import com.sun.max.vm.runtime.*;

/**
 * The normal Java run scheme that
 * creates a boot class loader,
 * boots the core JDK,
 * creates an application class loader,
 * loads a main class by the latter,
 * runs the main method in that class.
 *
 * @author Bernd Mathiske
 */
public class NewJavaRunScheme extends AbstractVMScheme implements RunScheme {

    public NewJavaRunScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    public IterableWithLength<MethodActor> gatherNativeInitializationMethods() {
        @JavacSyntax("javac type checker needs an extra hint here")
        final Class<IterableWithLength<MethodActor>> type = null;
        return StaticLoophole.cast(type, Iterables.empty());
    }

    public void runNativeInitializationMethods() {
    }

    protected final void bareMinimum() {
        VMConfiguration.hostOrTarget().initializeSchemes(MaxineVM.Phase.PRISTINE);

        Trap.initialize();

        MaxineVM.hostOrTarget().setPhase(MaxineVM.Phase.STARTING);

        // Now we can decode all the other VM arguments using the full language
        VMOptions.parseStarting();

        VMConfiguration.hostOrTarget().initializeSchemes(MaxineVM.Phase.STARTING);
    }

    /**
     * Main routine that "runs" the VM. Executed by the "main" 'VmThread'.
     *
     * Once we get here, three mutually connected thread representations referring to the current thread we are
     * running on have already been properly initialized: - a native thread that is separate from the primordial
     * native thread, - a "main" 'VmThread', - a Java 'Thread'.
     *
     * And thread synchronization should work, because we are now in a Java thread.
     *
     * Given this, we can perform these initializations: - fill the JNI interface with entry points of Java methods
     * that can be called from C. - initialize the heap
     *
     * And then we start the Java application sandbox.
     *
     * @return the designated exit code of the VM process
     */
    public void run() {
        bareMinimum();
        Debug.print("start nanoTime: ");
        Debug.println(System.nanoTime());
        Debug.print("start currentTimeMillis: ");
        Debug.println(System.currentTimeMillis());

        Trace.setStream(Debug.out);
        Trace.on(VMOptions.traceLevel());
        Trace.line(VMOptions.traceLevel(), "trace level is " + VMOptions.traceLevel());

        if (VMOptions.parseMain(true)) {
            final BootClassLoader bootClassLoader = new BootClassLoader();
            Classes.forName(SandBox.class.getName(), true, bootClassLoader);
        }

        Debug.print("end nanoTime: ");
        Debug.println(System.nanoTime());
        Debug.print("end currentTimeMillis: ");
        Debug.println(System.currentTimeMillis());
    }
}

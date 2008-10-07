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
/*VCSID=46982b1d-d95b-48d1-9e0a-1c1455259344*/
package com.sun.max.vm.run.newJava;

import java.lang.reflect.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
final class SandBox {
    private SandBox() {
    }

    private Runnable _runnable = new Runnable() {
        public void run() {

        }
    };

    private static void boot(String className) {
        Classes.initialize(Classes.forName(className));
    }

    private static void boot(Class... javaClasses) {
        for (Class javaClass : javaClasses) {
            boot(javaClass.getName());
        }
    }

    private static void bootJDK() {
        boot(String.class, System.class, ThreadGroup.class);
        try {
            MethodActor.fromJava(System.class.getDeclaredMethod("initializeSystemClass", void.class)).invoke();
        } catch (Throwable throwable) {
            ProgramError.unexpected(throwable);
        }

        boot(Method.class);
        boot("java.lang.ref.Finalizer");
    }

    static void run() {
        bootJDK();

        MaxineVM.hostOrTarget().setPhase(Phase.RUNNING);
        VMConfiguration.hostOrTarget().initializeSchemes(MaxineVM.Phase.RUNNING);

        boolean error = true;
        try {
            Debug.println("invoking main()");
            final SystemClassLoader systemClassLoader = new SystemClassLoader();
            final Class<?> mainClass = systemClassLoader.loadClass(VMOptions.mainClassName());
            final Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
            mainMethod.invoke(ReferenceValue.from(VMOptions.mainClassArguments()));
            error = false;
        } catch (ClassNotFoundException classNotFoundException) {
            System.err.println("could not load main class: " + classNotFoundException);
        } catch (NoSuchMethodException noSuchMethodException) {
            System.err.println("could not find main method: " + noSuchMethodException);
        } catch (InvocationTargetException invocationTargetException) {
            System.err.println("main method terminated by uncaught exception: " + invocationTargetException.getTargetException());
        } catch (IllegalAccessException illegalAccessException) {
            System.err.println("illegal access trying to invoke main method: " + illegalAccessException);
        } finally {
            if (error) {
                MaxineVM.setExitCode(1);
            }
        }
    }

    static {
        run();
    }
}

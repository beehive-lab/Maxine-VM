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
/*VCSID=963c30c8-0a84-459b-bd42-249500081f93*/
package com.sun.max.vm.run;

import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;

/**
 * The {@code RunScheme} interface defines what the VM is configured to execute
 * after it has started its basic services and is ready to set up and run a language
 * environment, e.g. Java program.
 * 
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public interface RunScheme extends VMScheme {

    /**
     * The run method for the main Java thread.
     */
    void run();

    /**
     * While prototyping, gather static native methods in JDK classes that need to be re-executed at target startup.
     * Typically, such are methods called "initIDs" in JDK classes and
     * they assign JNI method and field IDs to static C/C++ variables.
     * 
     * Note that this method may be called numerous times during the prototyping phase and so the data structure
     * maintained by this run scheme to record the methods should take this into account.
     * 
     * @return the set of methods gathered
     */
    IterableWithLength<? extends MethodActor> gatherNativeInitializationMethods();

    /**
     * At target startup, run the above methods.
     */
    void runNativeInitializationMethods();
}

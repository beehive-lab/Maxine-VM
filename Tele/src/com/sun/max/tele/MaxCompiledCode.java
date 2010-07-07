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
package com.sun.max.tele;

import com.sun.max.tele.object.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;


/**
 * Data describing a single compilation of a method, stub, adaptor, or other routine in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxCompiledCode extends MaxMachineCode<MaxCompiledCode> {

    /**
     * @return the 0-based sequence number of this compilation among the
     * possibly multiple compilations of the method.
     */
    int compilationIndex();

    /**
     * Gets accessor to the method descriptor in the VM for this compilation.
     *
     * @return access to the {@link ClassMethodActor} for the machine code in the VM, if it was
     * compiled from a Java method; null otherwise.
     */
    TeleClassMethodActor getTeleClassMethodActor();

    /**
     * @return local instance of {@link ClassMethodActor} corresponding to the machine code
     * in the VM, if it was compiled from a Java method; null otherwise.
     */
    ClassMethodActor classMethodActor();

    /**
     * Gets the local instance of the class description for the object that represents this
     * compilation in the VM.
     *
     * @return a local descriptor of the type of the object representing this compilation
     * in the VM.
     */
    ClassActor classActorForObjectType();

    /**
     * Gets the VM object that represents the compilation.
     *
     * @return the VM object holding the compiled code.
     */
    TeleTargetMethod teleTargetMethod();

}

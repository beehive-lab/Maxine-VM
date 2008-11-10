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

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.bytecode.*;

/**
 * Data describing a single code routine on the target VM, either compiled from a Java method or a block of native code.
 *
 * @author Michael Van De Vanter
 */
public interface TeleTargetRoutine extends TargetMethodAccess {

    /**
     * @return the region of memory in the teleVM occupied by this target routine:  Java method or native
     */
    TargetCodeRegion targetCodeRegion();

    TeleRoutine teleRoutine();

    /**
     * @return Target VM address of the first instruction in the target code represented by this routine. Note that this
     *         may differ from the designated {@linkplain #callEntryPoint() entry point} of the code.
     */
    Address codeStart();

    /**
     * @return length of code for this routine in the target VM in bytes.
     */
    Size codeSize();

    /**
     * @return target address at which this code is entered from a call (which may not be the same as the
     *         {@linkplain #codeStart() start address})
     */
    Address callEntryPoint();

    /**
     * @return  target code instructions in this routine, disassembling them if not yet done
     */
    IndexedSequence<TargetCodeInstruction> getInstructions();

    /**
     * Sets a target code breakpoint at the method entry.
     */
    TeleTargetBreakpoint setTargetBreakpointAtEntry();

    /**
     * Sets a target code breakpoint at every label synthesized by disassembly of this method.
     */
    void setTargetCodeLabelBreakpoints();

    /**
     * Removes a target code breakpoint at every label synthesized by disassembly of this method.
     */
    void removeTargetCodeLabelBreakpoints();

    /**
     * @return Local {@link TeleClassMethodActor} for the target routine in the tele VM, if it was
     * compiled from a Java method; null otherwise.
     */
    TeleClassMethodActor getTeleClassMethodActor();

    int[] getStopPositions();

    int[] bytecodeToTargetCodePositionMap();

    BytecodeInfo[] bytecodeInfos();

    /**
     * Gets the index of the stop corresponding to a given address that possibly denotes a position within this tele
     * routine.
     *
     * @param address a target code address
     * @return the index of the stop corresponding to {@code address} or -1 if {@code address} does not correspond to a
     *         stop within this tele routine
     */
    int getJavaStopIndex(Address address);

}

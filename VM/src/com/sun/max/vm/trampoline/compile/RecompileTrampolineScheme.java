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
/*VCSID=4dc24503-deef-4b38-8cf6-9d71fa704243*/
package com.sun.max.vm.trampoline.compile;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.trampoline.*;

/**
 * Full-recompilation based Trampoline scheme.
 * @see RecompileTrampolineGenerator
 *
 * @author Laurent Daynes
 */
public class RecompileTrampolineScheme extends AbstractVMScheme implements DynamicTrampolineScheme {
    private final DynamicTrampolineExit _dynamicTrampolineExit;

    public RecompileTrampolineScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _dynamicTrampolineExit = DynamicTrampolineExit.create(vmConfiguration);
    }

    public boolean isDynamicTrampoline(ClassMethodActor classMethodActor) {
        return VTableTrampolineSnippet.isVTableTrampoline(classMethodActor) || ITableTrampolineSnippet.isITableTrampoline(classMethodActor);
    }

    public Address makeInterfaceCallEntryPoint(int vTableIndex) {
        return ITableTrampolineSnippet.makeCallEntryPoint(vTableIndex);
    }

    public Address makeVirtualCallEntryPoint(int iIndex) {
        return VTableTrampolineSnippet.makeCallEntryPoint(iIndex);
    }

    public DynamicTrampolineExit dynamicTrampolineExit() {
        return _dynamicTrampolineExit;
    }
}

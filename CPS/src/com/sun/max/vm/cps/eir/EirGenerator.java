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
package com.sun.max.vm.cps.eir;

import com.sun.max.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirGenerator<EirGeneratorScheme_Type extends EirGeneratorScheme>
    extends IrGenerator<EirGeneratorScheme_Type, EirMethod> {

    private final EirABIsScheme eirABIsScheme;

    public EirABIsScheme eirABIsScheme() {
        return eirABIsScheme;
    }

    private final WordWidth wordWidth;

    public WordWidth wordWidth() {
        return wordWidth;
    }

    protected EirGenerator(EirGeneratorScheme_Type eirGeneratorScheme) {
        super(eirGeneratorScheme, "EIR");
        final VMConfiguration vmConfiguration = compilerScheme().vmConfiguration();
        final Platform platform = vmConfiguration.platform();
        wordWidth = platform.processorKind.dataModel.wordWidth;
        final MaxPackage eirPackage = new com.sun.max.vm.cps.eir.Package();
        final MaxPackage p = eirPackage.subPackage(platform.processorKind.instructionSet.name().toLowerCase(),
                                                   platform.operatingSystem.name().toLowerCase());
        eirABIsScheme = vmConfiguration.loadAndInstantiateScheme(p, EirABIsScheme.class, vmConfiguration);
    }

    @Override
    public final EirMethod createIrMethod(ClassMethodActor classMethodActor) {
        final EirMethod eirMethod = new EirMethod(classMethodActor, eirABIsScheme);
        notifyAllocation(eirMethod);
        return eirMethod;
    }

    public abstract EirLocation catchParameterLocation();

    public Kind eirKind(Kind kind) {
        final Kind k = kind.stackKind;
        if (k.isWord) {
            return (wordWidth == WordWidth.BITS_64) ? Kind.LONG : Kind.INT;
        }
        return k;
    }

    public EirMethod makeIrMethod(DirMethod dirMethod) {
        ProgramError.unexpected();
        return null;
    }
}

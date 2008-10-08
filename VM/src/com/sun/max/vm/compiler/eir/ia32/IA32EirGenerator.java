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
package com.sun.max.vm.compiler.eir.ia32;

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class IA32EirGenerator extends EirGenerator<IA32EirGeneratorScheme> {

    public IA32EirGenerator(IA32EirGeneratorScheme eirGeneratorScheme) {
        super(eirGeneratorScheme);
    }

    private final EirLocation _eirCatchParameterLocation = eirABIsScheme().javaABI().getResultLocation(Kind.REFERENCE);

    @Override
    public EirLocation catchParameterLocation() {
        return _eirCatchParameterLocation;
    }

    /**
     * Assigns an exception object into the result location defined by the {@linkplain EirABIsScheme#javaABI() Java ABI}.
     * which is where this compiler expects the exception to be when compiling a catch block.
     * 
     * The {@link NEVER_INLINE} annotation guarantees that the assignment to the result location actually occurs.
     */
    @NEVER_INLINE
    public static Throwable assignExceptionToCatchParameterLocation(Throwable throwable) {
        return throwable;
    }

}

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
/*VCSID=de46d921-5eec-4f69-9e09-040a5d0b7e55*/
package com.sun.max.vm.compiler.eir.amd64.guestvm;

import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.compiler.eir.amd64.unix.*;


public class GuestVMAMD64EirABIs extends AMD64EirABIsScheme {

    public GuestVMAMD64EirABIs(VMConfiguration vmConfiguration) {
        super(vmConfiguration, new UnixAMD64EirJavaABI(vmConfiguration),
                                new UnixAMD64EirNativeABI(vmConfiguration),
                                new UnixAMD64EirCFunctionABI(vmConfiguration, false /*called from Java only*/),
                                new UnixAMD64EirCFunctionABI(vmConfiguration, true /*called from native code only*/),
                                new UnixAMD64EirTrampolineABI(vmConfiguration),
                                new UnixAMD64EirTemplateABI(vmConfiguration),
                                new UnixAMD64EirTreeABI(vmConfiguration));
    }

}

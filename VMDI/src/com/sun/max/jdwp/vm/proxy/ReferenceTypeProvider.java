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
/*VCSID=864a39cc-36a4-45cb-94ee-4d53f81ef8af*/
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;

/**
 * Class representing a reference type in the JDWP protocol.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface ReferenceTypeProvider extends ObjectProvider {

	public final class ClassStatus {
	    public static final int VERIFIED = 1;
	    public static final int PREPARED = 2;
	    public static final int INITIALIZED = 4;
	    public static final int ERROR = 8;
	}

    @ConstantReturnValue
    VMValue.Type getType();

    int getStatus();

    @ConstantReturnValue
    int getFlags();

    @ConstantReturnValue
    String getSourceFileName();

    @ConstantReturnValue
    String getName();

    @ConstantReturnValue
    String getSignature();

    @ConstantReturnValue
    String getSignatureWithGeneric();

    @ConstantReturnValue
    ClassLoaderProvider classLoader();

    @ConstantReturnValue
    FieldProvider[] getFields();

    @ConstantReturnValue
    InterfaceProvider[] getImplementedInterfaces();

    @ConstantReturnValue
    MethodProvider[] getMethods();

    @ConstantReturnValue
    ReferenceTypeProvider[] getNestedTypes();

    @ConstantReturnValue
    ClassObjectProvider classObject();

    ObjectProvider[] getInstances();

    @ConstantReturnValue
    int majorVersion();

    @ConstantReturnValue
    int minorVersion();
}

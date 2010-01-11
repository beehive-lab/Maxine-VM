/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.target;

import java.util.*;

import com.sun.max.vm.compiler.target.Adapter.*;
import com.sun.max.vm.type.*;

/**
 * Generator for adapter frame stubs. An adapter frame stub contains code to handle the
 * transition between two compiled methods that have different argument passing conventions.
 *
 * Generic Adapter Frame Generator.
 *
 * @author Laurent Daynes
 */
public abstract class AdapterGenerator {


    public static String toAdapterSignature(SignatureDescriptor signature, int receiver) {
        int count = signature.numberOfParameters() + receiver;
        char[] result = new char[count];
        int i = 0;
        if (receiver == 1) {
            result[i++] = Kind.REFERENCE.character;
        }
        for (int j = 0; j <= signature.numberOfParameters(); j++) {
            result[i++] = signature.parameterDescriptorAt(i).toKind().character;
        }
        return new String(result);
    }

    final Map<String, Adapter> adapters = new HashMap<String, Adapter>(1000);


    public final synchronized Adapter make(SignatureDescriptor signature, Type type, boolean isStatic) {
        String key = toAdapterSignature(signature, isStatic ? 0 : 1);
        Adapter adapter = adapters.get(key);
        if (adapter == null) {
            adapter = create(key, type);
            adapters.put(key, adapter);
        }
        return adapter;
    }

    protected abstract Adapter create(String signature, Type type);
}

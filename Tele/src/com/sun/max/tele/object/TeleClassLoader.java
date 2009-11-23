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
package com.sun.max.tele.object;

import com.sun.max.collect.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.tele.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for a  {@link ClassLoader} in the {@link TeleVM}.
 *
 * @author Michael Van De Vanter
 *
 */
public class TeleClassLoader extends TeleTupleObject implements ClassLoaderProvider {

    protected TeleClassLoader(TeleVM teleVM, Reference classLoaderReference) {
        super(teleVM, classLoaderReference);
        teleClassLoaders.append(this);
    }

    private static final AppendableSequence<TeleClassLoader> teleClassLoaders = new LinkSequence<TeleClassLoader>();

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        // We map all tele VM classloaders down into one on the local host VM
        return HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER;
    }

    public static Sequence<TeleClassLoader> teleClassLoaders() {
        return teleClassLoaders;
    }

    public ReferenceTypeProvider[] visibleClasses() {
        return teleVM().vmAccess().getAllReferenceTypes();
    }
}

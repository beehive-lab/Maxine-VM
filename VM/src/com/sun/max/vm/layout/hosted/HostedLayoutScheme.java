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
package com.sun.max.vm.layout.hosted;

import com.sun.max.vm.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;

/**
 * @author Bernd Mathiske
 */
public class HostedLayoutScheme extends LayoutScheme {

    public HostedLayoutScheme(VMConfiguration vmConfiguration, GripScheme gripScheme) {
        super(vmConfiguration,
              new HostedGeneralLayout(gripScheme),
              new HostedTupleLayout(gripScheme),
              new HostedHybridLayout(gripScheme),
              new HostedArrayHeaderLayout(gripScheme),
              new HostedByteArrayLayout(gripScheme),
              new HostedBooleanArrayLayout(gripScheme),
              new HostedShortArrayLayout(gripScheme),
              new HostedCharArrayLayout(gripScheme),
              new HostedIntArrayLayout(gripScheme),
              new HostedFloatArrayLayout(gripScheme),
              new HostedLongArrayLayout(gripScheme),
              new HostedDoubleArrayLayout(gripScheme),
              new HostedWordArrayLayout(gripScheme),
              new HostedReferenceArrayLayout(gripScheme));
    }

}

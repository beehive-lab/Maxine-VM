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
/*VCSID=2531a522-4b9c-410d-b82a-4b5019f66740*/
package com.sun.max.vm.layout.prototype;

import com.sun.max.vm.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;

/**
 * @author Bernd Mathiske
 */
public class PrototypeLayoutScheme extends LayoutScheme {

    public PrototypeLayoutScheme(VMConfiguration vmConfiguration, GripScheme gripScheme) {
        super(vmConfiguration,
              new PrototypeGeneralLayout(gripScheme),
              new PrototypeTupleLayout(gripScheme),
              new PrototypeHybridLayout(gripScheme),
              new PrototypeArrayHeaderLayout(gripScheme),
              new PrototypeByteArrayLayout(gripScheme),
              new PrototypeBooleanArrayLayout(gripScheme),
              new PrototypeShortArrayLayout(gripScheme),
              new PrototypeCharArrayLayout(gripScheme),
              new PrototypeIntArrayLayout(gripScheme),
              new PrototypeFloatArrayLayout(gripScheme),
              new PrototypeLongArrayLayout(gripScheme),
              new PrototypeDoubleArrayLayout(gripScheme),
              new PrototypeWordArrayLayout(gripScheme),
              new PrototypeReferenceArrayLayout(gripScheme));
    }

}

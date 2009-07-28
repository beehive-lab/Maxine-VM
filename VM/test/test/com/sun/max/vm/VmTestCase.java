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
package test.com.sun.max.vm;

import com.sun.max.ide.*;
import com.sun.max.platform.*;

/**
 * This class should be subclassed by any test case that uses types in the VM project.
 * It takes care of boot strapping the environment correctly. In particular, it ensures
 * that class initialization happens in the right order.
 *
 * @author Doug Simon
 */
public abstract class VmTestCase extends MaxTestCase {

    public VmTestCase() {
    }

    public VmTestCase(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        // This seems to work in terms of triggering class initialization
        // in the right order...
        Platform.host();
    }

}

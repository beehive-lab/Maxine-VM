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
package test.com.sun.max.vm;

import static com.sun.max.vm.VMConfiguration.*;

import java.io.*;

import junit.extensions.*;
import junit.framework.Test;

import org.junit.*;

import com.sun.max.ide.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.hosted.*;

/**
 * This test closure takes care of initializing a VM environment correctly. In particular, it ensures
 * that class initialization happens in the right order.
 *
 * @author Doug Simon
 */
public class VmTestSetup extends TestSetup {

    public VmTestSetup(Test test) {
        super(test);
    }

    protected void initializeVM() {
        VMConfigurator.installStandard(BuildLevel.DEBUG);
    }

    private boolean setupGuard;

    protected void chainedSetUp() {
        Trace.on(1);
        initializeVM();
        JavaPrototype.initialize(false);
    }

    /**
     * This is final so that errors occurring during setup are caught and reported - the JUnit
     * harness seems to silently swallow such errors and exit.
     */
    @Before
    @Override
    protected final void setUp() {
        try {
            assert !setupGuard;
            setupGuard = true;
            chainedSetUp();
        } catch (Error error) {
            error.printStackTrace();
            throw error;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } finally {
            setupGuard = false;
        }
    }

    @After
    @Override
    protected final void tearDown() {
        vmConfig().finalizeSchemes(Phase.RUNNING);
        ClassfileReader.writeClassfilesToJar(new File(JavaProject.findVcsProjectDirectory(), "loaded-classes.jar"));
    }
}

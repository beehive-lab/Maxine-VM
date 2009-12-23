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

import java.io.*;

import junit.extensions.*;
import junit.framework.Test;

import org.junit.*;

import com.sun.max.*;
import com.sun.max.ide.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.prototype.*;

/**
 * This test closure takes care of initializing a VM environment correctly. In particular, it ensures
 * that class initialization happens in the right order.
 *
 * @author Doug Simon
 */
public class VmTestSetup extends TestSetup {

    private static JavaPrototype javaPrototype = null;

    public VmTestSetup(Test test) {
        super(test);
    }

    protected VMConfiguration createVMConfiguration() {
        return VMConfigurations.createStandard(BuildLevel.DEBUG, Platform.host());
    }

    public static JavaPrototype javaPrototype() {
        return javaPrototype;
    }

    private boolean setupGuard;

    protected void chainedSetUp() {
        javaPrototype = createJavaPrototype();
    }

    protected JavaPrototype createJavaPrototype() {
        final PrototypeGenerator prototypeGenerator = new PrototypeGenerator(new OptionSet());
        Trace.on(1);
        return prototypeGenerator.createJavaPrototype(createVMConfiguration(), false);
    }

    public static PackageLoader packageLoader() {
        return javaPrototype.packageLoader();
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
        javaPrototype.vmConfiguration().finalizeSchemes(Phase.RUNNING);
        ClassfileReader.writeClassfilesToJar(new File(JavaProject.findVcsProjectDirectory(), "loaded-classes.jar"));
    }
}

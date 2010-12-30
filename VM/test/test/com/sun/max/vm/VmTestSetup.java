/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
        if (JavaPrototype.javaPrototype() == null) {
            initializeVM();
            JavaPrototype.initialize(false);
        }
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
        vmConfig().initializeSchemes(Phase.TERMINATING);
        if (false) {
            // Re-enable this when it proves useful. Otherwise, it just clutters the workspace with unwanted files.
            ClassfileReader.writeClassfilesToJar(new File(JavaProject.findWorkspaceDirectory(), "loaded-classes.jar"));
        }
    }
}

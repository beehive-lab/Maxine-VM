/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.output;

import java.lang.reflect.*;

import test.com.sun.max.vm.*;
import test.com.sun.max.vm.run.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;

/**
 * A run scheme that will run the tests in test.output that need VM access.
 */
public class VMOutputRunScheme extends AbstractTestRunScheme {

    private static final VMStringOption testOption = VMOptions.register(new VMStringOption("-XX:Test=", false, "", "test to run"), MaxineVM.Phase.PRISTINE);


    @HOSTED_ONLY
    public VMOutputRunScheme() {
        super("main");
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.STARTING) {
            noTests = VMOptions.parseMain(false);
            if (!noTests) {
                runTest();
            }
        } else {
            if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
                Class<?>[] testClasses = MaxineTesterConfiguration.findOutputTests("test.vm.output.");
                for (Class<?> klass : testClasses) {
                    addClassToImage(klass);
                }
            }
        }
    }

    private void runTest() {
        try {
            Class< ? > testClass = Class.forName(testOption.getValue());
            Method main = findMain(testClass);
            main.invoke(null, (Object) new String[0]);
        } catch (Exception ex) {
            ProgramError.unexpected("failed to execute main method in " + testOption.getValue(), ex);
        }
    }

    private Method findMain(Class<?> klass) {
        try {
            return klass.getDeclaredMethod("main", String[].class);
        } catch (NoSuchMethodException ex) {
            ProgramError.unexpected("failed to find main method in " + klass.getName(), ex);
            return null;
        }
    }
}

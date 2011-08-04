/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.verifier;

import java.io.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.verifier.*;

import test.com.sun.max.vm.*;

/**
 * A command line interface for running the Maxine bytecode verifier over a set of methods.
 */
public class CommandLineVerifier extends MethodFinder {

    static OptionSet options = new OptionSet();
    static Option<Policy> policy = options.newEnumOption("policy", Policy.DEFAULT, Policy.class,
        "Which verification policy to use: 'DEFAULT' to derive the policy from the class file version, " +
        "'OLD' to use the type inferencing verifier, 'NEW' to use the type checking verifier.");
    static Option<Boolean> verbose = options.newBooleanOption("verbose", false,
        "Enable verbose execution.");
    static Option<Boolean> failFast = options.newBooleanOption("fail-fast", false,
        "Exit after first verification error.");

    static enum Policy {
        DEFAULT, OLD, NEW;
    }

    public static void main(String[] args) {
        PrintStream log = System.out;

        TypeCheckingVerifier.FailOverToOldVerifier = false;

        options.parseArguments(args);
        String[] patterns = options.getArguments();

        log.println("Initializing verifier system... ");
        JavaPrototype.initialize(false);
        log.println("Initialized verifier system");

        log.println("Finding specified methods...");
        CommandLineVerifier clv = new CommandLineVerifier();
        List<MethodActor> methods = clv.find(patterns, Classpath.fromSystem(), CommandLineVerifier.class.getClassLoader(), null);
        log.println("Found " + methods.size() + " methods");
        if (verbose.getValue()) {
            Verifier.TraceVerification = "";
        }
        for (MethodActor method : methods) {
            if (method instanceof ClassMethodActor) {
                ClassMethodActor classMethodActor = (ClassMethodActor) method;
                ClassVerifier verifier = null;
                ClassActor holder = method.holder();
                switch (policy.getValue()) {
                    case DEFAULT:
                        verifier = Verifier.verifierFor(holder);
                        break;
                    case OLD:
                        verifier = new TypeInferencingVerifier(holder);
                        break;
                    case NEW:
                        if (holder.majorVersion < 50) {
                            // Cannot use new verifier on old class files
                            if (verbose.getValue()) {
                                log.println("Class file " + holder.name() + " version " + holder.majorVersion + " incompatible with new verifier; falling back to old verifier");
                            }
                            verifier = new TypeInferencingVerifier(holder);
                        } else {
                            verifier = new TypeCheckingVerifier(holder);
                        }
                        break;
                }
                if (!verbose.getValue()) {
                    log.println("Verifying " + method.format("%H.%n(%p)") + " via " + (verifier instanceof TypeCheckingVerifier ? "type-checking" : "type-inferecing"));
                }
                try {
                    verifier.verify(classMethodActor, classMethodActor.codeAttribute());
                } catch (LinkageError e) {
                    e.printStackTrace();
                    if (failFast.getValue()) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected ClassActor getClassActor(Class< ? > javaClass) {
        try {
            return super.getClassActor(javaClass);
        } catch (HostOnlyClassError e) {
            return null;
        }
    }

    @Override
    protected void addMethod(MethodActor method, List<MethodActor> methods) {
        if (method instanceof ClassMethodActor && !method.isAbstract() && !method.isIntrinsic()) {
            super.addMethod(method, methods);
        }
    }
}


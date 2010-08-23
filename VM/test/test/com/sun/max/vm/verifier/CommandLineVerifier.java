/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package test.com.sun.max.vm.verifier;

import java.io.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.verifier.*;

import test.com.sun.max.vm.*;

/**
 * A command line interface for running the Maxine bytecode verifier over a set of methods.
 *
 * @author Doug Simon
 */
public class CommandLineVerifier extends MethodFinder {

    static OptionSet options = new OptionSet();
    static Option<Policy> policy = options.newEnumOption("policy", Policy.DEFAULT, Policy.class,
        "Which verification policy to use: 'DEFAULT' to derive the policy from the class file version, " +
        "'OLD' to use the type inferencing verifier, 'NEW' to use the type checking verifier.");
    static Option<Integer> verbose = options.newIntegerOption("verbose", 1,
        "Verbosity level.");

    static enum Policy {
        DEFAULT, OLD, NEW;
    }

    public static void main(String[] args) {
        PrintStream log = System.out;

        options.parseArguments(args);
        String[] patterns = options.getArguments();

        if (verbose.getValue() > 0) {
            log.println("Initializing verifier system... ");
        }
        new PrototypeGenerator(new OptionSet()).createJavaPrototype(false);
        if (verbose.getValue() > 0) {
            log.println("Initialized verifier system");
        }

        if (verbose.getValue() > 0) {
            log.println("Finding specified methods...");
        }
        CommandLineVerifier clv = new CommandLineVerifier();
        List<MethodActor> methods = clv.find(patterns, Classpath.fromSystem(), CommandLineVerifier.class.getClassLoader());
        if (verbose.getValue() > 0) {
            log.println("Found " + methods.size() + " methods");
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
                            if (verbose.getValue() > 0) {
                                log.println("Class file " + holder.name() + " version " + holder.majorVersion + " incompatible with new verifier; falling back to old verifier");
                            }
                            verifier = new TypeInferencingVerifier(holder);
                        } else {
                            verifier = new TypeCheckingVerifier(holder);
                        }
                        break;
                }
                if (verbose.getValue() == 2) {
                    log.println("Verifying " + method.format("%H.%n(%p)"));
                } else if (verbose.getValue() > 2) {
                    verifier.verbose = true;
                }
                verifier.verify(classMethodActor, classMethodActor.originalCodeAttribute(false));
            }
        }
    }

    @Override
    protected void addMethod(MethodActor method, List<MethodActor> methods) {
        if (method instanceof ClassMethodActor && !method.isAbstract() && !method.isBuiltin() && !method.isIntrinsic()) {
            super.addMethod(method, methods);
        }
    }
}


/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.vm.cps.cir.optimize;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.operator.*;
import com.sun.max.vm.cps.cir.variable.*;

/**
 * EnvBasedDFA implements a data-flow analysis interpreter that knows how
 * to propagate abstract values and continuations to uses of variables and
 * continuation variables.  The user of this module can plug their own
 * abstract value domain and override analyzeJavaOperatorCall and analyseSwitchCal.
 * for giving special meaning to these operators.  Handling each operator
 * is usually done by further processing the reachable continuations in some
 * (possibly extended) environment (based on what we know about the semantics
 * of the operator itself: what value it produces and what information do we
 * know about the parameters passed to it.  The abstract values reachable to
 * call sites are then remembered in a hash table in order to used in some
 * transformations once the analysis is terminated.
 *
 * @author Aziz Ghuloum
 */
public class EnvBasedDFA <AV_Type extends AbstractValue<AV_Type>>{

    // Checkstyle: stop
    private final AbstractValueDomain<AV_Type> domain;
    // Checkstyle: resume

    private final IdentityHashMapping<CirCall, AV_Type[]> hashmap = new IdentityHashMapping<CirCall, AV_Type[]>();

    private EnqueuedCall<AV_Type> callQueue;

    /* Every time a block b is called with arguments a0, a1, a2, ..,
     * the mapping _blockParameters holds the types t0, t1, t2, ...,
     * such that t0 is the type of all the variables that could flow
     * into the parameters p0, p1, p2, ....
     *
     */
    private IdentityHashMapping<CirBlock, AV_Type[]> blockParameters = new IdentityHashMapping<CirBlock, AV_Type[]>();

    /* the blockQueue holds the blocks that are reachable but have
     * not been analysed yet.  the types of the arguments supplied
     * to these blocks are in _blockParameters.
     */
    private IdentitySet<CirBlock> blockQueue = new IdentitySet<CirBlock>();

    /* map a continuation variable -> set of continuations that may
     * be called if the var is used as a continuation.
     */
    IdentityHashMapping<CirContinuationVariable, IdentitySet<CirContinuation>> continuationHash = new IdentityHashMapping<CirContinuationVariable, IdentitySet<CirContinuation>>();

    public EnvBasedDFA(AbstractValueDomain<AV_Type> domain) {
        this.domain = domain;
    }

    /** analyse(closure) is the main interface for the analysis.
     * it analyses the body of the closure and returns a hash map
     * partially mapping each CirCall in the body of the closure
     * to an array of abstract values (typically one per parameter).
     *
     * @param initialEnv an initial environment. This may be null.
     */
    public IdentityHashMapping<CirCall, AV_Type[]> analyse(CirClosure closure, Environment<CirVariable, AV_Type> initialEnv) {
        final CirVariable[] parameters = closure.parameters();
        /* create an environment */
        Environment<CirVariable, AV_Type> env = initialEnv == null ? new Environment<CirVariable, AV_Type>() : initialEnv;
        for (CirVariable var : parameters) {
            /* all uninitialized parameters are initialized to bottom since they can
             * effectively be anything (conservative since we assume that
             * a method can be called from anywhere
             */
            if (env.lookup(var) == null) {
                env = env.extend(var, domain.getBottom());
            }
        }
        /* start it up */
        analyzeCall(closure.body(), env);
        /* do more */
        reachFixedPoint();
        /* and we're done */
        return hashmap;
    }

    /** rememberMapping takes a CirCall and an array of abstract values
     * and records (in the main _hashmap) a mapping from the call to
     * these abstract values.  Multiple calls to rememberMapping do the
     * right thing by merging the newly added values to the existing one,
     * resulting in the least-general abstract value that meets both sets.
     */
    protected void rememberMapping(CirCall call, AV_Type... t2) {
        final AV_Type[] t1 = hashmap.get(call);
        if (t1 == null) {
            hashmap.put(call, t2);
        } else {
            assert t1.length == t2.length;
            for (int i = 0; i < t1.length; i++) {
                t1[i] = domain.meet(t1[i], t2[i]);
            }
        }
    }

    /** analyzeCall takes one extra step through the analysis.  It takes
     *  a CirCall and an environment and properly takes care of handling:
     *  1. calls to known lambdas (basically a "let"), in which the
     *     abstract values of the actual parameters  are copy-propagated
     *     to the formal parameters and the body of the lambda is analysed
     *     in the extended environment.
     *  2. calls to blocks: the block and the abstract values of the parameters
     *     are enqueued to be processed later.  See {@link enqueueBlockCall}.
     *  3. calls to continuation variables propagate the parameters to all
     *     continuations that could possibly be bound to that variable.
     *     These are recorded in _continuationHash hash table.
     *  4. calls to {@link JavaOperator} and {@link CirSwitch} are delegated
     *     to analyzeJavaOperatorCall and analyzeSwitchCall.
     */
    private void analyzeCall(CirCall call, Environment<CirVariable, AV_Type> env) {
        final CirValue op = call.procedure();
        if (op instanceof CirBlock) {
            final CirBlock block = (CirBlock) op;
            enqueueBlockCall(block, lookupArgs(call.arguments(), block.closure().parameters(), env));
        } else if (op instanceof CirClosure) {
            final CirClosure closure = (CirClosure) op;
            final AV_Type[] types = lookupArgs(call.arguments(), closure.parameters(), env);
            enqueueCall(closure.body(), buildEnv(closure.parameters(), types, env));
        } else if (op instanceof CirContinuationVariable) {
            final IdentitySet<CirContinuation> continuations = continuationHash.get((CirContinuationVariable) op);
            if (continuations != null) {
                for (CirContinuation k : continuations) {
                    final AV_Type[] types = lookupArgs(call.arguments(), k.parameters(), env);
                    enqueueCall(k.body(), buildEnv(k.parameters(), types, env));
                }
            }
        } else if (op instanceof JavaOperator) {
            analyzeJavaOperatorCall(call, env);
        } else if (op instanceof CirSwitch) {
            analyzeSwitchCall(call, env);
        } else {
            ProgramError.unexpected("analyseCall found operator " + op);
        }
    }

    /** The default implementation of analyzeSwitchCall simply visits all
     * continuation parameters (assumes all are reachable, and no abstract
     * value can be deduced from the operation itself.
     *
     * Specializing this operation gives branch-sensitive analysis.
     */
    protected void analyzeSwitchCall(CirCall call, Environment<CirVariable, AV_Type> env) {
        final CirSwitch op = (CirSwitch) call.procedure();
        final CirValue[] args = call.arguments();
        final int n = op.numberOfMatches();
        for (int i = 1; i <= n; i++) {
            final CirValue branchContinuation = args[n + i];
            visitContinuation(branchContinuation, env);
        }
        final CirValue defaultContinuation = args[args.length - 1];
        visitContinuation(defaultContinuation, env);
    }

    /** The default implementation of analyzeJavaOperatorCall simply visits
     * the two continuation parameters (assumes all are reachable, and no
     * abstract value can be deduced from the operation itself.
     *
     * Specialize this operation method by injecting more information to
     * each continuation when possible.  Make sure all reachable
     * continuations are actually visit (missing some will produce incorrect
     * results).
     */
    protected void analyzeJavaOperatorCall(CirCall call, Environment<CirVariable, AV_Type> env) {
        final CirValue[] args = call.arguments();
        assert args.length >= 2;
        final CirValue k0 = args[args.length - 2];
        final CirValue k1 = args[args.length - 1];
        visitContinuation(k0, env);
        visitContinuation(k1, env);
    }

    /** visitContinuation is called every time we discover than a continuation is reachable.
     * There are three possibilities here:
     * 1. the continuation is an explicit lambda, so, we process its body in an
     *    extended environment mapping lambda parameter (if one exists) to the abstract
     *    value to the received.
     * 2. The continuation is a continuation variable.  We get all continuations that
     *    could possibly be bound to that variable and apply the abstract value to each
     *    such continuation.
     *  3. The continuation is undefined (cannot be reachable), and we do nothing.
     */
    protected void visitContinuation(CirValue kv, AV_Type t, Environment<CirVariable, AV_Type> env) {
        if (kv instanceof CirContinuation) {
            final CirContinuation k = (CirContinuation) kv;
            final CirVariable[] params = k.parameters();
            if (params.length == 1) {
                enqueueCall(k.body(), env.extend(params[0], t));
            } else {
                enqueueCall(k.body(), env);
            }
        } else if (kv instanceof CirContinuationVariable) {
            final IdentitySet<CirContinuation> continuations = continuationHash.get((CirContinuationVariable) kv);
            if (continuations != null) {
                for (CirContinuation k : continuations) {
                    final CirVariable[] params = k.parameters();
                    if (params.length == 1) {
                        enqueueCall(k.body(), env.extend(params[0], t));
                    } else {
                        enqueueCall(k.body(), env);
                    }
                }
            }
        } else if (kv == CirValue.UNDEFINED) {
            /* do nothing */
        } else {
            ProgramError.unexpected("unhandled " + kv);
        }
    }

    protected void visitContinuation(CirValue kv, Environment<CirVariable, AV_Type> env) {
        visitContinuation(kv, domain.getBottom(), env);
    }

    private static class EnqueuedCall<Type>{
        CirCall call;
        Environment<CirVariable, Type> env;
        EnqueuedCall<Type> next;
        public EnqueuedCall(CirCall call, Environment<CirVariable, Type> env, EnqueuedCall<Type> queue) {
            this.call = call;
            this.env = env;
            this.next = queue;
        }
    }

    private void enqueueCall(CirCall call, Environment<CirVariable, AV_Type> env) {
        callQueue = new EnqueuedCall<AV_Type>(call, env, callQueue);
    }

    private void enqueueBlockCall(CirBlock block, AV_Type[] ts) {
        final AV_Type[] orig = blockParameters.get(block);
        if (orig == null) {
            blockParameters.put(block, ts);
            blockQueue.add(block);
            return;
        }
        boolean changed = false;
        for (int i = 0; i < orig.length; i++) {
            final AV_Type t0 = orig[i];
            final AV_Type t1 = domain.meet(t0, ts[i]);
            if (!domain.equal(t1, t0)) {
                orig[i] = t1;
                changed = true;
            }
        }
        if (changed) {
            blockQueue.add(block);
        }
    }

    protected AV_Type lookupArg(CirValue arg, Environment<CirVariable, AV_Type> env) {
        if (arg instanceof CirVariable) {
            final AV_Type t = env.lookup((CirVariable) arg);
            assert t != null;
            return t;
        } else if (arg instanceof CirConstant) {
            return domain.fromConstant((CirConstant) arg);
        } else {
            ProgramError.unexpected("arg: " + arg);
        }
        return null;
    }

    private AV_Type[] lookupArgs(CirValue[] arguments, CirVariable[] params, Environment<CirVariable, AV_Type> env) {
        final AV_Type[] ts = domain.getBottom().createArray(arguments.length);
        for (int i = 0; i < ts.length; i++) {
            if (params[i] instanceof CirContinuationVariable) {
                if (arguments[i] instanceof CirContinuationVariable) {
                    recordContinuationFlow((CirContinuationVariable) params[i], continuationHash.get((CirContinuationVariable) arguments[i]));
                } else if (arguments[i] instanceof CirContinuation) {
                    recordContinuationFlow((CirContinuationVariable) params[i], (CirContinuation) arguments[i]);
                } else {
                    ProgramError.unexpected("continuation parameter " + params[i] + " with noncontinuation argument " + arguments[i]);
                }
            } else {
                ts[i] = lookupArg(arguments[i], env);
            }
        }
        return ts;
    }

    private void recordContinuationFlow(CirContinuationVariable var, IdentitySet<CirContinuation> ks) {
        if (ks != null) {
            IdentitySet<CirContinuation> set = continuationHash.get(var);
            if (set == null) {
                set = new IdentitySet<CirContinuation>();
                continuationHash.put(var, set);
            }
            for (CirContinuation k : ks) {
                set.add(k);
            }
        }
    }

    private void recordContinuationFlow(CirContinuationVariable var, CirContinuation k) {
        IdentitySet<CirContinuation> set = continuationHash.get(var);
        if (set == null) {
            set = new IdentitySet<CirContinuation>();
            continuationHash.put(var, set);
        }
        set.add(k);
    }

    private void reachFixedPoint() {
        boolean more;
        do {
            more = false;
            while (callQueue != null) {
                more = true;
                final EnqueuedCall<AV_Type> q = callQueue;
                callQueue = q.next;
                analyzeCall(q.call, q.env);
            }
            final IdentitySet<CirBlock> oldBlockQueue = this.blockQueue;
            blockQueue = new IdentitySet<CirBlock>();
            for (CirBlock b : oldBlockQueue) {
                more = true;
                analyzeCall(b.closure().body(), buildEnv(b.closure().parameters(), blockParameters.get(b)));
            }
        } while (more);
        assert callQueue == null;
        assert blockQueue.numberOfElements() == 0 : "remaining " + blockQueue.numberOfElements();
    }

    private Environment<CirVariable, AV_Type> buildEnv(CirVariable[] parameters, AV_Type[] types) {
        return buildEnv(parameters, types, new Environment<CirVariable, AV_Type>());
    }

    private Environment<CirVariable, AV_Type> buildEnv(CirVariable[] parameters, AV_Type[] types, Environment<CirVariable, AV_Type> e) {
        Environment<CirVariable, AV_Type> env = e;
        for (int i = 0; i < parameters.length; i++) {
            env = env.extend(parameters[i], types[i]);
        }
        return env;
    }
}

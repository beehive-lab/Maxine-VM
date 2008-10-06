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
/*VCSID=f5c34db6-4506-4e1f-bee3-b7dc1ca8a24a*/
package com.sun.max.vm.compiler.cir.optimize;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.optimize.DefsetDomain.*;
import com.sun.max.vm.compiler.cir.optimize.TypesetDomain.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;


public final class ClassTypeAnalysis extends DataflowDriver<Typeset> {
    private final DefsetAnalysis _defset;
    private final NameAnalysis _name;
    private final TypesetDomain _domain;
    private final ClassTypeVisitor _visitor;

    public final class ClassTypeVisitor extends JavaOperatorDataflowSideEffectPropagateVisitor<TypesetDomain.Typeset> {
        @Override
        public void visit(New op) {
            if (op.isResolved()) {
                driver().flowInto(domain().fromClassActor(op.classActor()), _vark);
                driver().flowInto(domain().fromInstanceOf(InterfaceActor.fromJava(Throwable.class)), _varek);
            } else {
                visitDefault(op);
            }
        }

        @Override
        protected void onSplit(SplitTransformation.Split split, CirVariable oldv, CirVariable newv) {
            final CirValue tag = split.switchCall().arguments()[0];
            final CirSwitch sw = (CirSwitch) (split.switchCall().procedure());

            if (sw.numberOfMatches() == 1) {
                //TODO: currently we only process if (two-way switch)
                final CirValue pattern = split.switchCall().arguments()[1];
                //  tag matches pattern
                if (tag instanceof CirVariable && pattern instanceof CirConstant && driver().get(oldv) != null) {
                    final ValueComparator comp;
                    if (split.branchOrder() == 0) {
                        comp = sw.valueComparator().complement();
                    } else {
                        comp = sw.valueComparator();
                    }

                    final TypesetDomain.Typeset typeSet = interpret((CirVariable) tag, comp, (CirConstant) pattern, oldv);
                    driver().flowInto(typeSet, newv);
                }
            }
        }


        @Override
        public void visitDefault(JavaOperator op) {
            if (_vark != null) {
                driver().flowInto(_domain.getBottom(), _vark);
            }
            if (_varek != null) {
                driver().flowInto(_domain.fromInstanceOf(InterfaceActor.fromJava(Throwable.class)), _varek);
            }
        }

        /* given tag comp pattern holds, return the abstractvalue for var */
        public TypesetDomain.Typeset interpret(CirVariable tag, ValueComparator comp, CirConstant pattern, CirVariable var) {
            final TypesetDomain.Typeset set = driver().get(var);
            final Defset defset = _defset.get(tag);

            if (defset == null || defset.getSet() == null) {
                return set;
            }
            // TODO: currently we only deal with the case the where instanceof holds
            if (pattern.kind() != Kind.INT || pattern.value().asInt() != 0 || comp != ValueComparator.NOT_EQUAL) {
                return set;
            }
            TypesetDomain.Typeset typesets = _domain.getTop();

            for (CirNode node : defset.getSet()) {
                if (!(node instanceof CirCall)) {
                    return set;
                }
                final CirCall call = (CirCall) node;
                if (!(call.procedure() instanceof InstanceOf) ||
                    ((InstanceOf) call.procedure()).isResolved() == false ||
                    !(call.arguments()[0] instanceof CirVariable) ||
                    _name.get((CirVariable) (call.arguments()[0])) != _name.get(var)) {
                    return set;
                }
                final ClassActor thisClassActor = ((InstanceOf) call.procedure()).classActor();

                if (comp == ValueComparator.NOT_EQUAL) {
                    // instanceof holds
                    typesets = _domain.meet(typesets, _domain.fromInstanceOf(thisClassActor));
                }
            }
            return typesets.intersect(set);
        }

        @Override
        protected ClassTypeAnalysis driver() {
            return ClassTypeAnalysis.this;
        }
    }

    public ClassTypeAnalysis(DefsetAnalysis defset, NameAnalysis name, TypesetDomain domain) {
        _visitor = new ClassTypeVisitor();
        _domain = domain;
        _name = name;
        _defset = defset;
    }

    @Override
    protected JavaOperatorDataflowSideEffectPropagateVisitor<Typeset> javaOperatorDataflowSideEffectPropagateVisitor() {
        return _visitor;
    }

    @Override
    protected TypesetDomain domain() {
        return _domain;
    }
}

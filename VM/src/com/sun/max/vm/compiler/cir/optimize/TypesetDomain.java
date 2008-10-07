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
package com.sun.max.vm.compiler.cir.optimize;

import java.util.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.cir.*;

public final class TypesetDomain extends AbstractValueDomain<TypesetDomain.Typeset> {

    public static final TypesetDomain singleton = new TypesetDomain();
    public final Typeset _top;
    public final Typeset _bottom;

    private TypesetDomain() {
        _top = new Typeset();
        _bottom = new Typeset();
    }

    /**
     * Typeset defines the abstract value domain for ClassTypeAnalysis Its internal representation is a set of TypeElements.
     *
     * @see TypesetElement
     *
     * @author Yi Guo
     */
    public final class Typeset extends AbstractValue<Typeset> {

        public Typeset() {
            _set = new HashSet<TypesetElement>();
        }

        public boolean isEmpty() {
            return _set.isEmpty();
        }

        public boolean containsAll(Typeset v) {
            // this <= v    <=>    this is superset of v
            for (TypesetElement ve : v._set) {
                for (TypesetElement e : _set) {
                    boolean cover = false;
                    if (ve.isSubsetOf(e)) {
                        cover = true;
                        break;
                    }
                    if (!cover) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean lessOrEqualNontrivial(Typeset v) {
            return this.containsAll(v);
        }

        @Override
        public boolean eqNontrivial(Typeset v) {
            return this.lessOrEqualNontrivial(v) && v.lessOrEqualNontrivial(this);
        }

        public Typeset union(Typeset in) {
            if (in.isTop() || this.isBottom()) {
                return this;
            }

            if (this.isTop() || in.isBottom()) {
                return in;
            }

            final Typeset result = new Typeset();
            result._set.addAll(this._set);
            for (TypesetElement inE : in._set) {
                result.addElement(inE);
            }

            if (result._set.equals(this._set)) {
                return this;
            }
            if (result._set.equals(in._set)) {
                return in;
            }
            return result;
        }

        public Set<TypesetElement> getSet() {
            return _set;
        }

        public Typeset intersect(Typeset in) {
            if (in.isTop() || in.isBottom()) {
                return this;
            }

            if (this.isTop() || this.isBottom()) {
                return in;
            }

            Typeset result = getTop();
            for (TypesetElement a : this._set) {
                for (TypesetElement b : in._set) {
                    final Typeset intersect = a.intersect(b);
                    result = result.union(intersect);
                }
            }
            return result;
        }

        @Override
        public String toString() {
            if (this.isBottom()) {
                return "BOTTOM";
            }
            if (this.isTop()) {
                return "TOP";
            }
            final StringBuilder s = new StringBuilder();
            for (TypesetElement e : _set) {
                if (s.length() > 0) {
                    s.append(", ");
                }
                s.append(e.toString());
            }
            return s.toString();
        }

        private final Set<TypesetElement> _set;

        private boolean addElement(TypesetElement inE) {
            if (inE.isEmpty()) {
                return false;
            }
            final Set<TypesetElement> removeSet = new HashSet<TypesetElement>();
            for (TypesetElement e : this._set) {
                if (inE.isSubsetOf(e)) {
                    return false;
                } else if (e.isSubsetOf(inE)) {
                    removeSet.add(e);
                }
            }
            this._set.removeAll(removeSet);
            this._set.add(inE);
            return true;
        }

        @Override
        boolean isBottom() {
            return this == _bottom;
        }

        @Override
        boolean isTop() {
            return this == _top;
        }

        @Override
        Typeset meetNontrivial(Typeset v) {
            return this.union(v);
        }

        @Override
        Typeset[] createArray(int length) {
            return new Typeset[length];
        }
    }

    /**
     * Each TypesetElement describes a set of possible class types. It has two sub-classes: SingleElement or
     * SubtreeElement
     *
     * SingleElement represents just one class type SubtreeElement represents all classes that derive from the root
     * (including the root itself) and implement all interface restrictions.
     *
     * EMPTY is a special TypesetElement.
     *
     * @author Yi Guo
     */
    private final TypesetElement _empty = new SingleElement(null);

    public abstract class TypesetElement {

        protected final ClassActor _classActor;

        private TypesetElement(ClassActor classActor) {
            _classActor = classActor;
        }

        public ClassActor classActor() {
            return _classActor;
        }

        public boolean isEmpty() {
            return this == _empty;
        }

        public abstract boolean isSubsetOf(TypesetElement e);

        public Typeset intersect(TypesetElement e) {
            final Typeset result = new Typeset();
            if (e instanceof SingleElement) {
                if (e.isSubsetOf(this)) {
                    result.addElement(e);
                }
            } else {
                final SubtreeElement sub = (SubtreeElement) e;
                if (this instanceof SingleElement) {
                    result.addElement(sub.intersect((SingleElement) this));
                } else {
                    result.addElement(sub.intersect((SubtreeElement) this));
                }
            }
            return result;
        }

        public Typeset union(TypesetElement e) {
            final Typeset result = new Typeset();
            if (this.isSubsetOf(e)) {
                result.addElement(e);
            } else if (e.isSubsetOf(this)) {
                result.addElement(this);
            } else {
                result.addElement(e);
                result.addElement(this);
            }
            return result;
        }
    }

    public class SingleElement extends TypesetElement {

        public SingleElement(ClassActor classActor) {
            super(classActor);
        }

        public SubtreeElement union(SubtreeElement sub) {
            return sub.union(this);
        }

        public boolean isSubsetOf(SubtreeElement sub) {
            if (this.isEmpty()) {
                return true;
            }
            if (sub.classActor().isAssignableFrom(this.classActor()) == false) {
                return false;
            }
            for (InterfaceActor interfaceActor : sub.interfaceRestrictions()) {
                if (interfaceActor.isAssignableFrom(this.classActor()) == false) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            if (this.isEmpty()) {
                return "Empty";
            }

            return classActor().toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof SingleElement) {
                return _classActor == ((SingleElement) o).classActor();
            }
            return false;
        }

        @Override
        public boolean isSubsetOf(TypesetElement e) {
            if (e instanceof SingleElement) {
                return this.equals(e);
            }
            return this.isSubsetOf((SubtreeElement) e);
        }
    }

    public class SubtreeElement extends TypesetElement {

        private final Set<InterfaceActor> _interfaceRestrictions;

        public SubtreeElement(ClassActor classActor) {
            super(classActor);
            _interfaceRestrictions = new HashSet<InterfaceActor>();
        }

        public Set<InterfaceActor> interfaceRestrictions() {
            return _interfaceRestrictions;
        }

        public SubtreeElement union(SingleElement single) {
            if (single.isEmpty()) {
                return this;
            }
            final Set<InterfaceActor> removeSet = new HashSet<InterfaceActor>();

            ClassActor commonSuperClassActor = this.classActor();
            while (commonSuperClassActor.isAssignableFrom(single.classActor())) {
                commonSuperClassActor = commonSuperClassActor.superClassActor();
            }

            for (InterfaceActor a : this.interfaceRestrictions()) {
                if (a.isAssignableFrom(single.classActor()) == false) {
                    removeSet.add(a);
                }
            }

            if (commonSuperClassActor != this.classActor() || removeSet.isEmpty() == false) {
                final SubtreeElement result = new SubtreeElement(commonSuperClassActor);
                result.interfaceRestrictions().addAll(this.interfaceRestrictions());
                result.interfaceRestrictions().removeAll(removeSet);
                return result;
            }
            return this;
        }

        public SubtreeElement unionImprecise(SubtreeElement subtree) {
            ClassActor commonSuperClassActor = this.classActor();
            while (commonSuperClassActor.isAssignableFrom(subtree.classActor())) {
                commonSuperClassActor = commonSuperClassActor.superClassActor();
            }

            final Set<InterfaceActor> ir = new HashSet<InterfaceActor>();
            for (InterfaceActor a : this.interfaceRestrictions()) {
                boolean addc = false;
                InterfaceActor c = a;
                for (InterfaceActor b : subtree.interfaceRestrictions()) {
                    if (b.isAssignableFrom(c)) {
                        c = b;
                        addc = true;
                    }
                }
                if (addc) {
                    ir.add(c);
                }
            }

            if (commonSuperClassActor == this.classActor() && ir.equals(this.interfaceRestrictions())) {
                return this;
            }
            if (commonSuperClassActor == subtree.classActor() && ir.equals(subtree.interfaceRestrictions())) {
                return subtree;
            }
            final SubtreeElement result = new SubtreeElement(commonSuperClassActor);
            result._interfaceRestrictions.addAll(ir);
            return result;
        }

        public TypesetElement intersect(SingleElement single) {
            if (single.isSubsetOf(this)) {
                return single;
            }
            return _empty;
        }

        public TypesetElement intersect(SubtreeElement subtree) {
            final ClassActor classActor;
            if (this.classActor().isAssignableFrom(subtree.classActor())) {
                classActor = subtree.classActor();
            } else if (subtree.classActor().isAssignableFrom(this.classActor())) {
                classActor = this.classActor();
            } else {
                return _empty;
            }

            final Set<InterfaceActor> ir = new HashSet<InterfaceActor>();
            ir.addAll(this.interfaceRestrictions());

            for (InterfaceActor b : subtree.interfaceRestrictions()) {
                boolean discardB = false;
                final Set<InterfaceActor> removeSet = new HashSet<InterfaceActor>();
                for (InterfaceActor a : ir) {
                    if (b.isAssignableFrom(a)) {
                        discardB = true;
                    } else if (a.isAssignableFrom(b)) {
                        removeSet.add(a);
                    }
                }
                ir.removeAll(removeSet);
                if (!discardB) {
                    ir.add(b);
                }
            }

            if (classActor == this.classActor() && ir.equals(this.interfaceRestrictions())) {
                return this;
            }
            if (classActor == subtree.classActor() && ir.equals(subtree.interfaceRestrictions())) {
                return subtree;
            }
            final SubtreeElement result = new SubtreeElement(classActor);
            result._interfaceRestrictions.addAll(ir);
            return result;
        }

        public boolean isSubsetOf(SubtreeElement subtree) {
            return this.intersect(subtree).equals(this);
        }

        @Override
        public String toString() {
            final StringBuilder stringBuilder = new StringBuilder();
            if (_interfaceRestrictions != null) {
                for (InterfaceActor e : _interfaceRestrictions) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(",");
                    }
                    stringBuilder.append(e);
                }
            }
            return _classActor + "+[" + stringBuilder.toString() + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof SubtreeElement) {
                final SubtreeElement e = (SubtreeElement) o;
                return this.classActor() == e.classActor() && this.interfaceRestrictions().equals(e.interfaceRestrictions());
            }
            return false;
        }

        @Override
        public boolean isSubsetOf(TypesetElement e) {
            if (e instanceof SingleElement) {
                return false;
            }
            return this.isSubsetOf((SubtreeElement) e);
        }
    }

    public Typeset fromClassActor(ClassActor classActor) {
        final Typeset s = new Typeset();
        assert classActor.isInterfaceActor() == false;
        s.addElement(new SingleElement(classActor));
        return s;
    }

    public Typeset fromInstanceOf(ClassActor classActor) {
        final Typeset s = new Typeset();
        if (classActor.isInterfaceActor() == false) {
            s.addElement(new SubtreeElement(classActor));
        } else {
            final ClassActor objectClassActor = ClassActor.fromJava(Object.class);
            final SubtreeElement e = new SubtreeElement(objectClassActor);
            final InterfaceActor interfaceActor = (InterfaceActor) classActor;
            e.interfaceRestrictions().add(interfaceActor);
            s.addElement(e);
        }
        return s;
    }

    @Override
    public Typeset getBottom() {
        return _bottom;
    }

    @Override
    public Typeset getTop() {
        return _top;
    }

    @Override
    public Typeset fromConstant(CirConstant c) {
        return getBottom();
    }

}

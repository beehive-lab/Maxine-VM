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
package com.sun.max.vm.compiler.deps;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.deps.Dependencies.ClassDeps;
import com.sun.max.vm.compiler.deps.Dependencies.DependencyVisitor;


/**
 * A {@linkplain DependencyProcessor} handles the dependency-specific aspects of dependency processing,
 * specifically the conversion between the abstract, object-based, view of {@link CiAssumptions.Assumption}
 * and the encoded packed view used in a {@link Dependencies} object.
 *
 */
public abstract class DependencyProcessor {

    /**
     * Unique id for this {@linkplain DependencyProcessor} in range {@code 0 .. number dependency processors - 1}.
     */
    public final int id;

    /**
     * Unique bit mask for this {@linkplain DependencyProcessor}.
     * Defined as {@code 1 << id}
     */
    public final short bitMask;

    final boolean hasData;

    protected DependencyProcessor(Class<? extends CiAssumptions.Assumption> assumptionClass) {
        this(assumptionClass, true);
    }

    protected DependencyProcessor(Class<? extends CiAssumptions.Assumption> assumptionClass, boolean hasData) {
        id = DependenciesManager.registerDependencyProcessor(this, assumptionClass);
        bitMask = (short) (1 << id);
        this.hasData = hasData;
    }

    /**
     * Validate the given assumption, updating the packed dependencies structure if valid.
     * @param assumption to validate
     * @param classDeps packed dependencies for context {@link ClassActor} of this assumption
     * @return {@code true} if the assumption is valid, {@code false otherwise}
     */
    protected abstract boolean validate(CiAssumptions.Assumption assumption, ClassDeps classDeps);

    @HOSTED_ONLY
    @Override
    public String toString() {
        return getClass().getName() + ": " + bitMask;
    }

    /**
     * Tagging interface for the {@linkplain DependencyProcessor} component of visitors.
     */
    public interface DependencyProcessorVisitor {
    }

    /**
     * Mandatory visitor class that supports {@link String#toString} for tracing.
     */
    public static abstract class ToStringDependencyProcessorVisitor implements DependencyProcessorVisitor {
        protected StringBuilder sb;

        protected ToStringDependencyProcessorVisitor(StringBuilder sb) {
            this.sb = sb;
        }

        protected ToStringDependencyProcessorVisitor() {
        }

        void setStringBuilder(StringBuilder sb) {
            this.sb = sb;
        }
    }

    /**
     * Return the {@linkplain ToStringDependencyProcessorVisitor} for this processor.
     * @return
     */
    protected abstract ToStringDependencyProcessorVisitor getToStringDependencyProcessorVisitor();

    /**
     * Checks if {@code dependenciesVisitor} implements the {@linkplain DependencyProcessorVisitor} defined
     * by this processor.
     * @param dependencyVisitor
     * @return {@code null} if {@code dependencyVisitor} does not implement this processor's
     *         {@linkplain DependencyProcessorVisitor}, otherwise {@code dependencyVisitor} cast to that type.
     */
    protected abstract DependencyProcessorVisitor match(DependencyVisitor dependencyVisitor);

    /**
     * Visit a specific dependency invoking the given {@linkplain DependencyProcessorVisitor}, which may be {@code null},
     * indicating that the data need only be skipped (as efficiently as possible).
     * N.B. If not {@code null}, {@code dependencyProcessorVisitor} must be an instance of the {@linkplain DependencyProcessorVisitor}
     * defined by this processor.
     *
     * @param dependencyProcessorVisitor visitor to apply
     * @param context {@linkplain ClassActor class actor}
     * @param dependencies the associated{@linkplain Dependencies} instance
     * @param index start index of the dependency
     * @return the index of the next dependency or {@code -1} to terminate the visit.
     */
    protected abstract int visit(DependencyProcessorVisitor dependencyProcessorVisitor, ClassActor context, Dependencies dependencies, int index);

}

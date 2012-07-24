/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.hosted;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;

@HOSTED_ONLY
public final class ReferenceLiteralLocationRecorder implements TargetMethod.Closure {
    final CodeRegion codeRegion;
    final Object literalValue;
    int numLocations = 0;
    int numReferencingMethods = 0;
    int [] literalLocations;
    TargetMethod [] referencingTargetMethods;

    public ReferenceLiteralLocationRecorder(CodeRegion codeRegion, Object literalValue) {
        this.codeRegion = codeRegion;
        this.literalValue = literalValue;
        // There's at most one card table literal per method. Pre-allocate the array to that max, we'll trim it latter.
        referencingTargetMethods = new TargetMethod[codeRegion.numTargetMethods()];
        codeRegion.doAllTargetMethods(this);
    }

    @Override
    public boolean doTargetMethod(TargetMethod targetMethod) {
        if (targetMethod.numberOfReferenceLiterals() == 0) {
            return true;
        }
        Object [] referenceLiterals = targetMethod.referenceLiterals();
        int count = 0;
        for (int i = 0; i < referenceLiterals.length; i++) {
            if (referenceLiterals[i] == literalValue) {
                count++;
            }
        }
        if (count > 0) {
            numLocations += count;
            referencingTargetMethods[numReferencingMethods++] = targetMethod;
        }
        return true;
    }

    public void fillLiteralLocations() {
        final ArrayLayout arrayLayout = Layout.referenceArrayLayout();
        final int log2WordSize = Word.widthValue().log2numberOfBytes;
        int index = 0;
        for (int m = 0; m < numReferencingMethods; m++) {
            Object [] referenceLiterals = referencingTargetMethods[m].referenceLiterals();
            Address referenceLiteralsAddress = Heap.objectToCell(referenceLiterals).asPointer();
            for (int i = 0; i < referenceLiterals.length; i++) {
                if (referenceLiterals[i] == literalValue) {
                    final Address literalRefAddress = referenceLiteralsAddress.plus(arrayLayout.getElementOffsetInCell(i));
                    assert codeRegion.contains(literalRefAddress);
                    literalLocations[index++] = literalRefAddress.minus(codeRegion.start()).toInt() >> log2WordSize;
                }
            }
        }
    }

    public int [] getLiteralLocations() {
        if (literalLocations == null) {
            literalLocations = new int[numLocations];
        }
        return literalLocations;
    }
}

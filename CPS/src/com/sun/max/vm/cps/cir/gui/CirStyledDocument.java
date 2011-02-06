/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.cir.gui;

import static com.sun.max.vm.cps.collect.ListBag.MapType.*;

import java.util.*;

import javax.swing.text.*;

import com.sun.max.collect.*;
import com.sun.max.util.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.collect.*;

/**
 * A styled document derived from an annotated CIR trace.
 *
 * @author Doug Simon
 * @author Sumeet Panchal
 */
class CirStyledDocument extends DefaultStyledDocument {

    public int collapsedOffset;
    public CirAnnotatedTrace.ParenthesisElement collapsedDual;
    public CirAnnotatedTrace cirAnnotatedTrace;
    public IntHashMap<CirAnnotatedTrace.Element> offsetToElement = new IntHashMap<CirAnnotatedTrace.Element>();
    public ListBag<CirNode, CirAnnotatedTrace.Element> elementsPerNode = new ListBag<CirNode, CirAnnotatedTrace.Element>(IDENTITY);

    public CirStyledDocument(CirAnnotatedTrace cirAnnotatedTrace) {
        this.collapsedOffset = -1;
        this.collapsedDual = null;
        this.cirAnnotatedTrace = cirAnnotatedTrace;
        if (cirAnnotatedTrace == null) {
            try {
                insertString(0, "NO VISUALIZATION AVAILABLE", null);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            return;
        }
        for (final CirAnnotatedTrace.Element element : cirAnnotatedTrace) {
            final CirNode node = element.node();
            if (node != null) {
                elementsPerNode.add(node, element);
            }
            element.visitRanges(new RangeVisitor() {
                public void visitRange(Range range) {
                    for (int i = range.start(); i != range.end(); ++i) {
                        offsetToElement.put(i, element);
                    }
                }
            });
        }
    }

    public CirAnnotatedTrace.Element elementAt(int offset) {
        return offsetToElement.get(offset);
    }

    public List<CirAnnotatedTrace.Element> occurrences(CirAnnotatedTrace.Element element) {
        if (element.node() == null) {
            return Arrays.asList(element);
        }
        final List<CirAnnotatedTrace.Element> occurrences = elementsPerNode.get(element.node());
        return occurrences;
    }
}

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

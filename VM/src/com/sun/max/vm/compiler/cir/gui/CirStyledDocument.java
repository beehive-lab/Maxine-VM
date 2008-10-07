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
/*VCSID=af04c692-e124-440b-b3b8-c20c15d7c9de*/
package com.sun.max.vm.compiler.cir.gui;

import static com.sun.max.collect.SequenceBag.MapType.*;

import javax.swing.text.*;

import com.sun.max.collect.*;
import com.sun.max.util.*;
import com.sun.max.vm.compiler.cir.*;

/**
 * A styled document derived from an annotated CIR trace.
 * 
 * @author Doug Simon
 * @author Sumeet Panchal
 */
class CirStyledDocument extends DefaultStyledDocument {

    public int _collapsedOffset;
    public CirAnnotatedTrace.ParenthesisElement _collapsedDual;
    public CirAnnotatedTrace _cirAnnotatedTrace;
    public IntHashMap<CirAnnotatedTrace.Element> _offsetToElement = new IntHashMap<CirAnnotatedTrace.Element>();
    public Bag<CirNode, CirAnnotatedTrace.Element, Sequence<CirAnnotatedTrace.Element>> _elementsPerNode = new SequenceBag<CirNode, CirAnnotatedTrace.Element>(IDENTITY);

    public CirStyledDocument(CirAnnotatedTrace cirAnnotatedTrace) {
        _collapsedOffset = -1;
        _collapsedDual = null;
        _cirAnnotatedTrace = cirAnnotatedTrace;
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
                _elementsPerNode.add(node, element);
            }
            element.visitRanges(new RangeVisitor() {
                public void visitRange(Range range) {
                    for (int i = range.start(); i != range.end(); ++i) {
                        _offsetToElement.put(i, element);
                    }
                }
            });
        }
    }

    public CirAnnotatedTrace.Element elementAt(int offset) {
        return _offsetToElement.get(offset);
    }

    public Sequence<CirAnnotatedTrace.Element> occurrences(CirAnnotatedTrace.Element element) {
        if (element.node() == null) {
            return new ArraySequence<CirAnnotatedTrace.Element>(element);
        }
        final Sequence<CirAnnotatedTrace.Element> occurrences = _elementsPerNode.get(element.node());
        return occurrences;
    }
}

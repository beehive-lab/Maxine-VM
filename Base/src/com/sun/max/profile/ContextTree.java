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
package com.sun.max.profile;

import com.sun.max.util.timer.*;

/**
 * This class represents a context in which profiling is performed. Typically,
 * a profiling context is thread-specific, meaning that it collects metrics such as
 * execution times on a per-thread basis.
 *
 * @author Ben L. Titzer
 */
public class ContextTree {

    protected static class Node {
        protected final long id;
        protected Node sibling;
        protected Node child;
        protected Timer timer;

        public Node(long id) {
            this.id = id;
        }

        public Node findChild(long searchId) {
            Node pos = child;
            while (pos != null) {
                if (pos.id == this.id) {
                    return pos;
                }
                pos = pos.sibling;
            }
            return null;
        }

        public Node addChild(long searchId, Clock clock) {
            Node foundChild = findChild(searchId);
            if (foundChild == null) {
                foundChild = new Node(searchId);
                foundChild.timer = new SingleUseTimer(clock);
                foundChild.sibling = this.child;
                this.child = foundChild;
            }
            return foundChild;
        }
    }

    public static final int MAXIMUM_DEPTH = 1024;

    protected final Clock clock;
    protected final Node[] stack;
    protected int depth;

    public ContextTree(Clock clock) {
        this.clock = clock;
        this.stack = new Node[MAXIMUM_DEPTH];
        depth = 0;
        stack[0] = new Node(Long.MAX_VALUE);
    }

    public void enter(long id) {
        final Node top = stack[depth];
        final Node child = top.addChild(id, clock);
        // push a new profiling node onto the stack
        stack[++depth] = child;
        child.timer.start();
    }

    public void exit(long id) {
        while (depth > 0) {
            // pop all profiling nodes until we find the correct ID.
            final Node top = stack[depth--];
            top.timer.stop();
            if (top.id == id) {
                break;
            }
        }
    }
}

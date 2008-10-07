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

/**
 * This class represents a context in which profiling is performed. Typically,
 * a profiling context is thread-specific, meaning that it collects metrics such as
 * execution times on a per-thread basis.
 *
 * @author Ben L. Titzer
 */
public class ContextTree {

    protected static class Node {
        protected final long _id;
        protected Node _sibling;
        protected Node _child;
        protected Metrics.Timer _timer;

        public Node(long id) {
            this._id = id;
        }

        public Node findChild(long id) {
            Node pos = _child;
            while (pos != null) {
                if (pos._id == id) {
                    return pos;
                }
                pos = pos._sibling;
            }
            return null;
        }

        public Node addChild(long id, Clock clock) {
            Node child = findChild(id);
            if (child == null) {
                child = new Node(id);
                child._timer = new Metrics.Timer(clock);
                child._sibling = _child;
                _child = child;
            }
            return child;
        }
    }

    public static final int MAXIMUM_DEPTH = 1024;

    protected final Clock _clock;
    protected final Node[] _stack;
    protected int _depth;

    public ContextTree(Clock clock) {
        this._clock = clock;
        this._stack = new Node[MAXIMUM_DEPTH];
        _depth = 0;
        _stack[0] = new Node(Long.MAX_VALUE);
    }

    public void enter(long id) {
        final Node top = _stack[_depth];
        final Node child = top.addChild(id, _clock);
        // push a new profiling node onto the stack
        _stack[++_depth] = child;
        child._timer.start();
    }

    public void exit(long id) {
        while (_depth > 0) {
            // pop all profiling nodes until we find the correct ID.
            final Node top = _stack[_depth--];
            top._timer.stop();
            if (top._id == id) {
                break;
            }
        }
    }
}

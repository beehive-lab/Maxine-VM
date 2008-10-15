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
package com.sun.max.jdwp.handlers;

import java.util.regex.*;

import com.sun.max.collect.*;
import com.sun.max.jdwp.data.*;
import com.sun.max.jdwp.protocol.*;
import com.sun.max.jdwp.protocol.EventRequestCommands.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * @author Thomas Wuerthinger
 */
public interface JDWPEventModifier {

    boolean isAccepted(JDWPEventContext context);

    public static class Static {

        public static Sequence<JDWPEventModifier> createSequence(JDWPSession session, EventRequestCommands.Set.Modifier[] modifiers) throws JDWPException {

            final AppendableSequence<JDWPEventModifier> result = new LinkSequence<JDWPEventModifier>();
            for (Set.Modifier m : modifiers) {
                final Set.Modifier.ModifierCommon mc = m._aModifierCommon;
                if (mc instanceof Set.Modifier.ClassExclude) {
                    result.append(new JDWPEventModifier.ClassExclude(((Set.Modifier.ClassExclude) mc)._classPattern));
                } else if (mc instanceof Set.Modifier.ClassMatch) {
                    result.append(new JDWPEventModifier.ClassMatch(((Set.Modifier.ClassMatch) mc)._classPattern));
                } else if (mc instanceof Set.Modifier.ClassOnly) {
                    result.append(new JDWPEventModifier.ClassOnly(session.getReferenceType(((Set.Modifier.ClassOnly) mc)._clazz)));
                } else if (mc instanceof Set.Modifier.Conditional) {
                    throw new JDWPNotImplementedException();
                } else if (mc instanceof Set.Modifier.Count) {
                    result.append(new JDWPEventModifier.Count(((Set.Modifier.Count) mc)._count));
                } else if (mc instanceof Set.Modifier.ExceptionOnly) {
                    final Set.Modifier.ExceptionOnly emc = (Set.Modifier.ExceptionOnly) mc;
                    result.append(new JDWPEventModifier.ExceptionOnly(session.getReferenceType(emc._exceptionOrNull), emc._caught, emc._uncaught));
                } else if (mc instanceof Set.Modifier.FieldOnly) {
                    throw new JDWPNotImplementedException();
                } else if (mc instanceof Set.Modifier.InstanceOnly) {
                    throw new JDWPNotImplementedException();
                } else if (mc instanceof Set.Modifier.LocationOnly) {
                    result.append(new JDWPEventModifier.LocationOnly(((Set.Modifier.LocationOnly) mc)._loc));
                } else if (mc instanceof Set.Modifier.SourceNameMatch) {
                    throw new JDWPNotImplementedException();
                } else if (mc instanceof Set.Modifier.Step) {
                    final Set.Modifier.Step stepModifier = (Set.Modifier.Step) mc;
                    result.append(new JDWPEventModifier.Step(session.getThread(stepModifier._thread), stepModifier._size, stepModifier._depth));
                } else if (mc instanceof Set.Modifier.ThreadOnly) {
                    result.append(new JDWPEventModifier.ThreadOnly(session.getThread(((Set.Modifier.ThreadOnly) mc)._thread)));
                } else {
                    throw new JDWPNotImplementedException();
                }
            }
            return result;
        }
    }

    public static class Count implements JDWPEventModifier {

        private int _count;

        public Count(int count) {
            _count = count;
        }

        @Override
        public boolean isAccepted(JDWPEventContext context) {
            return --_count == 0;
        }
    }

    public static class Step extends ThreadOnly {
        private int _size;
        private int _depth;

        public Step(ThreadProvider thread, int size, int depth) {
            super(thread);
            _size = size;
            _depth = depth;
        }

        public int size() {
            return _size;
        }

        public int depth() {
            return _depth;
        }

    }

    public static class ThreadOnly implements JDWPEventModifier {

        private ThreadProvider _thread;

        public ThreadOnly(ThreadProvider thread) {
            _thread = thread;
        }

        @Override
        public boolean isAccepted(JDWPEventContext context) {
            return context.getThread() == null || context.getThread().equals(_thread);
        }

        public ThreadProvider thread() {
            return _thread;
        }
    }

    public static class ClassOnly implements JDWPEventModifier {

        private ReferenceTypeProvider _klass;

        public ClassOnly(ReferenceTypeProvider klass) {
            _klass = klass;
        }

        @Override
        public boolean isAccepted(JDWPEventContext context) {
            return context.getReferenceType() == null || context.getReferenceType().equals(_klass);
        }
    }

    public static class ClassMatch implements JDWPEventModifier {

        private String _regexp;

        public ClassMatch(String regexp) {
            _regexp = regexp;
        }

        @Override
        public boolean isAccepted(JDWPEventContext context) {
            if (context.getReferenceType() == null) {
                return true;
            }
            final Pattern pattern = Pattern.compile(_regexp);
            final String value = context.getReferenceType().getName();
            final Matcher matcher = pattern.matcher(value);
            return matcher.matches();
        }
    }

    public static class ClassExclude implements JDWPEventModifier {

        private String _regexp;

        public ClassExclude(String regexp) {
            _regexp = regexp;
        }

        @Override
        public boolean isAccepted(JDWPEventContext context) {
            if (context.getReferenceType() == null) {
                return true;
            }
            final Pattern pattern = Pattern.compile(_regexp);
            final String value = context.getReferenceType().getName();
            final Matcher matcher = pattern.matcher(value);
            return !matcher.matches();
        }
    }

    public static class LocationOnly implements JDWPEventModifier {

        private JDWPLocation _location;

        public LocationOnly(JDWPLocation location) {
            _location = location;
        }

        public JDWPLocation location() {
            return _location;
        }

        @Override
        public boolean isAccepted(JDWPEventContext context) {
            return context.getLocation() == null || context.getLocation().equals(_location);
        }
    }

    public static class ExceptionOnly implements JDWPEventModifier {

        public ExceptionOnly(ReferenceTypeProvider exceptionType, boolean caught, boolean uncaught) {
        }

        @Override
        public boolean isAccepted(JDWPEventContext context) {
            // TODO: Implement correctly!
            return false;
        }
    }
}

/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.jdwp.handlers;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.max.jdwp.data.JDWPException;
import com.sun.max.jdwp.data.JDWPLocation;
import com.sun.max.jdwp.data.JDWPNotImplementedException;
import com.sun.max.jdwp.protocol.EventRequestCommands;
import com.sun.max.jdwp.protocol.EventRequestCommands.Set;
import com.sun.max.jdwp.vm.proxy.ReferenceTypeProvider;
import com.sun.max.jdwp.vm.proxy.ThreadProvider;

/**
 * @author Thomas Wuerthinger
 */
public interface JDWPEventModifier {

    boolean isAccepted(JDWPEventContext context);

    public static class Static {

        public static List<JDWPEventModifier> createList(JDWPSession session, EventRequestCommands.Set.Modifier[] modifiers) throws JDWPException {

            final List<JDWPEventModifier> result = new LinkedList<JDWPEventModifier>();
            for (Set.Modifier m : modifiers) {
                final Set.Modifier.ModifierCommon mc = m.aModifierCommon;
                if (mc instanceof Set.Modifier.ClassExclude) {
                    result.add(new JDWPEventModifier.ClassExclude(((Set.Modifier.ClassExclude) mc).classPattern));
                } else if (mc instanceof Set.Modifier.ClassMatch) {
                    result.add(new JDWPEventModifier.ClassMatch(((Set.Modifier.ClassMatch) mc).classPattern));
                } else if (mc instanceof Set.Modifier.ClassOnly) {
                    result.add(new JDWPEventModifier.ClassOnly(session.getReferenceType(((Set.Modifier.ClassOnly) mc).clazz)));
                } else if (mc instanceof Set.Modifier.Conditional) {
                    throw new JDWPNotImplementedException();
                } else if (mc instanceof Set.Modifier.Count) {
                    result.add(new JDWPEventModifier.Count(((Set.Modifier.Count) mc).count));
                } else if (mc instanceof Set.Modifier.ExceptionOnly) {
                    final Set.Modifier.ExceptionOnly emc = (Set.Modifier.ExceptionOnly) mc;
                    result.add(new JDWPEventModifier.ExceptionOnly(session.getReferenceType(emc.exceptionOrNull), emc.caught, emc.uncaught));
                } else if (mc instanceof Set.Modifier.FieldOnly) {
                    throw new JDWPNotImplementedException();
                } else if (mc instanceof Set.Modifier.InstanceOnly) {
                    throw new JDWPNotImplementedException();
                } else if (mc instanceof Set.Modifier.LocationOnly) {
                    result.add(new JDWPEventModifier.LocationOnly(((Set.Modifier.LocationOnly) mc).loc));
                } else if (mc instanceof Set.Modifier.SourceNameMatch) {
                    throw new JDWPNotImplementedException();
                } else if (mc instanceof Set.Modifier.Step) {
                    final Set.Modifier.Step stepModifier = (Set.Modifier.Step) mc;
                    result.add(new JDWPEventModifier.Step(session.getThread(stepModifier.thread), stepModifier.size, stepModifier.depth));
                } else if (mc instanceof Set.Modifier.ThreadOnly) {
                    result.add(new JDWPEventModifier.ThreadOnly(session.getThread(((Set.Modifier.ThreadOnly) mc).thread)));
                } else {
                    throw new JDWPNotImplementedException();
                }
            }
            return result;
        }
    }

    public static class Count implements JDWPEventModifier {

        private int count;

        public Count(int count) {
            this.count = count;
        }

        public boolean isAccepted(JDWPEventContext context) {
            return --count == 0;
        }
    }

    public static class Step extends ThreadOnly {
        private int size;
        private int depth;

        public Step(ThreadProvider thread, int size, int depth) {
            super(thread);
            this.size = size;
            this.depth = depth;
        }

        public int size() {
            return size;
        }

        public int depth() {
            return depth;
        }

    }

    public static class ThreadOnly implements JDWPEventModifier {

        private ThreadProvider thread;

        public ThreadOnly(ThreadProvider thread) {
            this.thread = thread;
        }

        public boolean isAccepted(JDWPEventContext context) {
            return context.getThread() == null || context.getThread().equals(thread);
        }

        public ThreadProvider thread() {
            return thread;
        }
    }

    public static class ClassOnly implements JDWPEventModifier {

        private ReferenceTypeProvider klass;

        public ClassOnly(ReferenceTypeProvider klass) {
            this.klass = klass;
        }

        public boolean isAccepted(JDWPEventContext context) {
            return context.getReferenceType() == null || context.getReferenceType().equals(klass);
        }
    }

    public static class ClassMatch implements JDWPEventModifier {

        private String regexp;

        public ClassMatch(String regexp) {
            this.regexp = regexp;
        }

        public boolean isAccepted(JDWPEventContext context) {
            if (context.getReferenceType() == null) {
                return true;
            }
            final Pattern pattern = Pattern.compile(regexp);
            final String value = context.getReferenceType().getName();
            final Matcher matcher = pattern.matcher(value);
            return matcher.matches();
        }
    }

    public static class ClassExclude implements JDWPEventModifier {

        private String regexp;

        public ClassExclude(String regexp) {
            this.regexp = regexp;
        }

        public boolean isAccepted(JDWPEventContext context) {
            if (context.getReferenceType() == null) {
                return true;
            }
            final Pattern pattern = Pattern.compile(regexp);
            final String value = context.getReferenceType().getName();
            final Matcher matcher = pattern.matcher(value);
            return !matcher.matches();
        }
    }

    public static class LocationOnly implements JDWPEventModifier {

        private JDWPLocation location;

        public LocationOnly(JDWPLocation location) {
            this.location = location;
        }

        public JDWPLocation location() {
            return location;
        }

        public boolean isAccepted(JDWPEventContext context) {
            return context.getLocation() == null || context.getLocation().equals(location);
        }
    }

    public static class ExceptionOnly implements JDWPEventModifier {

        public ExceptionOnly(ReferenceTypeProvider exceptionType, boolean caught, boolean uncaught) {
        }

        public boolean isAccepted(JDWPEventContext context) {
            // TODO: Implement correctly!
            return false;
        }
    }
}

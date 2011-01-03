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
package com.sun.max.vm.cps.jit;

import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;

/**
 * A stop denotes a target code position for which the location of object references are precisely known.
 * This class and its subclasses are used to aggregate the stops for a method being translated by the
 * {@linkplain BytecodeToTargetTranslator template based JIT compiler}. Most of the stops in a JIT compiled
 * method are copied from {@linkplain TemplateStop templates}.
 *
 * @author Doug Simon
 */
public abstract class Stop {

    /**
     * The position of the stop in the target code of the method.
     */
    final int position;

    /**
     * The bytecode position correlated with the stop.
     */
    final int bytecodePosition;

    public Stop(int position, int bytecodePosition) {
        this.position = position;
        this.bytecodePosition = bytecodePosition;
    }

    /**
     * Determines if this stop denotes a direct call into the VM runtime (as opposed to an application method). This
     * method must not be called if this is not a stop for a direct call (i.e.
     * {@code this.type() != StopType.DIRECT_CALL}.
     */
    public boolean isDirectRuntimeCall() {
        throw ProgramError.unexpected();
    }

    /**
     * Gets the type of this stop.
     */
    public abstract StopType type();

    /**
     * Initializes the reference map denoting which frame slots contain object references at this stop.
     *
     * @param map the frame reference map for this stop
     */
    public abstract void initializeStackReferenceMap(ByteArrayBitMap map, int firstTemplateSlot);

    /**
     * Initializes the reference map denoting which registers contain object references at this safepoint stop. This
     * must not be called if this is not a safepoint stop (i.e. {@code this.type() != StopType.SAFEPOINT}.
     *
     * @param map the register reference map for this safepoint stop
     */
    public void initializeRegisterReferenceMap(ByteArrayBitMap map) {
        throw ProgramError.unexpected();
    }

    /**
     * Gets the method directly called at this stop. This method must not be called is this is not a stop for a direct
     * call (i.e. {@code this.type() != StopType.DIRECT_CALL}.
     */
    public ClassMethodActor directCallee() {
        throw ProgramError.unexpected();
    }

    /**
     * Represents a stop copied from a template.
     */
    public abstract static class TemplateStop extends Stop {
        /**
         * The template containing this derived stop.
         */
        final CPSTargetMethod template;

        /**
         * The stop-type specific index of this stop.
         */
        final int index;

        public TemplateStop(int position, int bytecodePosition, TargetMethod template, int index) {
            super(position, bytecodePosition);
            this.template = (CPSTargetMethod) template;
            this.index = index;
        }

        @Override
        public void initializeStackReferenceMap(ByteArrayBitMap map, int firstTemplateSlot) {
            final ByteArrayBitMap templateFrameReferenceMap = template.frameReferenceMapFor(type(), index);
            if (templateFrameReferenceMap != null) {
                for (int i = templateFrameReferenceMap.nextSetBit(0); i >= 0; i = templateFrameReferenceMap.nextSetBit(i + 1)) {
                    map.set(firstTemplateSlot + i);
                }
            }
        }
    }

    /**
     * Represents an {@linkplain StopType#INDIRECT_CALL indirect call} stop copied from a template.
     */
    public static class TemplateIndirectCall extends TemplateStop {
        public TemplateIndirectCall(int position, int bytecodePosition, TargetMethod template, int indirectCallIndex) {
            super(position, bytecodePosition, template, indirectCallIndex);
        }

        @Override
        public StopType type() {
            return StopType.INDIRECT_CALL;
        }
    }

    /**
     * Represents a {@linkplain StopType#DIRECT_CALL direct call} stop copied from a template.
     */
    public static class TemplateDirectCall extends TemplateStop {
        public TemplateDirectCall(int position, int bytecodePosition, TargetMethod template, int directCallIndex) {
            super(position, bytecodePosition, template, directCallIndex);
        }

        @Override
        public StopType type() {
            return StopType.DIRECT_CALL;
        }

        @Override
        public ClassMethodActor directCallee() {
            return (ClassMethodActor) template.directCallees()[index];
        }

        @Override
        public boolean isDirectRuntimeCall() {
            return true;
        }
    }

    /**
     * Represents a {@linkplain StopType#SAFEPOINT safepoint} stop copied from a template.
     */
    public static class TemplateSafepoint extends TemplateStop {
        public TemplateSafepoint(int position, int bytecodePosition, TargetMethod template, int safepointIndex) {
            super(position, bytecodePosition, template, safepointIndex);
        }

        @Override
        public StopType type() {
            return StopType.SAFEPOINT;
        }

        @Override
        public void initializeRegisterReferenceMap(ByteArrayBitMap map) {
            final ByteArrayBitMap templateRegisterReferenceMap = template.registerReferenceMapFor(index);
            for (int i = templateRegisterReferenceMap.nextSetBit(0); i >= 0; i = templateRegisterReferenceMap.nextSetBit(i + 1)) {
                map.set(i);
            }
        }
    }

    /**
     * Represents a {@linkplain StopType#DIRECT_CALL direct call} stop inserted when compiling an {@code INVOKE...} bytecode.
     */
    public static class BytecodeDirectCall extends Stop {
        final ClassMethodActor callee;
        public BytecodeDirectCall(int position, int bytecodePosition, ClassMethodActor callee) {
            super(position, bytecodePosition);
            this.callee = callee;
        }

        @Override
        public StopType type() {
            return StopType.DIRECT_CALL;
        }

        @Override
        public ClassMethodActor directCallee() {
            return callee;
        }

        @Override
        public void initializeStackReferenceMap(ByteArrayBitMap map, int firstTemplateSlot) {
            // Do nothing: the template slots are dead for the remainder of the template
        }

        @Override
        public boolean isDirectRuntimeCall() {
            return false;
        }
    }

    /**
     * Represents a {@linkplain StopType#SAFEPOINT safepoint} stop inserted for a backward branch in the bytecode.
     */
    public static class BackwardBranchBytecodeSafepoint extends Stop {
        public BackwardBranchBytecodeSafepoint(int position, int bytecodePosition) {
            super(position, bytecodePosition);
        }

        @Override
        public StopType type() {
            return StopType.SAFEPOINT;
        }

        @Override
        public void initializeStackReferenceMap(ByteArrayBitMap map, int firstTemplateSlot) {
            // Do nothing: the template slots are dead at a backward branch
        }

        @Override
        public void initializeRegisterReferenceMap(ByteArrayBitMap map) {
            // Do nothing: there are no references in the registers at a backward branch
        }
    }
}

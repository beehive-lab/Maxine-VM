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
package com.sun.max.vm.cps.hotpath.state;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.hotpath.*;
import com.sun.max.vm.cps.hotpath.compiler.*;
import com.sun.max.vm.type.*;

/**
 * A generic abstract Java stack used for bytecode interpretation and recording. This code is
 * similar to the {@link com.sun.max.vm.cps.b.c.JavaStack} class, but more general.
 *
 * @author Michael Bebenita
 * Checkstyle: stop
 */
public abstract class State<Element_Type extends Classifiable> {

    protected final ArrayList<Element_Type> slots;
    protected final ArrayList<Frame> frames;

    /**
     * Creates an empty state.
     */
    protected State() {
        slots = new ArrayList<Element_Type>();
        frames = new ArrayList<Frame>();
    }

    /**
     * Copies an existing state.
     */
    protected State(State<Element_Type> state) {
        slots = new ArrayList<Element_Type>(state.slots);
        frames = new ArrayList<Frame>();
        for (Frame frame : state.frames) {
            frames.add(new Frame(frame));
        }
    }

    /**
     * Creates a state by copying a sequence of frames from an existing state. Copied frames are normalized.
     */
    protected State(State<Element_Type> state, int frameIndex, int frameCount) {
        assert frameIndex >= 0 && frameIndex + frameCount <= state.frames.size();

        final int slotIndex = state.frames.get(frameIndex).lp;
        final int slotOffset = -slotIndex;
        final int slotCount = state.frames.get(frameIndex + frameCount - 1).sp;

        slots = new ArrayList<Element_Type>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            slots.add(state.slots.get(slotIndex + i));
        }

        frames = new ArrayList<Frame>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            frames.add(new Frame(state.frames.get(frameIndex + i), slotOffset));
        }
    }

    public List<Frame> frames() {
        return frames;
    }

    public Frame last() {
        return frames.get(frames.size() - 1);
    }

    public Frame first() {
        return frames.get(0);
    }

    /**
     * Gets the stack element at the specified index.
     */
    public Element_Type getOne(int index) {
        if (index < slots.size()) {
            return slots.get(index);
        } else if (index == slots.size()) {
            slots.add(undefined());
            return undefined();
        } else {
            ProgramError.unexpected();
            return null;
        }
    }

    /**
     * Sets a stack element at a specified index.
     */
    public Element_Type setOne(int index, Element_Type element) {
        if (index < slots.size()) {
            return slots.set(index, element);
        } else if (index == slots.size()) {
            slots.add(element);
        } else {
            ProgramError.unexpected();
        }
        return null;
    }

    /**
     * Pops a sequence of stack elements of a given kind.
     */
    public Element_Type [] popMany(Kind... kinds) {
        final Element_Type [] arguments = createArray(kinds.length);
        for (int i = kinds.length - 1; i >= 0; i--) {
            arguments[i] = pop(kinds[i]);
        }
        return arguments;
    }

    /**
     * Peeks a sequence of stack elements of a given kind and at a specified stack depth.
     */
    public Element_Type [] peekMany(int stackDepth, Kind... kinds) {
        int depth = stackDepth;
        final Element_Type [] arguments = createArray(kinds.length);
        for (int i = kinds.length - 1; i >= 0; i--) {
            arguments[i] = peek(kinds[i], depth);
            depth += kinds[i].stackSlots;
        }
        return arguments;
    }

    public Element_Type [] getLocalSlots() {
        final int numberOfLocalSlots = last().method().codeAttribute().maxLocals;
        return getSlots(0, numberOfLocalSlots);
    }

    public Element_Type [] getPrameterLocalSlots() {
        final int numberOfParameterLocalSlots = last().stackHeight() - last().method().numberOfParameterSlots();
        return getSlots(0, numberOfParameterLocalSlots);
    }

    public Element_Type [] getStackSlots() {
        final int numberOfLocalSlots = last().method().codeAttribute().maxLocals;
        final int numberOfStackSlots = last().stackHeight() - numberOfLocalSlots;
        return getSlots(numberOfLocalSlots, numberOfStackSlots);
    }

    public final Element_Type getSlot(int slot) {
        return getOne(last().lp + slot);
    }

    public Element_Type [] getCompressedSlots(int slot, int count) {
        final Element_Type [] slotArrays = createArray(count);
        int localIndex = 0;
        for (int i = 0; i < count; i++) {
            final Element_Type local = getSlot(slot + i);
            slotArrays[localIndex++] = local;
            if (!local.kind().isCategory1) {
                i++;
            }
        }
        return java.util.Arrays.copyOfRange(slotArrays, 0, localIndex);
    }

    public Element_Type [] getSlots(int slot, int count) {
        final Element_Type [] slotArray = createArray(count);
        for (int i = 0; i < count; i++) {
            slotArray[i] = getSlot(slot + i);
        }
        return slotArray;
    }

    public final void pushOne(Element_Type element) {
        setOne(last().sp++, element);
    }

    public final Element_Type popOne() {
        return setOne(--last().sp, undefined());
    }

    public final void push(Element_Type element) {
        pushOne(element);
        if (!element.kind().isCategory1) {
            pushOne(filler());
        }
    }

    public final Element_Type peek(Kind kind) {
        return getOne(length() - kind.stackSlots);
    }

    public final Element_Type peek(Kind kind, int stackDepth) {
        return getOne(length() - kind.stackSlots - stackDepth);
    }

    public final Element_Type pop(Kind kind) {
        final Element_Type element = pop();
        // Treat boolean, byte, short, char and int the same.
        assert element.kind().stackKind == kind.stackKind;
        return element;
    }

    public final Element_Type pop() {
        Element_Type element = popOne();
        if (element == filler()) {
            element = popOne();
            assert !element.kind().isCategory1;
        }
        return element;
    }

    public final void load(Kind kind, int slot) {
        final Element_Type element = getSlot(slot);
        setKind(element, kind);
        push(element);
    }

    protected abstract void setKind(Element_Type element, Kind kind);
    protected abstract Element_Type [] createArray(int length);
    protected abstract Element_Type filler();
    protected abstract Element_Type undefined();

    public final void store(Kind kind, int slot) {
        final Element_Type newElement = pop(kind);
        final int index = last().lp + slot;
        set(index, newElement);
    }

    public final void store(int slot, final Element_Type newElement) {
        final int index = last().lp + slot;
        set(index, newElement);
    }

    public final void set(int index, final Element_Type newElement) {
        final Element_Type oldElement = getOne(index);
        if (oldElement == filler()) {
            setOne(index - 1, undefined());
        }
        setOne(index, newElement);
        if (!newElement.kind().isCategory1) {
            setOne(index + 1, filler());
        }
        int nextIndex = index + 1;
        if (!newElement.kind().isCategory1) {
            nextIndex++;
        }
        if (getOne(nextIndex) == filler()) {
            setOne(nextIndex, undefined());
        }
    }

    public final void enter(ClassMethodActor method, int returnPc) {
        final Frame frame = new Frame(method, 0, 0);
        last().sp -= method.numberOfParameterSlots();
        last().pc = returnPc;
        frame.lp = length();
        frame.sp = frame.lp + method.codeAttribute().maxLocals;
        for (int i = method.numberOfParameterSlots(); i < method.codeAttribute().maxLocals; i++) {
            setOne(frame.lp + i, undefined());
        }
        frames.add(frame);
    }

    public final void leave() {
        final Kind resultKind = last().method.resultKind();
        Element_Type result = null;
        if (resultKind != Kind.VOID) {
            result = pop();
        }
        frames.remove(frames.size() - 1);
        if (resultKind != Kind.VOID) {
            push(result);
        }
    }

    public final void leaveWithoutReturn() {
        frames.remove(frames.size() - 1);
    }

    public final void execute(int opcode) {

        if (opcode == Bytecodes.POP) {
            popOne();
            return;
        }

        if (opcode == Bytecodes.POP2) {
            popOne();
            popOne();
            return;
        }

        final Element_Type element1 = popOne();

        if (opcode == Bytecodes.DUP) {
            pushOne(element1);
            pushOne(element1);
            return;
        }

        final Element_Type element2 = popOne();

        if (opcode == Bytecodes.DUP2) {
            pushOne(element2);
            pushOne(element1);
            pushOne(element2);
            pushOne(element1);
            return;
        }

        if (opcode == Bytecodes.SWAP) {
            pushOne(element1);
            pushOne(element2);
            return;
        }

        if (opcode == Bytecodes.DUP_X1) {
            pushOne(element1);
            pushOne(element2);
            pushOne(element1);
            return;
        }

        final Element_Type element3 = popOne();

        if (opcode == Bytecodes.DUP_X2) {
            pushOne(element1);
            pushOne(element3);
            pushOne(element2);
            pushOne(element1);
            return;
        }

        if (opcode == Bytecodes.DUP2_X1) {
            pushOne(element2);
            pushOne(element1);
            pushOne(element3);
            pushOne(element2);
            pushOne(element1);
            return;
        }

        final Element_Type element4 = popOne();

        if (opcode == Bytecodes.DUP2_X2) {
            pushOne(element2);
            pushOne(element1);
            pushOne(element4);
            pushOne(element3);
            pushOne(element2);
            pushOne(element1);
            return;
        }

        ProgramError.unexpected("Bytecodes " + opcode);
    }

    public final <Other_Element_Type extends Classifiable> void compare(State<Other_Element_Type> other, StatePairVisitor<Element_Type, Other_Element_Type> visitor) {
        for (int i = 0; i < length(); i++) {
            final Element_Type elment = getOne(i);
            final Other_Element_Type otherElement = i < other.length() ? other.getOne(i) : other.undefined();
            visitor.setIndex(i);
            visitor.visit(elment, otherElement);
            if (!elment.kind().isCategory1) {
                i++;
            }
        }
    }

    public final void visit(Predicate<Element_Type> predicate, StateVisitor<Element_Type> visitor) {
        for (int i = 0; i < length(); i++) {
            final Element_Type elment = getOne(i);
            if (predicate.evaluate(elment)) {
                visitor.setIndex(i);
                visitor.visit(elment);
            }
            if (!elment.kind().isCategory1) {
                i++;
            }
        }
    }

    public void visit(StateVisitor<Element_Type> visitor) {
        for (int i = 0; i < length(); i++) {
            final Element_Type elment = getOne(i);
            visitor.setIndex(i);
            visitor.visit(elment);
            if (!elment.kind().isCategory1) {
                i++;
            }
        }
    }

    public int length() {
        return last().sp;
    }

    public void append(List<Frame> frameList) {
        assert this.frames.get(this.frames.size() - 1).method() == Utils.first(frameList).method();
        final Frame active = this.frames.remove(this.frames.size() - 1);
        for (Frame frame : frameList) {
            this.frames.add(new Frame(frame, active.lp()));
        }
    }

    public <Other_Element_Type extends Classifiable> boolean matchesSliceOf(State<Other_Element_Type> other) {
        if (frames.size() > other.frames.size()) {
            return false;
        }
        for (int i = 1; i <= frames.size(); i++) {
            if (frames.get(frames.size() - i).matches(other.frames.get(other.frames.size() - i)) == false) {
                return false;
            }
        }
        return true;
    }

    public <Other_Element_Type extends Classifiable> boolean matches(State<Other_Element_Type> other) {
        if (frames.size() != other.frames.size()) {
            return false;
        }
        for (int i = frames.size() - 1; i >= 0; i--) {
            if (frames.get(i).matches(other.frames.get(i)) == false) {
                return false;
            }
        }
        return true;
    }

    protected MapFunction<Element_Type, String> defaultMap() {
        return new MapFunction<Element_Type, String>() {
            public String map(Element_Type from) {
                return from.toString();
            }
        };
    }

    public final void println() {
        println(defaultMap());
    }

    public final void printDetailed() {
        printDetailed(defaultMap());
    }

    public final void printDetailed(MapFunction<Element_Type, String> labelMap) {
        for (int i = 0; i < frames.size(); i++) {
            final Frame frame = frames.get(i);
            final ClassMethodActor method = frame.method;
            int locals = 0;
            String methodName = "null";
            if (method != null) {
                methodName = method.name.toString();
                locals = method.numberOfParameterSlots();
            }
            Console.printf("method: %-10s lp:%2d, sp:%2d, pc:%3d, stack: ", methodName, frame.lp, frame.sp, frame.pc);
            for (int j = frame.lp; j < frame.sp; j++) {
                if (locals > 0 && j == frame.lp + locals) {
                    Console.print("| ");
                }
                Console.print(labelMap.map(getOne(j)) + " ");
            }
            Console.println();
        }
    }

    public final void println(MapFunction<Element_Type, String> labelMap) {
        print(labelMap);
        Console.println();
    }

    public final void print(MapFunction<Element_Type, String> labelMap) {
        String framesString = "";
        String valuesString = "";

        for (int i = 0; i < this.frames.size(); i++) {
            final Frame frame = this.frames.get(i);
            final ClassMethodActor method = frame.method;
            String methodName = "null";
            if (method != null) {
                methodName = method.name.toString();
            }
            framesString += methodName;

            if (frame.stackHeight() == 0) {
                valuesString += "empty";
            } else {
                for (int j = frame.lp; j < frame.sp; j++) {
                    valuesString += labelMap.map(getOne(j));
                    if (j < frame.sp - 1) {
                        valuesString += " ";
                    }
                }
            }

            if (i < this.frames.size() - 1) {
                framesString += " | ";
                valuesString += " | ";
            } else {
                framesString = "[" + framesString + "] sp:" + frame.sp + " sh:" + frame.stackHeight() + ", pc:" + frame.pc;
            }
        }

        Console.print(framesString + " [" + valuesString + "]");
    }
}

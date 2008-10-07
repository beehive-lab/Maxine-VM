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
/*VCSID=9cc4b0bc-1220-426a-9dce-39f25149e4e6*/
package com.sun.max.vm.hotpath.state;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.util.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.hotpath.compiler.*;
import com.sun.max.vm.type.*;

/**
 * A generic abstract Java stack used for bytecode interpretation and recording. This code is
 * similar to the {@link com.sun.max.vm.compiler.b.c.JavaStack} class, but more general.
 *
 * @author Michael Bebenita
 * Checkstyle: stop
 */
public abstract class State<Element_Type extends Classifiable> {

    protected final VariableSequence<Element_Type> _slots;
    protected final VariableSequence<Frame> _frames;

    /**
     * Creates an empty state.
     */
    protected State() {
        _slots = new ArrayListSequence<Element_Type>();
        _frames = new ArrayListSequence<Frame>();
    }

    /**
     * Copies an existing state.
     */
    protected State(State<Element_Type> state) {
        _slots = (VariableSequence<Element_Type>) state._slots.clone();
        _frames = new ArrayListSequence<Frame>();
        for (Frame frame : state._frames) {
            _frames.append(new Frame(frame));
        }
    }

    /**
     * Creates a state by copying a sequence of frames from an existing state. Copied frames are normalized.
     */
    protected State(State<Element_Type> state, int frameIndex, int frameCount) {
        assert frameIndex >= 0 && frameIndex + frameCount <= state._frames.length();

        final int slotIndex = state._frames.get(frameIndex)._lp;
        final int slotOffset = -slotIndex;
        final int slotCount = state._frames.get(frameIndex + frameCount - 1)._sp;

        _slots = new ArrayListSequence<Element_Type>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            _slots.append(state._slots.get(slotIndex + i));
        }

        _frames = new ArrayListSequence<Frame>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            _frames.append(new Frame(state._frames.get(frameIndex + i), slotOffset));
        }
    }

    public Sequence<Frame> frames() {
        return _frames;
    }

    public Frame last() {
        return _frames.last();
    }

    public Frame first() {
        return _frames.first();
    }

    /**
     * Gets the stack element at the specified index.
     */
    public Element_Type getOne(int index) {
        if (index < _slots.length()) {
            return _slots.get(index);
        } else if (index == _slots.length()) {
            _slots.append(undefined());
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
        if (index < _slots.length()) {
            return _slots.set(index, element);
        } else if (index == _slots.length()) {
            _slots.append(element);
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
            depth += kinds[i].stackSlots();
        }
        return arguments;
    }

    public Element_Type [] getLocalSlots() {
        final int numberOfLocalSlots = last().method().codeAttribute().maxLocals();
        return getSlots(0, numberOfLocalSlots);
    }

    public Element_Type [] getPrameterLocalSlots() {
        final int numberOfParameterLocalSlots = last().stackHeight() - last().method().numberOfParameterLocals();
        return getSlots(0, numberOfParameterLocalSlots);
    }

    public Element_Type [] getStackSlots() {
        final int numberOfLocalSlots = last().method().codeAttribute().maxLocals();
        final int numberOfStackSlots = last().stackHeight() - numberOfLocalSlots;
        return getSlots(numberOfLocalSlots, numberOfStackSlots);
    }

    public final Element_Type getSlot(int slot) {
        return getOne(last()._lp + slot);
    }

    public Element_Type [] getCompressedSlots(int slot, int count) {
        final Element_Type [] slots = createArray(count);
        int localIndex = 0;
        for (int i = 0; i < count; i++) {
            final Element_Type local = getSlot(slot + i);
            slots[localIndex++] = local;
            if (local.kind().isCategory2()) {
                i++;
            }
        }
        return Arrays.subArray(slots, 0, localIndex);
    }

    public Element_Type [] getSlots(int slot, int count) {
        final Element_Type [] slots = createArray(count);
        for (int i = 0; i < count; i++) {
            slots[i] = getSlot(slot + i);
        }
        return slots;
    }

    public final void pushOne(Element_Type element) {
        setOne(last()._sp++, element);
    }

    public final Element_Type popOne() {
        return setOne(--last()._sp, undefined());
    }

    public final void push(Element_Type element) {
        pushOne(element);
        if (element.kind().isCategory2()) {
            pushOne(filler());
        }
    }

    public final Element_Type peek(Kind kind) {
        return getOne(length() - kind.stackSlots());
    }

    public final Element_Type peek(Kind kind, int stackDepth) {
        return getOne(length() - kind.stackSlots() - stackDepth);
    }

    public final Element_Type pop(Kind kind) {
        final Element_Type element = pop();
        // Treat boolean, byte, short, char and int the same.
        assert element.kind().toStackKind() == kind.toStackKind();
        return element;
    }

    public final Element_Type pop() {
        Element_Type element = popOne();
        if (element == filler()) {
            element = popOne();
            assert element.kind().isCategory2();
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
        final int index = last()._lp + slot;
        set(index, newElement);
    }

    public final void store(int slot, final Element_Type newElement) {
        final int index = last()._lp + slot;
        set(index, newElement);
    }

    public final void set(int index, final Element_Type newElement) {
        final Element_Type oldElement = getOne(index);
        if (oldElement == filler()) {
            setOne(index - 1, undefined());
        }
        setOne(index, newElement);
        if (newElement.kind().isCategory2()) {
            setOne(index + 1, filler());
        }
        int nextIndex = index + 1;
        if (newElement.kind().isCategory2()) {
            nextIndex++;
        }
        if (getOne(nextIndex) == filler()) {
            setOne(nextIndex, undefined());
        }
    }

    public final void enter(ClassMethodActor method, int returnPc) {
        final Frame frame = new Frame(method, 0, 0);
        last()._sp -= method.numberOfParameterLocals();
        last()._pc = returnPc;
        frame._lp = length();
        frame._sp = frame._lp + method.codeAttribute().maxLocals();
        for (int i = method.numberOfParameterLocals(); i < method.codeAttribute().maxLocals(); i++) {
            setOne(frame._lp + i, undefined());
        }
        _frames.append(frame);
    }

    public final void leave() {
        final Kind resultKind = last()._method.resultKind();
        Element_Type result = null;
        if (resultKind != Kind.VOID) {
            result = pop();
        }
        _frames.removeLast();
        if (resultKind != Kind.VOID) {
            push(result);
        }
    }

    public final void leaveWithoutReturn() {
        _frames.removeLast();
    }

    public final void execute(Bytecode bytecode) {

        if (bytecode == Bytecode.POP) {
            popOne();
            return;
        }

        if (bytecode == Bytecode.POP2) {
            popOne();
            popOne();
            return;
        }

        final Element_Type element1 = popOne();

        if (bytecode == Bytecode.DUP) {
            pushOne(element1);
            pushOne(element1);
            return;
        }

        final Element_Type element2 = popOne();

        if (bytecode == Bytecode.DUP2) {
            pushOne(element2);
            pushOne(element1);
            pushOne(element2);
            pushOne(element1);
            return;
        }

        if (bytecode == Bytecode.SWAP) {
            pushOne(element1);
            pushOne(element2);
            return;
        }

        if (bytecode == Bytecode.DUP_X1) {
            pushOne(element1);
            pushOne(element2);
            pushOne(element1);
            return;
        }

        final Element_Type element3 = popOne();

        if (bytecode == Bytecode.DUP_X2) {
            pushOne(element1);
            pushOne(element3);
            pushOne(element2);
            pushOne(element1);
            return;
        }

        if (bytecode == Bytecode.DUP2_X1) {
            pushOne(element2);
            pushOne(element1);
            pushOne(element3);
            pushOne(element2);
            pushOne(element1);
            return;
        }

        final Element_Type element4 = popOne();

        if (bytecode == Bytecode.DUP2_X2) {
            pushOne(element2);
            pushOne(element1);
            pushOne(element4);
            pushOne(element3);
            pushOne(element2);
            pushOne(element1);
            return;
        }

        ProgramError.unexpected("Bytecode " + bytecode);
    }

    public final <Other_Element_Type extends Classifiable> void compare(State<Other_Element_Type> other, StatePairVisitor<Element_Type, Other_Element_Type> visitor) {
        for (int i = 0; i < length(); i++) {
            final Element_Type elment = getOne(i);
            final Other_Element_Type otherElement = i < other.length() ? other.getOne(i) : other.undefined();
            visitor.setIndex(i);
            visitor.visit(elment, otherElement);
            if (elment.kind().isCategory2()) {
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
            if (elment.kind().isCategory2()) {
                i++;
            }
        }
    }

    public void visit(StateVisitor<Element_Type> visitor) {
        for (int i = 0; i < length(); i++) {
            final Element_Type elment = getOne(i);
            visitor.setIndex(i);
            visitor.visit(elment);
            if (elment.kind().isCategory2()) {
                i++;
            }
        }
    }

    public int length() {
        return last()._sp;
    }

    public void append(Sequence<Frame> frames) {
        assert _frames.last().method() == frames.first().method();
        final Frame active = _frames.removeLast();
        for (Frame frame : frames) {
            _frames.append(new Frame(frame, active.lp()));
        }
    }

    public <Other_Element_Type extends Classifiable> boolean matchesSliceOf(State<Other_Element_Type> other) {
        if (_frames.length() > other._frames.length()) {
            return false;
        }
        for (int i = 1; i <= _frames.length(); i++) {
            if (_frames.get(_frames.length() - i).matches(other._frames.get(other._frames.length() - i)) == false) {
                return false;
            }
        }
        return true;
    }

    public <Other_Element_Type extends Classifiable> boolean matches(State<Other_Element_Type> other) {
        if (_frames.length() != other._frames.length()) {
            return false;
        }
        for (int i = _frames.length() - 1; i >= 0; i--) {
            if (_frames.get(i).matches(other._frames.get(i)) == false) {
                return false;
            }
        }
        return true;
    }

    protected MapFunction<Element_Type, String> defaultMap() {
        return new MapFunction<Element_Type, String>() {
            @Override
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
        for (int i = 0; i < _frames.length(); i++) {
            final Frame frame = _frames.get(i);
            final ClassMethodActor method = frame._method;
            int locals = 0;
            String methodName = "null";
            if (method != null) {
                methodName = method.name().toString();
                locals = method.numberOfParameterLocals();
            }
            Console.printf("method: %-10s lp:%2d, sp:%2d, pc:%3d, stack: ", methodName, frame._lp, frame._sp, frame._pc);
            for (int j = frame._lp; j < frame._sp; j++) {
                if (locals > 0 && j == frame._lp + locals) {
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
        String frames = "";
        String values = "";

        for (int i = 0; i < _frames.length(); i++) {
            final Frame frame = _frames.get(i);
            final ClassMethodActor method = frame._method;
            String methodName = "null";
            if (method != null) {
                methodName = method.name().toString();
            }
            frames += methodName;

            if (frame.stackHeight() == 0) {
                values += "empty";
            } else {
                for (int j = frame._lp; j < frame._sp; j++) {
                    values += labelMap.map(getOne(j));
                    if (j < frame._sp - 1) {
                        values += " ";
                    }
                }
            }

            if (i < _frames.length() - 1) {
                frames += " | ";
                values += " | ";
            } else {
                frames = "[" + frames + "] sp:" + frame._sp + " sh:" + frame.stackHeight() + ", pc:" + frame._pc;
            }
        }

        Console.print(frames + " [" + values + "]");
    }
}

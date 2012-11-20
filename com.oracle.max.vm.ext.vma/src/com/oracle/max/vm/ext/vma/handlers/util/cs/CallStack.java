/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.handlers.util.cs;


public abstract class CallStack<T> {
    private T[] stack;
    private int index;

    public CallStack(int length) {
        stack = create(length);
    }

    public CallStack() {
        this(10);
    }

    public int depth() {
        return index;
    }

    public T peek() {
        return index == 0 ? null : stack[index - 1];
    }

    public T peek(int i) {
        if (index - i <= 0) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        return stack[index - i - 1];
    }

    public void push(T t) {
        if (index >= stack.length) {
            T[] newStack = create(stack.length * 2);
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }
        stack[index++] = t;
    }

    public T pop() {
        if (index == 0) {
            return null;
        }
        return stack[--index];
    }

    public void pop(int items) {
        if (index - items < 0) {
            throw new IllegalArgumentException();
        }
        index = index - items;
    }

    protected abstract T[] create(int length);
}

/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.t1x;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;

/**
 * {@HOSTED_ONLY} support for automatically generating template method sources.
 * The focus of the generating code is readability and not conciseness or performance.
 */

@HOSTED_ONLY
public class T1XTemplateGenerator {
    /**
     * Allows customization of the generated methods to support bytecode advising.
     * There are two opportunities for adding advice, {@link AdviceType#BEFORE before}
     * the operation is executed (but after all the arguments are prepared) and
     * {@link AdviceType#AFTER after} the operation has executed.
     * N.B. Currently, {@link AdviceType#AFTER after} advice is only implemented for invoke, as
     * there are technical problems making calls after the stack is modified.
     */
    @HOSTED_ONLY
    public interface AdviceHook {
        /**
         * Called by the generator to allow advice content to be inserted in the template.
         * The advice code may access any state that is accessible in the standard template
         * and also use {@link T1XFrameOps} to access the execution stack.
         *
         * @param tag the tag identifying the template under generation
         * @param adviceType the advice type
         * @param args template-specific arguments that might be useful to the advice writer.
         * Typically it is the type associated with the template tag.
         */
        void generate(T1XTemplateTag tag, AdviceType adviceType, String ... args);
    }

    @HOSTED_ONLY
    public enum AdviceType {BEFORE, AFTER};

    private static AdviceHook adviceHook;

    private static final String[] NULL_ARGS = new String[] {};

    private static final String[] types = {"boolean", "byte", "char", "short", "int", "float", "long", "double", "Reference", "Word", "void"};
    /**
     * As {@link #types} but with first character uppercase.
     */
    private static Map<String, String> uTypes = new HashMap<String, String>();

    /**
     * As {@link #types} but with first character lower case.
     */
    private static Map<String, String> lTypes = new HashMap<String, String>();
    /**
     * As {@link #types} but with {@code Reference} transposed to {@code Object}.
     */
    private static Map<String, String> oTypes = new HashMap<String, String>();

    /**
     * As {@link #otypes} but with first character upper case.
     */
    private static Map<String, String> uoTypes = new HashMap<String, String>();

    /**
     * As {@link #types} but all upper case.
     */
    private static Map<String, String> auTypes = new HashMap<String, String>();

    private static final String[] lockVariants = new String[] {"", "unlockClass", "unlockReceiver"};

    static {
        for (String type : types) {
            uTypes.put(type, type.substring(0, 1).toUpperCase() + type.substring(1));
            lTypes.put(type, type.substring(0, 1).toLowerCase() + type.substring(1));
            oTypes.put(type, type.equals("Reference") ? "Object" : type);
            uoTypes.put(type, type.equals("Reference") ? "Object" : uTypes.get(type));
            auTypes.put(type, type.toUpperCase());
        }
    }

    public static PrintStream o = System.out;

    /**
     * Lower-case variant {@code type}.
     * @param type a member of {@link #types}
     * @return
     */
    public static String lType(String type) {
        return lTypes.get(type);
    }

    /**
     * Returns {@code type} with first character upper cased.
     * @param type type a member of {@link #types}
     * @return
     */
    public static String uType(String type) {
        return uTypes.get(type);
    }

    /**
     * Returns {@code type} but with {@code Reference} replaced by {@code Object}.
     * @param type type a member of {@link #types}
     * @return
     */
    public static String oType(String type) {
        return oTypes.get(type);
    }

    /**
     * Returns the result of {@link #otype(type)} with first character upper cased.
     * @param type type a member of {@link #types}
     * @return
     */
    public static String uoType(String type) {
        return uoTypes.get(type);
    }

    /**
     * Returns the all upper-case version of the type.
     * @param type type a member of {@link #types}
     * @return
     */
    public static String auType(String type) {
        return auTypes.get(type);
    }

    /**
     * Returns the argument with first character upper-cased.
     * @param s
     * @return
     */
    public static String toFirstUpper(String s) {
        if (s.length() == 0) {
            return s;
        } else {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

    /**
     * If the argument is the empty string, returns it, otherwise returns it prefixed with a {@code $}.
     * @param s
     * @return
     */
    public static String prefixDollar(String s) {
        if (s.length() == 0) {
            return s;
        } else {
            return "$" + s;
        }
    }

    public static boolean isRefOrWord(String k) {
        return k.equals("Reference") || k.equals("Word");
    }

    public static boolean isTwoStackWords(String k) {
        return k.equals("long") || k.equals("double");
    }

    /**
     * The string that precedes the generic template tag name to indicate type.
     * E.g. The {@code A} in {@code ALOAD}.
     * @param k
     * @return
     */
    public static String tagPrefix(String k) {
        return k.equals("Reference") ? "A" : (k.equals("void") ? "" : uType(k).substring(0, 1));
    }

    /**
     * The string that precedes the generic template method name to indicate type.
     * @param k
     * @return
     */
    public static String opPrefix(String k) {
        return  k.equals("Reference") ? "a" : (k.equals("void") ? "v" : lType(k).substring(0, 1));
    }

    public static boolean hasGetPutOps(String k) {
        return !k.equals("void");
    }
    public static boolean hasArrayOps(String k) {
        return !(k.equals("void") || k.equals("byte") || k.equals("Word"));
    }

    public static boolean hasPOps(String k) {
        return !(k.equals("void") || k.equals("boolean")/* || k.equals("char")*/);
    }

    public static boolean hasPCmpSwpOps(String k) {
        return k.equals("int") || k.equals("Reference") || k.equals("Word");
    }

    public static boolean hasI2Ops(String k) {
        return !(k.equals("int") || k.equals("void") || k.equals("Reference") || k.equals("Word") || k.equals("boolean"));
    }

    public static boolean hasL2Ops(String k) {
        return k.equals("int") || k.equals("float") || k.equals("double");
    }

    public static boolean hasF2Ops(String k) {
        return k.equals("int") || k.equals("long") || k.equals("double");
    }

    public static boolean hasD2Ops(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float");
    }

    public static boolean hasArithOps(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float") || k.equals("double");
    }

    public static boolean hasLogOps(String k) {
        return k.equals("int") || k.equals("long");
    }

    public static String algOp(String op) {
        if (op.equals("add")) {
            return "+";
        } else if (op.equals("sub")) {
            return "-";
        } else if (op.equals("mul")) {
            return "*";
        } else if (op.equals("div")) {
            return "/";
        } else if (op.equals("rem")) {
            return "%";
        } else if (op.equals("or")) {
            return "|";
        } else if (op.equals("and")) {
            return "&";
        } else if (op.equals("xor")) {
            return "^";
        } else if (op.equals("shl")) {
            return "<<";
        } else if (op.equals("shr")) {
            return ">>";
        } else if (op.equals("ushr")) {
            return ">>>";
        } else {
            return "???";
        }
    }

    public static boolean hasLoadStoreOps(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float") || k.equals("double") || k.equals("Reference") || k.equals("Word");
    }

    public static boolean hasLDCOps(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float") || k.equals("double") || k.equals("Reference");
    }

    public static boolean hasReturnOps(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float") || k.equals("double") ||
               k.equals("Word") || k.equals("Reference") || k.equals("void");
    }

    public static boolean hasInvokeOps(String k) {
        return k.equals("void") || k.equals("float") || k.equals("long") || k.equals("double") || k.equals("Word");
    }

    public static boolean hasIfCmpOps(String k) {
        return k.equals("int") || k.equals("Reference");
    }

    public static boolean hasCmpOps(String k) {
        return k.equals("float") || k.equals("double") || k.equals("long");
    }

    public static void generateAutoComment() {
        o.printf("    // GENERATED -- EDIT AND RUN T1XTemplateGenerator.main() TO MODIFY%n");
    }

    public static void newLine() {
        o.println();
    }

    private static StringBuilder tagSB = new StringBuilder();
    private static Formatter formatter = new Formatter(tagSB);
    private static T1XTemplateTag currentTemplateTag;

    /**
     * Generates and outputs a {@link T1XTemplate} annotation from a format string and associated arguments.
     * Sets {@link #currentTemplateTag} as a side effect to support passing to any {@link #adviceHook}.
     * @param f
     * @param args
     */
    private static void generateTemplateTag(String f, Object ... args) {
        tagSB.setLength(0);
        formatter.format(f, args);
        String tagString =  tagSB.toString();
        currentTemplateTag = T1XTemplateTag.valueOf(tagString);
        o.printf("    @T1X_TEMPLATE(%s)%n", tagString);
    }

    @INLINE
    private static void generateBeforeAdvice(String ... args) {
        if (adviceHook != null) {
            adviceHook.generate(currentTemplateTag, AdviceType.BEFORE, args);
        }
    }

    @INLINE
    private static void generateAfterAdvice(String ... args) {
        if (adviceHook != null) {
            adviceHook.generate(currentTemplateTag, AdviceType.AFTER, args);
        }
    }


    private static void generateShortConstOps(String k, String arg) {
        generateAutoComment();
        generateTemplateTag("%sCONST_%s", tagPrefix(k), arg);
        o.printf("    public static void %sconst_%s() {%n", opPrefix(k), arg.toLowerCase());
        String argVal = k.equals("Word") ? "Address.zero()" : (arg.equals("M1") ? "-1" : arg.toLowerCase());
        generateBeforeAdvice(k);
        o.printf("        push%s(%s);%n", uoType(k), argVal);
        o.printf("    }%n");
        newLine();
    }


    private static void generateConstOps(String k) {
        generateAutoComment();
        generateTemplateTag("%sCONST", tagPrefix(k));
        o.printf("    public static void %sconst(%s constant) {%n", opPrefix(k), k);
        generateBeforeAdvice(k);
        o.printf("        push%s(constant);%n", uType(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generatePutField(String k) {
        final boolean isTwoStackWords = isTwoStackWords(k);
        final int peekOffset = isTwoStackWords ? 2 : 1;
        final String m = uoType(k).equals("Object") ? "noninlineW" : "w";
        generateAutoComment();
        generateTemplateTag("PUTFIELD$%s$resolved", lType(k));
        o.printf("    public static void putfield%s(int offset) {%n", uType(k));
        o.printf("        Object object = peekObject(%d);%n", peekOffset);
        o.printf("        %s value = peek%s(0);%n", oType(k), uoType(k));
        generateBeforeAdvice(k);
        o.printf("        removeSlots(%d);%n", peekOffset + 1);
        o.printf("        TupleAccess.%srite%s(object, offset, value);%n", m, uoType(k));
        o.printf("    }%n");
        newLine();

        generateAutoComment();
        generateTemplateTag("PUTFIELD$%s", lType(k));
        o.printf("    public static void putfield%s(ResolutionGuard.InPool guard) {%n", uType(k));
        o.printf("        Object object = peekObject(%d);%n", peekOffset);
        o.printf("        %s value = peek%s(0);%n", oType(k), uoType(k));
        o.printf("        resolveAndPutField%s(guard, object, value);%n", uType(k));
        o.printf("        removeSlots(%d);%n", peekOffset + 1);
        o.printf("    }%n");
        newLine();

        generateAutoComment();
        o.printf("    @NEVER_INLINE%n");
        o.printf("    public static void resolveAndPutField%s(ResolutionGuard.InPool guard, Object object, %s value) {%n", uType(k), oType(k));
        o.printf("        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);%n");
        generateBeforeAdvice(k);
        o.printf("        if (f.isVolatile()) {%n");
        o.printf("            preVolatileWrite();%n");
        o.printf("            TupleAccess.write%s(object, f.offset(), value);%n", uoType(k));
        o.printf("            postVolatileWrite();%n");
        o.printf("        } else {%n");
        o.printf("            TupleAccess.write%s(object, f.offset(), value);%n", uoType(k));
        o.printf("        }%n");
        o.printf("    }%n");
        o.printf("%n");

        generateAutoComment();
        generateTemplateTag("PUTSTATIC$%s$init", lType(k));
        o.printf("    public static void putstatic%s(Object staticTuple, int offset) {%n", uType(k));
        o.printf("        %s value = pop%s();%n", oType(k), uoType(k));
        generateBeforeAdvice(k);
        o.printf("        TupleAccess.%srite%s(staticTuple, offset, value);%n", m, uoType(k));
        o.printf("    }%n");
        newLine();

        generateAutoComment();
        generateTemplateTag("PUTSTATIC$%s", lType(k));
        o.printf("    public static void putstatic%s(ResolutionGuard.InPool guard) {%n", uType(k));
        o.printf("        resolveAndPutStatic%s(guard, pop%s());%n", uType(k), uoType(k));
        o.printf("    }%n");
        newLine();

        generateAutoComment();
        o.printf("    @NEVER_INLINE%n");
        o.printf("    public static void resolveAndPutStatic%s(ResolutionGuard.InPool guard, %s value) {%n", uType(k), oType(k));
        o.printf("        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);%n");
        generateBeforeAdvice(k);
        o.printf("        Snippets.makeHolderInitialized(f);%n");
        o.printf("        if (f.isVolatile()) {%n");
        o.printf("            preVolatileWrite();%n");
        o.printf("            TupleAccess.write%s(f.holder().staticTuple(), f.offset(), value);%n", uoType(k));
        o.printf("            postVolatileWrite();%n");
        o.printf("        } else {%n");
        o.printf("            TupleAccess.write%s(f.holder().staticTuple(), f.offset(), value);%n", uoType(k));
        o.printf("        }%n");
        o.printf("    }%n");
        o.printf("%n");
    }

    private static void generateGetField(String k) {
        final boolean isTwoStackWords = isTwoStackWords(k);
        generateAutoComment();
        generateTemplateTag("GETFIELD$%s$resolved", lType(k));
        o.printf("    public static void getfield%s(int offset) {%n", uType(k));
        o.printf("        Object object = peekObject(0);%n");
        generateBeforeAdvice(k);
        if (isTwoStackWords) {
            o.printf("        addSlots(1);%n");
        }
        o.printf("        poke%s(0, TupleAccess.read%s(object, offset));%n", uoType(k), uoType(k));
        o.printf("    }%n");
        newLine();

        generateAutoComment();
        generateTemplateTag("GETFIELD$%s", lType(k));
        o.printf("    public static void getfield%s(ResolutionGuard.InPool guard) {%n", uType(k));
        o.printf("        Object object = peekObject(0);%n");
        if (isTwoStackWords) {
            o.printf("        addSlots(1);%n");
        }
        o.printf("        poke%s(0, resolveAndGetField%s(guard, object));%n", uoType(k), uType(k));
        o.printf("    }%n");
        newLine();

        generateAutoComment();
        o.printf("    @NEVER_INLINE");
        o.printf("    public static %s resolveAndGetField%s(ResolutionGuard.InPool guard, Object object) {%n", oType(k), uType(k));
        o.printf("        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);%n");
        generateBeforeAdvice(k);
        o.printf("        if (f.isVolatile()) {%n");
        o.printf("            preVolatileRead();%n");
        o.printf("            %s value = TupleAccess.read%s(object, f.offset());%n", oType(k), uoType(k));
        o.printf("            postVolatileRead();%n");
        o.printf("            return value;%n");
        o.printf("        } else {%n");
        o.printf("            return TupleAccess.read%s(object, f.offset());%n", uoType(k));
        o.printf("        }%n");
        o.printf("    }%n");
        o.printf("%n");

        generateAutoComment();
        generateTemplateTag("GETSTATIC$%s", lType(k));
        o.printf("    public static void getstatic%s(ResolutionGuard.InPool guard) {%n", uType(k));
        o.printf("        push%s(resolveAndGetStatic%s(guard));%n", uoType(k), uType(k));
        o.printf("    }%n");
        newLine();

        generateAutoComment();
        o.printf("    @NEVER_INLINE");
        o.printf("    public static %s resolveAndGetStatic%s(ResolutionGuard.InPool guard) {%n", oType(k), uType(k));
        o.printf("        FieldActor f = Snippets.resolveStaticFieldForReading(guard);%n");
        o.printf("        Snippets.makeHolderInitialized(f);%n");
        generateBeforeAdvice(k);
        o.printf("        if (f.isVolatile()) {%n");
        o.printf("            preVolatileRead();%n");
        o.printf("            %s value = TupleAccess.read%s(f.holder().staticTuple(), f.offset());%n", oType(k), uoType(k));
        o.printf("            postVolatileRead();%n");
        o.printf("            return value;%n");
        o.printf("        } else {%n");
        o.printf("            return TupleAccess.read%s(f.holder().staticTuple(), f.offset());%n", uoType(k));
        o.printf("        }%n");
        o.printf("    }%n");
        o.printf("%n");

        generateAutoComment();
        generateTemplateTag("GETSTATIC$%s$init", lType(k));
        o.printf("    public static void getstatic%s(Object staticTuple, int offset) {%n", uType(k));
        generateBeforeAdvice(k);
        o.printf("        push%s(TupleAccess.read%s(staticTuple, offset));%n", uoType(k), uoType(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generateLoadOps(String k) {
        generateAutoComment();
        generateTemplateTag("%sLOAD", tagPrefix(k));
        o.printf("    public static void %sload(int dispToLocalSlot) {%n", opPrefix(k));
        o.printf("        %s value = getLocal%s(dispToLocalSlot);%n", oType(k), uoType(k));
        generateBeforeAdvice(k);
        o.printf("        push%s(value);%n", uoType(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generateLDCOps(String k) {
        String tail = k.equals("Reference") ? "$resolved" : "";
        generateAutoComment();
        generateTemplateTag("LDC$%s%s", lType(k), tail);
        o.printf("    public static void %sldc(%s constant) {%n", lType(k).charAt(0), oType(k));
        generateBeforeAdvice(k);
        o.printf("        push%s(constant);%n", uoType(k));
        o.printf("    }%n");
        newLine();

        if (!k.equals("Reference")) {
            return;
        }
        generateAutoComment();
        generateTemplateTag("LDC$%s", lType(k));
        o.printf("    public static void urldc(ResolutionGuard guard) {%n");
        o.printf("        ClassActor classActor = Snippets.resolveClass(guard);%n");
        o.printf("        Object constant = T1XRuntime.getClassMirror(classActor);%n");
        generateBeforeAdvice(k);
        o.printf("        push%s(constant);%n", oType(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generateStoreOps(String k) {
        generateAutoComment();
        generateTemplateTag("%sSTORE", tagPrefix(k));
        o.printf("    public static void %sstore(int dispToLocalSlot) {%n", opPrefix(k));
        o.printf("        %s value = pop%s();%n", oType(k), uoType(k));
        generateBeforeAdvice(k);
        o.printf("        setLocal%s(dispToLocalSlot, value);%n", uoType(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generateArrayLoad(String k) {
        final boolean isTwoStackWords = isTwoStackWords(k);
        generateAutoComment();
        generateTemplateTag("%sALOAD", tagPrefix(k));
        o.printf("    public static void %saload() {%n", opPrefix(k));
        o.printf("        int index = peekInt(0);%n");
        o.printf("        Object array = peekObject(1);%n");
        o.printf("        ArrayAccess.checkIndex(array, index);%n");
        generateBeforeAdvice(k);
        if (!isTwoStackWords) {
            o.printf("        removeSlots(1);%n");
        }
        o.printf("        poke%s(0, ArrayAccess.get%s(array, index));%n", uoType(k), uoType(k));
        o.printf("    }%n");
        newLine();
    }

    private static void generateIPushOps(String k) {
        generateAutoComment();
        generateTemplateTag("%sIPUSH", tagPrefix(k));
        o.printf("    public static void %sipush(%s value) {%n", opPrefix(k), k);
        generateBeforeAdvice(k);
        o.printf("        pushInt(value);%n");
        o.printf("    }%n");
        newLine();

    }

    private static void generateArrayStore(String k) {
        final boolean isTwoStackWords = isTwoStackWords(k);
        final int indexSlot = isTwoStackWords ? 2 : 1;
        generateAutoComment();
        generateTemplateTag("%sASTORE", tagPrefix(k));
        o.printf("    public static void %sastore() {%n", opPrefix(k));
        o.printf("        int index = peekInt(%d);%n", indexSlot);
        o.printf("        Object array = peekObject(%d);%n", indexSlot + 1);
        o.printf("        ArrayAccess.checkIndex(array, index);%n");
        o.printf("        %s value = peek%s(0);%n", oType(k), uoType(k));
        generateBeforeAdvice(k);
        if (k.equals("Reference")) {
            o.printf("        ArrayAccess.checkSetObject(array, value);%n");
        }
        o.printf("        ArrayAccess.set%s(array, index, value);%n", uoType(k));
        o.printf("        removeSlots(%d);%n", indexSlot + 2);
        o.printf("    }%n");
        newLine();
    }

    private static void generateNewOp(String init) {
        generateAutoComment();
        String t;
        String m;
        if (init.equals("")) {
            t = "ResolutionGuard";
            m = "resolveClassForNewAndCreate";
        } else {
            t = "ClassActor";
            m = "createTupleOrHybrid";
        }
        generateTemplateTag("NEW%s", prefixDollar(init));
        o.printf("    public static void new_(%s arg) {%n", t);
        o.printf("        Object object = %s(arg);%n", m);
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        pushObject(object);%n");
        o.printf("    }%n");
        newLine();
    }

    private static void generateNewArrayOp() {
        generateAutoComment();
        generateTemplateTag("NEWARRAY");
        o.printf("    public static void newarray(Kind<?> kind) {%n");
        o.printf("        int length = peekInt(0);%n");
        o.printf("        Object array = createPrimitiveArray(kind, length);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        pokeObject(0, array);%n");
        o.printf("    }%n");
        newLine();
    }

    private static void generateANewArrayOp(String resolved) {
        generateAutoComment();
        String t;
        String v;
        if (resolved.equals("")) {
            t = "ResolutionGuard";
            v = "guard";
        } else {
            t = "ArrayClassActor<?>";
            v = "arrayClassActor";
        }
        generateTemplateTag("ANEWARRAY%s", prefixDollar(resolved));
        o.printf("    public static void anewarray(%s %s) {%n", t, v);
        if (resolved.equals("")) {
            o.printf("        ArrayClassActor<?> arrayClassActor = UnsafeCast.asArrayClassActor(Snippets.resolveArrayClass(guard));%n");
        }
        o.printf("        int length = peekInt(0);%n");
        o.printf("        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        pokeObject(0, array);%n");
        o.printf("    }%n");
        newLine();
    }

    private static void generateMultiANewArrayOp(String resolved) {
        generateAutoComment();
        String t;
        String v;
        if (resolved.equals("")) {
            t = "ResolutionGuard";
            v = "guard";
        } else {
            t = "ArrayClassActor<?>";
            v = "arrayClassActor";
        }
        generateTemplateTag("MULTIANEWARRAY%s", prefixDollar(resolved));
        o.printf("    public static void multianewarray(%s %s, int[] lengthsShared) {%n", t, v);
        if (resolved.equals("")) {
            o.printf("        ClassActor arrayClassActor = Snippets.resolveClass(guard);%n");
        }
        o.printf("        // Need to use an unsafe cast to remove the checkcast inserted by javac as that%n");
        o.printf("        // causes this template to have a reference literal in its compiled form.%n");
        o.printf("        int[] lengths = UnsafeCast.asIntArray(cloneArray(lengthsShared));%n");
        o.printf("        int numberOfDimensions = lengths.length;%n");
        o.println();
        o.printf("        for (int i = 1; i <= numberOfDimensions; i++) {%n");
        o.printf("            int length = popInt();%n");
        o.printf("            checkArrayDimension(length);%n");
        o.printf("            ArrayAccess.setInt(lengths, numberOfDimensions - i, length);%n");
        o.printf("        }%n");
        o.printf("        %n");
        o.printf("        Object array = Snippets.createMultiReferenceArray(arrayClassActor, lengths);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        pushObject(array);%n");
        o.printf("    }%n");
        newLine();
    }

    private static void generateCheckcastOp(String resolved) {
        String t;
        String m;
        if (resolved.equals("")) {
            t = "ResolutionGuard";
            m = "resolveAndCheckcast";
        } else {
            t = "ClassActor";
            m = "Snippets.checkCast";
        }
        generateAutoComment();
        generateTemplateTag("CHECKCAST%s", prefixDollar(resolved));
        o.printf("    public static void checkcast(%s arg) {%n", t);
        o.printf("        Object value = peekObject(0);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        %s(arg, value);%n", m);
        o.printf("    }%n");
        newLine();
    }

    private static void generateInstanceofOp(String resolved) {
        generateAutoComment();
        String t;
        String v;
        if (resolved.equals("")) {
            t = "ResolutionGuard";
            v = "guard";
        } else {
            t = "ClassActor";
            v = "classActor";
        }
        generateTemplateTag("INSTANCEOF%s", prefixDollar(resolved));
        o.printf("    public static void instanceof_(%s %s) {%n", t, v);
        if (resolved.equals("")) {
            o.printf("        ClassActor classActor = Snippets.resolveClass(guard);%n");
        }
        o.printf("        Object object = peekObject(0);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        pokeInt(0, UnsafeCast.asByte(Snippets.instanceOf(classActor, object)));%n");
        o.printf("    }%n");
        newLine();
    }

    private static void generateArraylengthOp() {
        generateAutoComment();
        generateTemplateTag("ARRAYLENGTH");
        o.printf("    public static void arraylength() {%n");
        o.printf("        Object array = peekObject(0);%n");
        o.printf("        int length = ArrayAccess.readArrayLength(array);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        pokeInt(0, length);%n");
        o.printf("    }%n");
        newLine();
    }

    private static void generateAThrowOp() {
        generateAutoComment();
        generateTemplateTag("ATHROW");
        o.printf("    public static void athrow() {%n");
        o.printf("        Object object = peekObject(0);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        Throw.raise(object);%n");
        o.printf("    }%n");
        newLine();
    }

    private static void generateMonitorOp(String tag) {
        generateAutoComment();
        generateTemplateTag("MONITOR%s", tag.toUpperCase());
        o.printf("    public static void monitor%s() {%n", tag);
        o.printf("        Object object = peekObject(0);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        T1XRuntime.monitor%s(object);%n", tag);
        o.printf("        removeSlots(1);%n");
        o.printf("    }%n");
        newLine();
    }


    /*
     * A note on the template sources for the invocation of resolved methods.
     *
     * The calling convention is such that the part of the callee's frame that contains the incoming arguments of the method
     * is the top of the stack of the caller. The rest of the frame is built on method entry. Thus, the template for a
     * method invocation doesn't need to marshal the arguments. We can't just have templates compiled from standard method
     * invocations as the compiler mixes instructions for arguments passing and method invocation. So we have to write
     * template such that the call is explicitly made (using the Call SpecialBuiltin). Further, we need a template for each
     * of the four kinds of returned result (void, one word, two words, a reference).
     *
     * For methods with a static binding (e.g., methods invoked via invokestatic or invokespecial), we just need to issue a
     * call. Thus a template for these bytecode is a single call instruction. A template resulting in this can be achieved
     * by invoke a parameterless static method of an initialized class. We generate these templates for completion, although
     * in practice a JIT might be better off generating the call instruction directly.
     *
     * For dynamic method, the receiver is needed and method dispatch need to be generated. For the template, we pick an
     * object at an arbitrary position on the expression stack to be the receiver, and rely on the optimizing compiler to
     * generate dependency information about the constant value used as offset to read off the expression stack. JIT
     * compilers just have to modify this offset using the appropriate instruction modifier from the generated template.
     * Similarly, JIT compilers have to customized the virtual table index / itable serial identifier. Note that we use a
     * parameter-less void method in the expression performing dynamic method selection, regardless of the number of
     * parameters or of the kind of return value the template is to be used for.
     *
     * Laurent Daynes
     */


    private static void generateInvokeVI(String k, String variant, String tag) {
        String param1 = tag.equals("") ? "ResolutionGuard.InPool guard" :
            (variant.equals("interface") ? "InterfaceMethodActor interfaceMethodActor" : "int vTableIndex");
        param1 += ", int receiverStackIndex";
        if (tag.equals("instrumented")) {
            param1 += ", MethodProfile mpo, int mpoIndex";
        }
        generateAutoComment();
        generateTemplateTag("INVOKE%s$%s%s", variant.toUpperCase(), lType(k), prefixDollar(tag));
        o.printf("    public static void invoke%s%s(%s) {%n", variant, uType(k), param1);
        o.printf("        Object receiver = peekObject(receiverStackIndex);%n");
        generateBeforeAdvice(k, variant, tag);
        if (variant.equals("interface")) {
            if (tag.equals("")) {
                o.printf("        Address entryPoint = resolveAndSelectInterfaceMethod(guard, receiver);%n");
            } else if (tag.equals("resolved")) {
                o.printf("        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress();%n");
            } else if (tag.equals("instrumented")) {
                o.printf("        Address entryPoint = Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex);%n");
            }
        } else {
            if (tag.equals("")) {
                o.printf("        Address entryPoint = resolveAndSelectVirtualMethod(receiver, guard, receiverStackIndex);%n");
            } else if (tag.equals("resolved")) {
                o.printf("        Address entryPoint = ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress();%n");
            } else if (tag.equals("instrumented")) {
                o.printf("        Address entryPoint = selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex);%n");
            }
        }

        o.printf("        indirectCall%s(entryPoint, CallEntryPoint.VTABLE_ENTRY_POINT, receiver);%n", uType(k));
        generateAfterAdvice(k, variant, tag);
        o.printf("    }%n");
        newLine();
    }


    private static void generateInvokeSS(String k, String variant, String xtag) {
        String tag = variant.equals("static") && xtag.equals("resolved") ? "init" : xtag;
        String params = tag.equals("") ? "ResolutionGuard.InPool guard" : "";
        if (variant.equals("special")) {
            if (params.length() > 0) {
                params += ", ";
            }
            params += "int receiverStackIndex";
        }
        generateAutoComment();
        generateTemplateTag("INVOKE%s$%s%s", variant.toUpperCase(), lType(k), prefixDollar(tag));
        o.printf("    public static void invoke%s%s(%s) {%n", variant, uType(k), params);
        if (variant.equals("special")) {
            o.printf("        Pointer receiver = peekWord(receiverStackIndex).asPointer();%n");
            o.printf("        nullCheck(receiver);%n");
        }
        generateBeforeAdvice(k, variant, tag);
        if (xtag.equals("resolved")) {
            o.printf("        directCall%s();%n", uType(k));
        } else {
            o.printf("        indirectCall%s(resolve%sMethod(guard), CallEntryPoint.OPTIMIZED_ENTRY_POINT);%n", uType(k), toFirstUpper(variant));
        }
        generateAfterAdvice(k, variant, tag);
        o.printf("    }%n");
        newLine();
    }


    private static void generateI2Ops(String k) {
        final boolean isTwoStackWords = isTwoStackWords(k);
        generateAutoComment();
        generateTemplateTag("I2%c", uType(k).charAt(0));
        o.printf("    public static void i2%c() {%n", k.charAt(0));
        o.printf("        int value = peekInt(0);%n");
        if (isTwoStackWords) {
            o.printf("        addSlots(1);%n");
        }
        String cast = k.equals("char") || k.equals("byte") || k.equals("short") ? "(" + k + ") " : "";
        generateBeforeAdvice(k);
        o.printf("        poke%s(0, %svalue);%n", uType(k), cast);
        o.printf("    }%n");
        newLine();
    }


    private static void generateL2Ops(String k) {
        final boolean isTwoStackWords = isTwoStackWords(k);
        generateAutoComment();
        generateTemplateTag("L2%c", uType(k).charAt(0));
        o.printf("    public static void l2%c() {%n", k.charAt(0));
        o.printf("        long value = peekLong(0);%n");
        if (!isTwoStackWords) {
            o.printf("        removeSlots(1);%n");
        }
        generateBeforeAdvice(k);
        String cast = k.equals("int") ? "(int) " : "";
        o.printf("        poke%s(0, %svalue);%n", uType(k), cast);
        o.printf("    }%n");
        newLine();
    }


    private static void generateD2Ops(String k) {
        final boolean isTwoStackWords = isTwoStackWords(k);
        generateAutoComment();
        generateTemplateTag("D2%c", uType(k).charAt(0));
        o.printf("    public static void d2%c() {%n", k.charAt(0));
        o.printf("        double value = peekDouble(0);%n");
        if (!isTwoStackWords) {
            o.printf("        removeSlots(1);%n");
        }
        generateBeforeAdvice(k);
        String arg2 = k.equals("float") ? "(float) value" : "T1XRuntime.d2" + k.charAt(0) + "(value)";
        o.printf("        poke%s(0, %s);%n", uType(k), arg2);
        o.printf("    }%n");
        newLine();
    }


    private static void generateF2Ops(String k) {
        final boolean isTwoStackWords = isTwoStackWords(k);
        generateAutoComment();
        generateTemplateTag("F2%c", uType(k).charAt(0));
        o.printf("    public static void f2%c() {%n", k.charAt(0));
        o.printf("        float value = peekFloat(0);%n");
        if (isTwoStackWords) {
            o.printf("        addSlots(1);%n");
        }
        generateBeforeAdvice(k);
        String arg2 = k.equals("double") ? "value" : "T1XRuntime.f2" + k.charAt(0) + "(value)";
        o.printf("        poke%s(0, %s);%n", uType(k), arg2);
        o.printf("    }%n");
        newLine();
    }

    private static void generateMovOps(String from, String to) {
        generateAutoComment();
        generateTemplateTag("MOV_%s2%s", uType(from).charAt(0), uType(to).charAt(0));
        o.printf("    public static void mov_%s2%s() {%n", from.charAt(0), to.charAt(0));
        o.printf("        %s value = peek%s(0);%n", from, uType(from));
        generateBeforeAdvice(from, to);
        o.printf("        poke%s(0, Intrinsics.%sTo%s(value));%n", uType(to), from, uType(to));
        o.printf("    }%n");
        newLine();
    }

    private static void generatePopOp(int arg) {
        generateAutoComment();
        final String tag = arg == 1 ? "" : Integer.toString(arg);
        generateTemplateTag("POP%s", tag);
        o.printf("    public static void pop%s() {%n", tag);
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        removeSlots(%d);%n", arg);
        o.printf("    }%n");
        newLine();
    }

    private static void generateNegOps(String k) {
        final String op = "neg";
        final String param = k.equals("float") || k.equals("double") ? k + " zero" : "";
        final String op1 = k.equals("float") || k.equals("double") ? "zero - " : "-";
        generateAutoComment();
        generateTemplateTag("%s%s", tagPrefix(k), op.toUpperCase());
        o.printf("    public static void %s%s(%s) {%n", opPrefix(k), op, param);
        o.printf("        %s value = %speek%s(0);%n", k, op1, uType(k));
        generateBeforeAdvice(k);
        o.printf("        poke%s(0, value);%n", uType(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generateWordDivRemOps(String op, String iTag) {
        final String m = op.equals("div") ? "dividedBy" : "remainder";
        generateAutoComment();
        generateTemplateTag("W%s%s", op.toUpperCase(), iTag.toUpperCase());
        o.printf("    public static void w%s%s() {%n", op, iTag);
        if (iTag.equals("i")) {
            o.printf("        int value2 = peekInt(0);%n");
        } else {
            o.printf("        Address value2 = peekWord(0).asAddress();%n");
        }
        o.printf("        Address value1 = peekWord(1).asAddress();%n");
        generateBeforeAdvice(op, iTag);
        o.printf("        removeSlots(1);%n");
        final String t = op.equals("rem") && iTag.equals("i") ? "Int" : "Word";
        o.printf("        poke%s(0, value1.%s(value2));%n", t, m);
        o.printf("    }%n");
        newLine();
    }

    private static void generateDyadicOps(String k, String op, boolean forceInt) {
        final int removeCount = isTwoStackWords(k) && !forceInt ? 2 : 1;
        generateAutoComment();
        generateTemplateTag("%s%s", tagPrefix(k), op.toUpperCase());
        o.printf("    public static void %s%s() {%n", opPrefix(k), op);
        o.printf("        %s value2 = peek%s(0);%n", forceInt ? "int" : k, forceInt ? "Int" : uType(k));
        o.printf("        %s value1 = peek%s(%d);%n", k, uType(k), removeCount);
        generateBeforeAdvice(k);
        o.printf("        removeSlots(%d);%n", removeCount);
        o.printf("        poke%s(0, value1 %s value2);%n", uType(k), algOp(op));
        o.printf("    }%n");
        newLine();
    }

    private static void generateIIncOp() {
        generateAutoComment();
        generateTemplateTag("IINC");
        o.printf("    public static void iinc(int dispToLocalSlot, int increment) {%n");
        o.printf("        int value = getLocalInt(dispToLocalSlot);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        setLocalInt(dispToLocalSlot, value  + increment);%n");
        o.printf("    }%n");
        newLine();
    }

    private static void generateReturn(String k, String unlock) {
        // Admittedly, the readability goal is a stretch here!
        final String arg = unlock.equals("") ? "" :
            (unlock.equals("unlockClass") ? "Class<?> rcvr" : "int dispToRcvrCopy");
        generateAutoComment();
        generateTemplateTag("%sRETURN%s", tagPrefix(k), prefixDollar(unlock));
        o.printf("    public static %s %sreturn%s(%s) {%n", oType(k), opPrefix(k), toFirstUpper(unlock), arg);
        if (unlock.equals("unlockReceiver") || unlock.equals("registerFinalizer")) {
            o.printf("        Object rcvr = getLocalObject(dispToRcvrCopy);%n");
        }
        generateBeforeAdvice(k);
        if (unlock.equals("registerFinalizer")) {
            o.printf("        if (ObjectAccess.readClassActor(rcvr).hasFinalizer()) {%n");
            o.printf("            SpecialReferenceManager.registerFinalizee(rcvr);%n");
            o.printf("        }%n");
        } else {
            if (unlock.length() > 0) {
                o.printf("        Monitor.noninlineExit(rcvr);%n");
            }
            if (!k.equals("void")) {
                o.printf("        return pop%s();%n", uoType(k));
            }
        }
        o.printf("    }%n");
        newLine();
    }


    /*
     * Templates for conditional branch bytecode instructions.
     *
     * These templates only comprise the prefix of a conditional branch: popping operands of the comparison and the comparison itself.
     * They have no dependencies, i.e., they can be copied as is by the bytecode-to-target translator of the JIT. The actual branching
     * is emitted by the JIT. Templates for the same family of bytecodes are identical (what typically makes them different is the condition being tested).
     * The templates relies on two special builtins for comparing issuing an object comparison and a integer comparison. These are specific to template generation.
     */

    private static void generateIfCmpOps(String k, String op) {
        generateAutoComment();
        generateTemplateTag("IF_%sCMP%s", tagPrefix(k), op.toUpperCase());
        o.printf("    public static void if_%scmp%s() {%n", opPrefix(k), op);
        generateBeforeAdvice(k);
        o.printf("        %scmp_prefix();%n", opPrefix(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generateIfOps(String k, String op) {
        generateAutoComment();
        generateTemplateTag("IF%s", op.toUpperCase());
        o.printf("    public static void if%s() {%n", op);
        generateBeforeAdvice(k);
        o.printf("        %scmp0_prefix();%n", opPrefix(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generateCmpOps(String k, String variant) {
        int value1Index = isTwoStackWords(k) ? 2 : 1;
        generateAutoComment();
        generateTemplateTag("%sCMP%s", tagPrefix(k), variant.toUpperCase());
        o.printf("    public static void %scmp%sOp() {%n", opPrefix(k), variant);
        o.printf("        %s value2 = peek%s(0);%n", k, uType(k));
        o.printf("        %s value1 = peek%s(%d);%n", k, uType(k), value1Index);
        o.printf("        int result = %scmp%s(value1, value2);%n", opPrefix(k), variant);
        generateBeforeAdvice(k);
        o.printf("        removeSlots(%d);%n", 2 * value1Index - 1);
        o.printf("        pokeInt(0, result);%n");
        o.printf("    }%n");
        newLine();
    }



    private static void generatePSet(String k) {
        if (k.equals("char")) {
            return;
        }
        final boolean isTwoStackWords = isTwoStackWords(k);
        final int indexSlot = isTwoStackWords ? 2 : 1;
        generateAutoComment();
        generateTemplateTag("PSET_%s", auType(k));
        o.printf("    public static void pset_%s() {%n", lType(k));
        o.printf("        %s value = peek%s(0);%n", k, uType(k));
        o.printf("        int index = peekInt(%d);%n", indexSlot);
        o.printf("        int disp = peekInt(%d);%n", indexSlot + 1);
        o.printf("        Pointer ptr = peekWord(%d).asPointer();%n", indexSlot + 2);
        generateBeforeAdvice(k);
        o.printf("        removeSlots(%d);%n", indexSlot + 3);
        o.printf("        ptr.set%s(disp, index, value);%n", uType(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generatePGet(String k) {
        final boolean isTwoStackWords = isTwoStackWords(k);
        generateAutoComment();
        generateTemplateTag("PGET_%s", auType(k));
        o.printf("    public static void pget_%s() {%n", lType(k));
        o.printf("        int index = peekInt(0);%n");
        o.printf("        int disp = peekInt(1);%n");
        o.printf("        Pointer ptr = peekWord(2).asPointer();%n");
        generateBeforeAdvice(k);
        o.printf("        removeSlots(%d);%n", isTwoStackWords ? 1 : 2);
        o.printf("        poke%s(0, ptr.get%s(disp, index));%n", uType(k), uType(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generatePRead(String k, boolean isI) {
        final boolean isTwoStackWords = isTwoStackWords(k);
        generateAutoComment();
        generateTemplateTag("PREAD_%s%s", auType(k), isI ? "_I" : "");
        o.printf("    public static void pread_%s%s() {%n", lType(k), isI ? "_i" : "");
        if (isI) {
            o.printf("        int off = peekInt(0);%n");
        } else {
            o.printf("        Offset off = peekWord(0).asOffset();%n");
        }
        o.printf("        Pointer ptr = peekWord(1).asPointer();%n");
        generateBeforeAdvice(k);
        if (!isTwoStackWords) {
            o.printf("        removeSlots(1);%n");
        }
        o.printf("        poke%s(0, ptr.read%s(off));%n", uType(k), uType(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generatePWrite(String k, boolean isI) {
        if (k.equals("char")) {
            return;
        }
        final boolean isTwoStackWords = isTwoStackWords(k);
        generateAutoComment();
        generateTemplateTag("PWRITE_%s%s", auType(k), isI ? "_I" : "");
        o.printf("    public static void pwrite_%s%s() {%n", lType(k), isI ? "_i" : "");
        o.printf("        Pointer ptr = peekWord(2).asPointer();%n");
        if (isI) {
            o.printf("        int off = peekInt(1);%n");
        } else {
            o.printf("        Offset off = peekWord(1).asOffset();%n");
        }
        o.printf("        %s value = peek%s(0);%n", k, uType(k));
        generateBeforeAdvice(k);
        o.printf("        removeSlots(%d);%n", isTwoStackWords ? 4 : 3);
        o.printf("        ptr.write%s(off, value);%n", uType(k));
        o.printf("    }%n");
        newLine();
    }


    private static void generatePCmpSwp(String k, boolean isI) {
        generateAutoComment();
        generateTemplateTag("PCMPSWP_%s%s", auType(k), isI ? "_I" : "");
        o.printf("    public static void pcmpswp_%s%s() {%n", lType(k), isI ? "_i" : "");
        o.printf("        %s newValue = peek%s(0);%n", k, uType(k));
        o.printf("        %s expectedValue = peek%s(1);%n", k, uType(k));
        if (isI) {
            o.printf("        int off = peekInt(2);%n");
        } else {
            o.printf("        Offset off = peekWord(2).asOffset();%n");
        }
        o.printf("        Pointer ptr = peekWord(3).asPointer();%n");
        generateBeforeAdvice(k);
        o.printf("        removeSlots(3);%n");
        o.printf("        poke%s(0, ptr.compareAndSwap%s(off, expectedValue, newValue));%n", uType(k), uType(k));
        o.printf("    }%n");
        newLine();
    }

    /**
     * Generate the template source code using the provided {@link AdviceHook} to the standard output.
     * The order of the output is first the typed operations, per type, followed by the untyped operations.
     * Ordering (grouping) by operation is not currently possible.
     * @param hook the advice hook or {@code null} if no advice
     */
    public static void generate(AdviceHook hook) {
        adviceHook = hook;
        for (String k : types) {
            // constants
            if (k.equals("int")) {
                for (String s : new String[] {"0", "1", "2", "3", "4", "5", "M1"}) {
                    generateShortConstOps(k, s);
                }
            } else if (k.equals("Reference")) {
                generateShortConstOps(k, "NULL");
            } else if (k.equals("Word")) {
                generateShortConstOps(k, "0");
            } else if (k.equals("float") || k.equals("double") || k.equals("long")) {
                generateConstOps(k);
            }
            if (hasGetPutOps(k)) {
                generateGetField(k);
                generatePutField(k);
            }
            if (hasLoadStoreOps(k)) {
                generateLoadOps(k);
                generateStoreOps(k);
            }
            if (hasLDCOps(k)) {
                generateLDCOps(k);
            }
            if (hasI2Ops(k)) {
                generateI2Ops(k);
            }
            if (hasL2Ops(k)) {
                generateL2Ops(k);
            }
            if (hasF2Ops(k)) {
                generateF2Ops(k);
            }
            if (hasD2Ops(k)) {
                generateD2Ops(k);
            }
            if (hasArithOps(k)) {
                for (String op : new String[] {"add", "sub", "mul", "div", "rem"}) {
                    generateDyadicOps(k, op, false);
                }
                generateNegOps(k);
            }
            if (hasLogOps(k)) {
                for (String op : new String[] {"or", "and", "xor"}) {
                    generateDyadicOps(k, op, false);
                }
                for (String op : new String[] {"shl", "shr", "ushr"}) {
                    generateDyadicOps(k, op, true);
                }
            }
            if (hasIfCmpOps(k)) {
                for (String s : new String[] {"eq", "ne", "lt", "ge", "gt", "le"}) {
                    if (k.equals("Reference") && !(s.equals("eq") || s.equals("ne"))) {
                        continue;
                    }
                    generateIfCmpOps(k, s);
                    if (!k.equals("Reference")) {
                        generateIfOps(k, s);
                    }
                }
                if (k.equals("Reference")) {
                    generateIfOps(k, "null");
                    generateIfOps(k, "nonnull");
                }
            }
            if (hasCmpOps(k)) {
                if (k.equals("long")) {
                    generateCmpOps(k, "");
                } else {
                    for (String s : new String[] {"g", "l"}) {
                        generateCmpOps(k, s);
                    }
                }
            }
            if (hasReturnOps(k)) {
                for (String lockType : lockVariants) {
                    generateReturn(k, lockType);
                }
            }
            if (hasArrayOps(k)) {
                generateArrayLoad(k);
                generateArrayStore(k);
            }
            if (hasInvokeOps(k))  {
                for (String s : new String[] {"virtual", "interface"}) {
                    for (String t : new String[] {"", "resolved", "instrumented"}) {
                        generateInvokeVI(k, s, t);
                    }
                }
                for (String s : new String[] {"special", "static"}) {
                    for (String t : new String[] {"", "resolved"}) {
                        generateInvokeSS(k, s, t);
                    }
                }
            }
            if (hasPOps(k)) {
                generatePGet(k);
                generatePSet(k);
                generatePRead(k, false);
                generatePWrite(k, false);
                generatePRead(k, true);
                generatePWrite(k, true);
            }
            if (hasPCmpSwpOps(k)) {
                generatePCmpSwp(k, false);
                generatePCmpSwp(k, true);
            }
        }
        // Special cases
        for (String s : new String[] {"div", "rem"}) {
            generateWordDivRemOps(s, "");
            generateWordDivRemOps(s, "i");
        }
        generateIIncOp();
        generateMovOps("float", "int");
        generateMovOps("int", "float");
        generateMovOps("double", "long");
        generateMovOps("long", "double");
        generateIPushOps("byte");
        generateIPushOps("short");
        generatePopOp(1);
        generatePopOp(2);
        generateNewOp("");
        generateNewOp("init");
        generateNewArrayOp();
        generateANewArrayOp("");
        generateANewArrayOp("resolved");
        generateMultiANewArrayOp("");
        generateMultiANewArrayOp("resolved");
        generateCheckcastOp("");
        generateCheckcastOp("resolved");
        generateArraylengthOp();
        generateAThrowOp();
        generateMonitorOp("enter");
        generateMonitorOp("exit");
        generateInstanceofOp("");
        generateInstanceofOp("resolved");
        generateReturn("void", "registerFinalizer");


    }

    public static void main(String[] args) {
        generate(null);
    }

}

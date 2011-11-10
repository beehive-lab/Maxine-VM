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
package com.oracle.max.vm.ext.t1x;

import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;
import static com.sun.max.vm.type.Kind.*;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.vm.type.*;

/**
 * {@HOSTED_ONLY} support for automatically generating template method sources.
 * The focus of the generating code is readability and not conciseness or performance.
 * Templates can be generated in several ways:
 * <ul>
 * <li>As a complete set via the {@link #generateAll(AdviceHook)} method.
 * <li>In families, e.g., all {@code PUTFIELD} templates via the {@link #generatePutFieldTemplates()} method.
 * <li>Singly, e.g. the {@code SIPUSH} template via the {@link #generateIPushTemplate(String)} method with
 * argument "short".
 * </ul>
 * The generated code is written to {@link #out}, which defaults to {@link System#out).
 * However, this stream can be changed before invoking any of the generating methods.
 * The default behavior is to generate all methods using the {@link #main} method.
 *
 * A mechanism, {@link AdviceHook}, is provided to allow "advice" to be inserted into the
 * generated templates. The templates are written in such a way to make this possible
 * without violating the constraints, e.g., no calls after modifying the stack.
 * This sometimes make the unadvised code a little more verbose than might appear necessary.
 * However, it has no impact on runtime performance.
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
         * The advice code may access any state that is accessible in the standard template.
         *
         * @param tag the tag identifying the template under generation
         * @param adviceType the advice type
         * @param args template-specific arguments that might be useful to the advice writer.
         * Typically it is the type associated with the template tag.
         */
        void generate(T1XTemplateTag tag, AdviceType adviceType, Object ... args);

        /**
         * Called just before the first output is generated for a method, which is the
         * comment noting that it is automatically generated. N.B. Most, but not all
         * generated methods are template tag implementations.
         */
        void startMethodGeneration();
    }

    @HOSTED_ONLY
    public enum AdviceType {
        BEFORE("Before"),
        AFTER("After");

        public final String methodNameComponent;

        AdviceType(String m) {
            this.methodNameComponent = m;
        }
    }

    private static final Object[] NULL_ARGS = new Object[] {};

    /**
     * {@link String} equivalent of {@link KindEnum} with standard case rules.
     * Arguably this class could use {@link KindEnum} more, but it is mostly doing string processing.
     */
    public static final Kind[] kinds = {BOOLEAN, BYTE, CHAR, SHORT, INT, FLOAT, LONG, DOUBLE, REFERENCE, WORD, VOID};

    public static final String[] conditions = new String[] {"eq", "ne", "lt", "ge", "gt", "le"};

    /**
     * Returns {@code k.javaName}.
     */
    public static String j(Kind k) {
        return k.javaClass.getSimpleName();
    }

    /**
     * Returns {@code k.javaName} with first character lower cased.
     */
    public static String l(Kind k) {
        return j(k).toLowerCase();
    }

    /**
     * Returns {@code k.javaName} with first character upper cased.
     */
    public static String u(Kind k) {
        return j(k).toUpperCase().substring(0, 1) + j(k).substring(1);
    }

    /**
     * Returns {@code k} except with "Object" replaced by "Reference".
     */
    public static String r(Kind k) {
        return k == REFERENCE ? "Reference" : k.javaClass.getSimpleName();
    }

    /**
     * Returns {@code k.stackKind().javaName}.
     */
    public static String s(Kind k) {
        return k.stackKind.javaClass.getSimpleName();
    }

    /**
     * Returns {@code k.stackKind().javaName} except with "Object" replaced by "Reference".
     */
    public static String rs(Kind k) {
        return k == REFERENCE ? "Reference" : s(k);
    }

    /**
     * Returns {@code k.javaName} with first letter upper-cased and "Object" replaced by "Reference".
     */
    public static String ur(Kind k) {
        return k == REFERENCE ? "Reference" : u(k);
    }

    /**
     * Returns {@code k.javaName} with first letter lower-cased and "object" replaced by "reference".
     */
    public static String lr(Kind k) {
        return k == REFERENCE ? "reference" : l(k);
    }

    /**
     * Returns {@code k.javaName} all upper-cased and "OBJECT" replaced by "REFERENCE".
     */
    public static String au(Kind k) {
        return r(k).toUpperCase();
    }

    public static String toStackKindCast(Kind k, String var) {
        if (k == BOOLEAN) {
            return "UnsafeCast.asByte(" + var + ")";
        } else if (k == REFERENCE) {
            return "Reference.fromJava(" + var + ")";
        } else {
            return var;
        }
    }

    public static String fromStackKindCast(Kind k, String var) {
        if (k == BOOLEAN) {
            return "UnsafeCast.asBoolean((byte) " + var + ")";
        } else if (k.stackKind == INT && k != INT) {
            return "(" + k.javaClass.getSimpleName() + ") " + var;
        } else {
            return var;
        }
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

    public static boolean isRefOrWord(Kind k) {
        return k == REFERENCE || k == WORD;
    }

    /**
     * The string that precedes the generic template tag name to indicate type.
     * E.g. The {@code A} in {@code ALOAD}.
     * @param k
     * @return
     */
    public static String tagPrefix(Kind k) {
        switch (k.asEnum) {
            case VOID:
                return "";
            case LONG:
                return "L";
            case REFERENCE:
                return "A";
            default:
                return String.valueOf(Character.toUpperCase(k.character));
        }
    }

    /**
     * The string that precedes the generic template method name to indicate type.
     * @param k
     * @return
     */
    public static String opPrefix(Kind k) {
        switch (k.asEnum) {
            case LONG:
                return "l";
            case REFERENCE:
                return "a";
            default:
                return String.valueOf(Character.toLowerCase(k.character));
        }
    }

    public static boolean hasGetPutTemplates(Kind k) {
        return k != VOID;
    }
    public static boolean hasArrayTemplates(Kind k) {
        return !(k == VOID || k == BOOLEAN || k == WORD);
    }

    public static boolean hasI2Templates(Kind k) {
        return !(k == INT || k == VOID || k == REFERENCE || k == WORD || k == BOOLEAN);
    }

    public static boolean hasL2Templates(Kind k) {
        return k == INT || k == FLOAT || k == DOUBLE;
    }

    public static boolean hasF2Templates(Kind k) {
        return k == INT || k == LONG || k == DOUBLE;
    }

    public static boolean hasD2Templates(Kind k) {
        return k == INT || k == LONG || k == FLOAT;
    }

    public static boolean hasArithTemplates(Kind k) {
        return k == INT || k == LONG || k == FLOAT || k == DOUBLE;
    }

    public static boolean hasLogTemplates(Kind k) {
        return k == INT || k == LONG;
    }

    private static boolean isShift(String op) {
        return op.equals("shl") || op.equals("shr") || op.equals("ushr");
    }

    private static String algOp(String op) {
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

    public static boolean hasReturnTemplates(Kind k) {
        return k == k.stackKind && k != WORD;
    }

    public static boolean hasInvokeTemplates(Kind k) {
        return k == VOID || k == FLOAT || k == LONG || k == DOUBLE || k == REFERENCE || k == WORD;
    }

    /**
     * The stream to use for the generated output.
     */
    public final PrintStream out;

    // end of static declarations

    protected AdviceHook adviceHook;

    public T1XTemplateGenerator(PrintStream out) {
        this.out = out;
    }

    /**
     * Set the advice hook explicitly, which may be necessary when generating individual templates
     * with advice.
     * @param adviceHook
     */
    public void setAdviceHook(AdviceHook hook) {
        adviceHook = hook;
    }

    /**
     * Notify advice hook that a new method is being generated.
     * N.B. This may be a support method for a {@link T1X_TEMPLATE} method.
     */
    public void startMethodGeneration() {
        if (adviceHook != null) {
            adviceHook.startMethodGeneration();
        }
    }

    public void newLine() {
        out.println();
    }

    private StringBuilder tagSB = new StringBuilder();
    private Formatter formatter = new Formatter(tagSB);
    private T1XTemplateTag currentTemplateTag;

    /**
     * Generates and outputs a {@link T1XTemplate} annotation from a format string and associated arguments.
     * Sets {@link #currentTemplateTag} as a side effect to support passing to any {@link #adviceHook}.
     * @param f
     * @param args
     */
    public void generateTemplateTag(String f, Object ... args) {
        tagSB.setLength(0);
        formatter.format(f, args);
        String tagString =  tagSB.toString();
        currentTemplateTag = T1XTemplateTag.valueOf(tagString);
        out.printf("    @T1X_TEMPLATE(%s)%n", tagString);
    }

    private void generateBeforeAdvice(T1XTemplateTag tag, Object ... args) {
        if (adviceHook != null) {
            adviceHook.generate(tag, AdviceType.BEFORE, args);
        }
    }

    public void generateBeforeAdvice(Object ... args) {
        if (adviceHook != null) {
            adviceHook.generate(currentTemplateTag, AdviceType.BEFORE, args);
        }
    }

    public void generateAfterAdvice(Object ... args) {
        if (adviceHook != null) {
            adviceHook.generate(currentTemplateTag, AdviceType.AFTER, args);
        }
    }

    // Here starts the actual template generation methods

    public void generateTraceMethodEntryTemplate() {
        startMethodGeneration();
        generateTemplateTag("%s", TRACE_METHOD_ENTRY);
        out.printf("    public static void traceMethodEntry(String method) {%n");
        out.printf("        Log.println(method);%n");
        generateAfterAdvice();
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> PUTFIELD_TEMPLATE_TAGS = tags("PUTFIELD$");

    /**
     * Generate all the {@link #PUTFIELD_TEMPLATE_TAGS}.
     */
    public void generatePutFieldTemplates() {
        for (Kind k : kinds) {
            if (hasGetPutTemplates(k)) {
                generatePutFieldTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code PUTFIELD} template tag for given type.
     */
    public void generatePutFieldTemplate(Kind k) {
        final int objectSlot = k.stackSlots;
        final String m = k == REFERENCE ? "noninlineW" : "w";
        startMethodGeneration();
        generateTemplateTag("PUTFIELD$%s$resolved", lr(k));
        out.printf("    public static void putfield%s(@Slot(%d) Object object, int offset, @Slot(0) %s value) {%n", ur(k), objectSlot, rs(k));
        generateBeforeAdvice(k);
        out.printf("        TupleAccess.%srite%s(object, offset, %s);%n", m, u(k), fromStackKindCast(k, "value"));
        out.printf("    }%n");
        newLine();

        startMethodGeneration();
        generateTemplateTag("PUTFIELD$%s", lr(k));
        out.printf("    public static void putfield%s(ResolutionGuard.InPool guard, @Slot(%d) Object object, @Slot(0) %s value) {%n", ur(k), objectSlot, rs(k));
        out.printf("        resolveAndPutField%s(guard, object, value);%n", ur(k));
        out.printf("    }%n");
        newLine();

        startMethodGeneration();
        out.printf("    @NEVER_INLINE%n");
        out.printf("    public static void resolveAndPutField%s(ResolutionGuard.InPool guard, Object object, %s value) {%n", ur(k), rs(k));
        out.printf("        FieldActor f = Snippets.resolveInstanceFieldForWriting(guard);%n");
        generateBeforeAdvice(k);
        out.printf("        if (f.isVolatile()) {%n");
        out.printf("            preVolatileWrite();%n");
        out.printf("            TupleAccess.write%s(object, f.offset(), %s);%n", u(k), fromStackKindCast(k, "value"));
        out.printf("            postVolatileWrite();%n");
        out.printf("        } else {%n");
        out.printf("            TupleAccess.write%s(object, f.offset(), %s);%n", u(k), fromStackKindCast(k, "value"));
        out.printf("        }%n");
        out.printf("    }%n");
        out.printf("%n");
    }

    public static final EnumSet<T1XTemplateTag> PUTSTATIC_TEMPLATE_TAGS = tags("PUTSTATIC$");
    /**
    * Generate all the {@link #PUTSTATIC_TEMPLATE_TAGS}.
    */
    public void generatePutStaticTemplates() {
        for (Kind k : kinds) {
            if (hasGetPutTemplates(k)) {
                generatePutStaticTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code PUTSTATIC} template tag for given type.
     * @param k type
     */
    public void generatePutStaticTemplate(Kind k) {
        final String m = k == REFERENCE ? "noninlineW" : "w";
        startMethodGeneration();
        generateTemplateTag("PUTSTATIC$%s$init", lr(k));
        out.printf("    public static void putstatic%s(Object staticTuple, int offset, @Slot(0) %s value) {%n", ur(k), rs(k));
        generateBeforeAdvice(k);
        out.printf("        TupleAccess.%srite%s(staticTuple, offset, %s);%n", m, u(k), fromStackKindCast(k, "value"));
        out.printf("    }%n");
        newLine();

        startMethodGeneration();
        generateTemplateTag("PUTSTATIC$%s", lr(k));
        out.printf("    public static void putstatic%s(ResolutionGuard.InPool guard, @Slot(0) %s value) {%n", ur(k), rs(k));
        out.printf("        resolveAndPutStatic%s(guard, value);%n", ur(k));
        out.printf("    }%n");
        newLine();

        startMethodGeneration();
        out.printf("    @NEVER_INLINE%n");
        out.printf("    public static void resolveAndPutStatic%s(ResolutionGuard.InPool guard, %s value) {%n", ur(k), rs(k));
        out.printf("        FieldActor f = Snippets.resolveStaticFieldForWriting(guard);%n");
        generateBeforeAdvice(k);
        out.printf("        Snippets.makeHolderInitialized(f);%n");
        out.printf("        if (f.isVolatile()) {%n");
        out.printf("            preVolatileWrite();%n");
        out.printf("            TupleAccess.write%s(f.holder().staticTuple(), f.offset(), %s);%n", u(k), fromStackKindCast(k, "value"));
        out.printf("            postVolatileWrite();%n");
        out.printf("        } else {%n");
        out.printf("            TupleAccess.write%s(f.holder().staticTuple(), f.offset(), %s);%n", u(k), fromStackKindCast(k, "value"));
        out.printf("        }%n");
        out.printf("    }%n");
        out.printf("%n");
    }

    public static final EnumSet<T1XTemplateTag> GETFIELD_TEMPLATE_TAGS = tags("GETFIELD$");

    /**
    * Generate all the {@link #GETFIELD_TEMPLATE_TAGS}.
    */
    public void generateGetFieldTemplates() {
        for (Kind k : kinds) {
            if (hasGetPutTemplates(k)) {
                generateGetFieldTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code GETFIELD} template tag for given type.
     * @param k type
     */
    public void generateGetFieldTemplate(Kind k) {
        startMethodGeneration();
        generateTemplateTag("GETFIELD$%s$resolved", lr(k));
        out.printf("    public static %s getfield%s(@Slot(0) Object object, int offset) {%n", rs(k), u(k));
        generateBeforeAdvice(k);
        out.printf("        %s result = TupleAccess.read%s(object, offset);%n", j(k), u(k));
        out.printf("        return %s;%n", toStackKindCast(k, "result"));
        out.printf("    }%n");
        newLine();

        startMethodGeneration();
        generateTemplateTag("GETFIELD$%s", lr(k));
        out.printf("    public static %s getfield%s(ResolutionGuard.InPool guard, @Slot(0) Object object) {%n", rs(k), ur(k));
        out.printf("        return resolveAndGetField%s(guard, object);%n", ur(k), u(k));
        out.printf("    }%n");
        newLine();

        startMethodGeneration();
        out.printf("    @NEVER_INLINE%n");
        out.printf("    public static %s resolveAndGetField%s(ResolutionGuard.InPool guard, Object object) {%n", rs(k), ur(k));
        out.printf("        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);%n");
        generateBeforeAdvice(k);
        out.printf("        if (f.isVolatile()) {%n");
        out.printf("            preVolatileRead();%n");
        out.printf("            %s value = TupleAccess.read%s(object, f.offset());%n", j(k), u(k));
        out.printf("            postVolatileRead();%n");
        out.printf("            return %s;%n", toStackKindCast(k, "value"));
        out.printf("        } else {%n");
        out.printf("            %s result = TupleAccess.read%s(object, f.offset());%n", j(k), u(k));
        out.printf("            return %s;%n", toStackKindCast(k, "result"));
        out.printf("        }%n");
        out.printf("    }%n");
        out.printf("%n");
    }

    public static final EnumSet<T1XTemplateTag> GETSTATIC_TEMPLATE_TAGS = tags("GETSTATIC$");

    /**
    * Generate all the {@link #GETSTATIC_TEMPLATE_TAGS}.
    */
    public void generateGetStaticTemplates() {
        for (Kind k : kinds) {
            if (hasGetPutTemplates(k)) {
                generateGetStaticTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code GETFIELD} template tag for given type.
     * @param k type
     */
    public void generateGetStaticTemplate(Kind k) {
        startMethodGeneration();
        generateTemplateTag("GETSTATIC$%s", lr(k));
        out.printf("    public static %s getstatic%s(ResolutionGuard.InPool guard) {%n", rs(k), ur(k));
        out.printf("        return resolveAndGetStatic%s(guard);%n", ur(k), u(k));
        out.printf("    }%n");
        newLine();

        startMethodGeneration();
        out.printf("    @NEVER_INLINE%n");
        out.printf("    public static %s resolveAndGetStatic%s(ResolutionGuard.InPool guard) {%n", rs(k), ur(k));
        out.printf("        FieldActor f = Snippets.resolveStaticFieldForReading(guard);%n");
        out.printf("        Snippets.makeHolderInitialized(f);%n");
        generateBeforeAdvice(k);
        out.printf("        if (f.isVolatile()) {%n");
        out.printf("            preVolatileRead();%n");
        out.printf("            %s value = TupleAccess.read%s(f.holder().staticTuple(), f.offset());%n", j(k), u(k));
        out.printf("            postVolatileRead();%n");
        out.printf("            return %s;%n", toStackKindCast(k, "value"));
        out.printf("        } else {%n");
        out.printf("            %s result = TupleAccess.read%s(f.holder().staticTuple(), f.offset());%n", j(k), u(k));
        out.printf("            return %s;%n", toStackKindCast(k, "result"));
        out.printf("        }%n");
        out.printf("    }%n");
        out.printf("%n");

        startMethodGeneration();
        generateTemplateTag("GETSTATIC$%s$init", lr(k));
        out.printf("    public static %s getstatic%s(Object staticTuple, int offset) {%n", rs(k), u(k));
        generateBeforeAdvice(k);
        out.printf("        %s result = TupleAccess.read%s(staticTuple, offset);%n", j(k), u(k));
        out.printf("        return %s;%n", toStackKindCast(k, "result"));
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> ALOAD_TEMPLATE_TAGS = EnumSet.of(IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD);

    /**
     * Generate all the {@link #ALOAD_TEMPLATE_TAGS}.
     */
    public void generateArrayLoadTemplates() {
        for (Kind k : kinds) {
            if (hasArrayTemplates(k)) {
                generateArrayLoadTemplate(k);
            }
        }
    }

    /**
     * Generate the {@code ALOAD} template(s) for given type.
     */
    public void generateArrayLoadTemplate(Kind k) {
        startMethodGeneration();
        generateTemplateTag("%sALOAD", tagPrefix(k));
        out.printf("    public static %s %saload(@Slot(1) Object array, @Slot(0) int index) {%n", rs(k), opPrefix(k));
        out.printf("        ArrayAccess.checkIndex(array, index);%n");
        generateBeforeAdvice(k);
        out.printf("        %s result = ArrayAccess.get%s(array, index);%n", j(k), u(k));
        out.printf("        return %s;%n", toStackKindCast(k, "result"));
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> ASTORE_TEMPLATE_TAGS = EnumSet.of(IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE);

    /**
     * Generate all the {@link #ASTORE_TEMPLATE_TAGS}.
     */
    public void generateArrayStoreTemplates() {
        for (Kind k : kinds) {
            if (hasArrayTemplates(k)) {
                generateArrayStoreTemplate(k);
            }
        }
    }

    /**
     * Generate the {@code ALOAD} template(s) for given type.
     * @param k type
     */
    public void generateArrayStoreTemplate(Kind k) {
        final int arraySlot = k.stackSlots + 1;
        final int indexSlot = k.stackSlots;
        startMethodGeneration();
        generateTemplateTag("%sASTORE", tagPrefix(k));
        out.printf("    public static void %sastore(@Slot(%d) Object array, @Slot(%d) int index, @Slot(0) %s value) {%n", opPrefix(k), arraySlot, indexSlot, rs(k));
        out.printf("        ArrayAccess.checkIndex(array, index);%n");
        generateBeforeAdvice(k);
        if (k == REFERENCE) {
            out.printf("        ArrayAccess.checkSetObject(array, value);%n");
        }
        out.printf("        ArrayAccess.set%s(array, index, %s);%n", u(k), fromStackKindCast(k, "value"));
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> NEW_TEMPLATE_TAGS = EnumSet.of(NEW, NEW$init);

    /**
     * Generate all the {@link #NEW_TEMPLATE_TAGS}.
     */
    public void generateNewTemplates() {
        generateNewTemplate(T1XTemplateTag.NEW);
        generateNewTemplate(T1XTemplateTag.NEW$init);
        generateNewTemplate(T1XTemplateTag.NEW_HYBRID);
    }

    /**
     * Generate the requested {@code NEW} template.
     */
    public void generateNewTemplate(T1XTemplateTag tag) {
        if (tag == T1XTemplateTag.NEW) {
            startMethodGeneration();
            generateTemplateTag(tag.name());
            out.printf("    public static Object new_(ResolutionGuard guard) {%n");
            out.printf("        Object object = resolveClassForNewAndCreate(guard);%n");
            generateAfterAdvice(NULL_ARGS);
            out.printf("        return object;%n");
            out.printf("    }%n");
            newLine();
        } else {
            startMethodGeneration();
            generateTemplateTag(tag.name());
            out.printf("    public static Object %s(DynamicHub hub) {%n", tag == T1XTemplateTag.NEW$init ? "new_" : "new_hybrid");
            out.printf("        Object object = Heap.create%s(hub);%n", tag == T1XTemplateTag.NEW$init ? "Tuple" : "Hybrid");
            generateAfterAdvice(NULL_ARGS);
            out.printf("        return object;%n");
            out.printf("    }%n");
            newLine();
        }
    }

    public static final EnumSet<T1XTemplateTag> NEWARRAY_TEMPLATE_TAGS = EnumSet.of(NEWARRAY);

    public void generateNewArrayTemplate() {
        startMethodGeneration();
        generateTemplateTag("NEWARRAY");
        out.printf("    public static Object newarray(ClassActor arrayClass, @Slot(0) int length) {%n");
        out.printf("        Object array = Snippets.createArray(arrayClass, length);%n");
        generateAfterAdvice(NULL_ARGS);
        out.printf("        return array;%n");
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> ANEWARRAY_TEMPLATE_TAGS = EnumSet.of(ANEWARRAY, ANEWARRAY$resolved);

    /**
     * Generate all the {@link #NEWARRAY_TEMPLATE_TAGS}.
     */
    public void generateANewArrayTemplates() {
        generateANewArrayTemplate("");
        generateANewArrayTemplate("resolved");
    }

    /**
     * Generate the requested {@code ANEWARRAY} template.
     * @param resolved if "" generate {@code ANEWARRAY} template, else {@code ANEWARRAY$resolved}.
     */
    public void generateANewArrayTemplate(String resolved) {
        String t;
        String v;
        if (resolved.equals("")) {
            t = "ResolutionGuard";
            v = "arrayType";
        } else {
            t = "ArrayClassActor<?>";
            v = "arrayType";
        }
        startMethodGeneration();
        generateTemplateTag("ANEWARRAY%s", prefixDollar(resolved));
        out.printf("    public static Object anewarray(%s %s, @Slot(0) int length) {%n", t, v);
        if (resolved.equals("")) {
            out.printf("        ArrayClassActor<?> arrayClassActor = UnsafeCast.asArrayClassActor(Snippets.resolveArrayClass(arrayType));%n");
        } else {
            out.printf("        ArrayClassActor<?> arrayClassActor = arrayType;%n");
        }
        out.printf("        Object array = Snippets.createArray(arrayClassActor, length);%n");
        generateAfterAdvice(NULL_ARGS);
        out.printf("        return array;%n");
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> MULTIANEWARRAY_TEMPLATE_TAGS = EnumSet.of(MULTIANEWARRAY, MULTIANEWARRAY$resolved);

    /**
     * Generate all the {@link #MULTINEWARRAY_TEMPLATE_TAGS}.
     */
    public void generateMultiANewArrayTemplates() {
        generateMultiANewArrayTemplate("");
        generateMultiANewArrayTemplate("resolved");
    }

    /**
     * Generate the requested {@code MULTIANEWARRAY} template.
     * @param resolved if "" generate {@code MULTIANEWARRAY} template, else {@code MULTIANEWARRAY$resolved}.
     */
    public void generateMultiANewArrayTemplate(String resolved) {
        String t;
        String v;
        if (resolved.equals("")) {
            t = "ResolutionGuard";
            v = "guard";
        } else {
            t = "ArrayClassActor<?>";
            v = "arrayClassActor";
        }
        startMethodGeneration();
        generateTemplateTag("MULTIANEWARRAY%s", prefixDollar(resolved));
        out.printf("    public static Reference multianewarray(%s %s, int[] lengths) {%n", t, v);
        if (resolved.equals("")) {
            out.printf("        ClassActor arrayClassActor = Snippets.resolveClass(guard);%n");
        }
        out.printf("        Object array = Snippets.createMultiReferenceArray(arrayClassActor, lengths);%n");
        generateAfterAdvice(NULL_ARGS);
        out.printf("        return Reference.fromJava(array);%n");
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> CHECKCAST_TEMPLATE_TAGS = EnumSet.of(CHECKCAST, CHECKCAST$resolved);

    /**
     * Generate all the {@link #CHECKCAST_TEMPLATE_TAGS}.
     */
    public void generateCheckcastTemplates() {
        generateCheckcastTemplate("");
        generateCheckcastTemplate("resolved");
        generateResolveAndCheckCast();
    }

    /**
     * Generate the requested {@code CHECKCAST} template.
     * @param resolved if "" generate {@code CHECKCAST} template, else {@code CHECKCAST$resolved}.
     */
    public void generateCheckcastTemplate(String resolved) {
        String t;
        String m;
        boolean isResolved = resolved.equals("resolved");
        String arg = isResolved ? "classActor" : "guard";

        if (!isResolved) {
            t = "ResolutionGuard";
            m = "resolveAndCheckcast";
        } else {
            t = "ClassActor";
            m = "Snippets.checkCast";
        }
        startMethodGeneration();
        generateTemplateTag("CHECKCAST%s", prefixDollar(resolved));
        out.printf("    public static Object checkcast(%s %s, @Slot(0) Object object) {%n", t, arg);
        if (isResolved) {
            generateBeforeAdvice(NULL_ARGS);
        }
        out.printf("        %s(%s, object);%n", m, arg);
        out.printf("        return object;%n", m, arg);
        out.printf("    }%n");
        newLine();
    }

    public void generateResolveAndCheckCast() {
        startMethodGeneration();
        out.printf("    @NEVER_INLINE%n");
        out.printf("    private static void resolveAndCheckcast(ResolutionGuard guard, final Object object) {%n");
        out.printf("        ClassActor classActor = Snippets.resolveClass(guard);%n");
        generateBeforeAdvice();
        out.printf("        Snippets.checkCast(classActor, object);%n");
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> INSTANCEOF_TEMPLATE_TAGS = EnumSet.of(INSTANCEOF, INSTANCEOF$resolved);

    /**
     * Generate all the {@link #INSTANCEOF_TEMPLATE_TAGS}.
     */
    public void generateInstanceofTemplates() {
        generateInstanceofTemplate("");
        generateInstanceofTemplate("resolved");
    }

    /**
     * Generate the requested {@code INSTANCEOF} template.
     * @param resolved if "" generate {@code INSTANCEOF} template, else {@code INSTANCEOF$resolved}.
     */
    public void generateInstanceofTemplate(String resolved) {
        String t;
        String v;
        if (resolved.equals("")) {
            t = "ResolutionGuard";
            v = "guard";
        } else {
            t = "ClassActor";
            v = "classActor";
        }
        startMethodGeneration();
        generateTemplateTag("INSTANCEOF%s", prefixDollar(resolved));
        out.printf("    public static int instanceof_(%s %s, @Slot(0) Object object) {%n", t, v);
        if (resolved.equals("")) {
            out.printf("        ClassActor classActor = Snippets.resolveClass(guard);%n");
        }
        generateBeforeAdvice(NULL_ARGS);
        out.printf("        return UnsafeCast.asByte(Snippets.instanceOf(classActor, object));%n");
        out.printf("    }%n");
        newLine();
    }

    public void generateArraylengthTemplate() {
        startMethodGeneration();
        generateTemplateTag("ARRAYLENGTH");
        out.printf("    public static int arraylength(@Slot(0) Object array) {%n");
        out.printf("        int length = ArrayAccess.readArrayLength(array);%n");
        generateBeforeAdvice(NULL_ARGS);
        out.printf("        return length;%n");
        out.printf("    }%n");
        newLine();
    }

    public void generateAThrowTemplate() {
        startMethodGeneration();
        generateTemplateTag("ATHROW");
        out.printf("    public static void athrow(@Slot(0) Object object) {%n");
        generateBeforeAdvice(NULL_ARGS);
        out.printf("        Throw.raise(object);%n");
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> MONITOR_TEMPLATE_TAGS = EnumSet.of(MONITORENTER, MONITOREXIT);

    /**
     * Generate all the {@link #MONITOR_TEMPLATE_TAGS}.
     */
    public  void generateMonitorTemplates() {
        generateMonitorTemplate("enter");
        generateMonitorTemplate("exit");
    }

    /**
     * Generate the requested {@code MONITOR} template.
     * @param tag one of "enter" or "exit":
     */
    public void generateMonitorTemplate(String tag) {
        startMethodGeneration();
        generateTemplateTag("MONITOR%s", tag.toUpperCase());
        out.printf("    public static void monitor%s(@Slot(0) Object object) {%n", tag);
        generateBeforeAdvice(NULL_ARGS);
        out.printf("        Monitor.%s(object);%n", tag);
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> LOCK_TEMPLATE_TAGS = EnumSet.of(LOCK, UNLOCK);

    public void generateLockTemplates() {
        for (T1XTemplateTag tag : LOCK_TEMPLATE_TAGS) {
            generateLockTemplate(tag);
        }
    }

    public void generateLockTemplate(T1XTemplateTag tag) {
        startMethodGeneration();
        generateTemplateTag("%s", tag);
        out.printf("    public static void %s(Object object) {%n", tag.name().toLowerCase());
        generateBeforeAdvice();
        out.printf("        Monitor.%s(object);%n", tag == LOCK ? "enter" : "exit");
        out.printf("    }%n");
        newLine();
    }

    private static EnumSet<T1XTemplateTag> tags(String prefix) {
        EnumSet<T1XTemplateTag> tags = EnumSet.noneOf(T1XTemplateTag.class);
        for (T1XTemplateTag tag : T1XTemplateTag.class.getEnumConstants()) {
            if (tag.name().startsWith(prefix)) {
                tags.add(tag);
            }
        }
        return tags;
    }

    public static final EnumSet<T1XTemplateTag> INVOKE_VIRTUAL_TEMPLATE_TAGS = tags("INVOKEVIRTUAL$");

    public static final EnumSet<T1XTemplateTag> INVOKE_INTERFACE_TEMPLATE_TAGS = tags("INVOKEINTERFACE$");

    /**
     * Generate all the {@link #INVOKE_VIRTUAL_TEMPLATE_TAGS}.
     */
    public void generateInvokeVirtualTemplates() {
        for (Kind k : kinds) {
            if (hasInvokeTemplates(k)) {
                generateUnresolvedInvokeVITemplate(k, "virtual");
                generateInvokeVITemplate(k, "virtual", false);
                generateInvokeVITemplate(k, "virtual", true);
            }
        }
    }

    /**
     * Generate all the {@link #INVOKE_INTERFACE_TEMPLATE_TAGS}.
     */
    public void generateInvokeInterfaceTemplates() {
        for (Kind k : kinds) {
            if (hasInvokeTemplates(k)) {
                generateUnresolvedInvokeVITemplate(k, "interface");
                generateInvokeVITemplate(k, "interface", false);
                generateInvokeVITemplate(k, "interface", true);
            }
        }
    }

    /**
     * Generate a specific {@code INVOKE} template.
     * @param k type
     * @param variant one of "virtual" or "interface"
     */
    public void generateUnresolvedInvokeVITemplate(Kind k, String variant) {
        startMethodGeneration();
        out.printf("    /**%n");
        out.printf("     * Resolves and selects the correct implementation of a method referenced by an INVOKE%s instruction.%n", variant.toUpperCase());
        out.printf("     *%n");
        out.printf("     * @param guard guard for a method symbol%n");
        out.printf("     * @param receiver the receiver object of the invocation%n");
        out.printf("     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called%n");
        out.printf("     */%n");
        generateTemplateTag("INVOKE%s$%s", variant.toUpperCase(), lr(k));
        out.printf("    @Slot(-1)%n");
        out.printf("    public static Address invoke%s%s(ResolutionGuard.InPool guard, Reference receiver) {%n", variant, u(k));
        generateBeforeAdvice(k, variant);
        if (variant.equals("interface")) {
            out.printf("        return resolveAndSelectInterfaceMethod(guard, receiver);%n");
        } else {
            out.printf("        return resolveAndSelectVirtualMethod(receiver, guard);%n");
        }
        out.printf("    }%n");
        newLine();
    }

    /**
     * Generate a specific {@code INVOKE} template.
     * @param k type
     * @param variant one of "virtual" or "interface"
     */
    public void generateInvokeVITemplate(Kind k, String variant, boolean instrumented) {
        String params = variant.equals("interface") ? "InterfaceMethodActor methodActor" : "int vTableIndex";
        if (instrumented) {
            params += ", MethodProfile mpo, int mpoIndex";
        }
        startMethodGeneration();
        out.printf("    /**%n");
        out.printf("     * Selects the correct implementation of a resolved method referenced by an INVOKE%s instruction.%n", variant.toUpperCase());
        out.printf("     *%n");
        if (variant.equals("interface")) {
            out.printf("     * @param methodActor the resolved interface method being invoked%n");
        } else {
            out.printf("     * @param vTableIndex the index into the vtable of the virtual method being invoked%n");
        }
        if (instrumented) {
            out.printf("     * @param mpo the profile object for an instrumented invocation%n");
            out.printf("     * @param mpoIndex a profile specific index%n");
        }
        out.printf("     * @param receiver the receiver object of the invocation%n");
        out.printf("     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be called%n");
        out.printf("     */%n");
        generateTemplateTag("INVOKE%s$%s%s", variant.toUpperCase(), lr(k), instrumented ? "$instrumented" : "$resolved");
        out.printf("    @Slot(-1)%n");
        out.printf("    public static Address invoke%s%s(%s, Reference receiver) {%n", variant, u(k), params);
        generateBeforeAdvice(k, variant);
        if (variant.equals("interface")) {
            if (!instrumented) {
                out.printf("        return Snippets.selectInterfaceMethod(receiver, methodActor).asAddress().%n");
            } else {
                out.printf("        return Snippets.selectInterfaceMethod(receiver, methodActor, mpo, mpoIndex).%n");
            }
        } else {
            if (!instrumented) {
                out.printf("        return ObjectAccess.readHub(receiver).getWord(vTableIndex).asAddress().%n");
            } else {
                out.printf("        return selectVirtualMethod(receiver, vTableIndex, mpo, mpoIndex).%n");
            }
        }
        out.printf("            plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());%n");
        out.printf("    }%n");
        newLine();
    }


    public static final EnumSet<T1XTemplateTag> INVOKE_STATIC_TEMPLATE_TAGS = tags("INVOKESPECIAL$");

    public static final EnumSet<T1XTemplateTag> INVOKE_SPECIAL_TEMPLATE_TAGS = tags("INVOKESTATIC$");


    /**
     * Generate all the {@link #INVOKE_STATIC_TEMPLATE_TAGS}.
     */
    public void generateInvokeStaticTemplates() {
        for (Kind k : kinds) {
            if (hasInvokeTemplates(k)) {
                generateInvokeSSTemplate(k, "static");
            }
        }
    }

    /**
     * Generate all the {@link #INVOKE_SPECIAL_TEMPLATE_TAGS}.
     */
    public void generateInvokeSpecialTemplates() {
        for (Kind k : kinds) {
            if (hasInvokeTemplates(k)) {
                generateInvokeSSTemplate(k, "special");
            }
        }
    }

    /**
     * Generate a specific {@code INVOKE} template.
     * @param k type
     * @param variant one of "special" or "static"
     * @param xtag one of "" or "init" or "resolved"
     */
    public void generateInvokeSSTemplate(Kind k, String variant) {
        String params = "ResolutionGuard.InPool guard";
        if (variant.equals("special")) {
            params += ", Reference receiver";
        }
        startMethodGeneration();
        out.printf("    /**%n");
        out.printf("     * Resolves a method referenced by an INVOKE%s instruction.%n", variant.toUpperCase());
        out.printf("     *%n");
        out.printf("     * @param guard guard for a method symbol%n");
        out.printf("     * @param receiver the receiver object of the invocation%n");
        out.printf("     * @return the {@link CallEntryPoint#BASELINE_ENTRY_POINT} to be invoked%n");
        out.printf("     */%n");
        generateTemplateTag("INVOKE%s$%s", variant.toUpperCase(), lr(k));
        out.printf("    @Slot(-1)%n");
        out.printf("    public static Address invoke%s%s(%s) {%n", variant, u(k), params);
        if (variant.equals("special")) {
            out.printf("        nullCheck(receiver.toOrigin());%n");
        }
        generateBeforeAdvice(k, variant);
        out.printf("        return resolve%sMethod(guard);%n", toFirstUpper(variant));
        out.printf("    }%n");
        newLine();
    }


    public static final EnumSet<T1XTemplateTag> I2_TEMPLATE_TAGS = EnumSet.of(I2L, I2F, I2D);

    /**
     * Generate all the {@link #I2_TEMPLATE_TAGS}.
     */
    public void generateI2Templates() {
        for (Kind k : kinds) {
            if (hasI2Templates(k)) {
                generateI2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code I2} template.
     * @param k target type
     */
    public void generateI2Template(Kind k) {
        startMethodGeneration();
        generateTemplateTag("I2%c", u(k).charAt(0));
        out.printf("    public static %s i2%s(@Slot(0) int value) {%n", rs(k), opPrefix(k));
        String cast = k == CHAR || k == BYTE || k == SHORT ? "(" + k + ") " : "";
        generateBeforeAdvice(k);
        out.printf("        return %svalue;%n", cast);
        out.printf("    }%n");
        newLine();
    }


    public static final EnumSet<T1XTemplateTag> L2_TEMPLATE_TAGS = EnumSet.of(L2I, L2F, L2D);

    /**
     * Generate all the {@link #L2_TEMPLATE_TAGS}.
     */
    public void generateL2Templates() {
        for (Kind k : kinds) {
            if (hasL2Templates(k)) {
                generateL2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code L2} template.
     * @param k target type
     */
    public void generateL2Template(Kind k) {
        startMethodGeneration();
        generateTemplateTag("L2%c", u(k).charAt(0));
        out.printf("    public static %s l2%s(@Slot(0) long value) {%n", rs(k), opPrefix(k));
        generateBeforeAdvice(k);
        String cast = k == INT ? "(int) " : "";
        out.printf("        return %svalue;%n", cast);
        out.printf("    }%n");
        newLine();
    }


    public static final EnumSet<T1XTemplateTag> D2_TEMPLATE_TAGS = EnumSet.of(D2I, D2L, D2F);

    /**
     * Generate all the {@link #D2_TEMPLATE_TAGS}.
     */
    public void generateD2Templates() {
        for (Kind k : kinds) {
            if (hasD2Templates(k)) {
                generateD2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code D2} template.
     * @param k target type
     */
    public void generateD2Template(Kind k) {
        startMethodGeneration();
        generateTemplateTag("D2%c", u(k).charAt(0));
        out.printf("    public static %s d2%s(@Slot(0) double value) {%n", rs(k), opPrefix(k));
        generateBeforeAdvice(k);
        String arg2 = k == FLOAT ? "(float) value" : "T1XRuntime.d2" + opPrefix(k) + "(value)";
        out.printf("        return %s;%n", arg2);
        out.printf("    }%n");
        newLine();
    }


    public static final EnumSet<T1XTemplateTag> F2_TEMPLATE_TAGS = EnumSet.of(F2I, F2L, F2D);

    /**
     * Generate all the {@link #F2_TEMPLATE_TAGS}.
     */
    public void generateF2Templates() {
        for (Kind k : kinds) {
            if (hasF2Templates(k)) {
                generateF2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code F2} template.
     * @param k target type
     */
    public void generateF2Template(Kind k) {
        startMethodGeneration();
        generateTemplateTag("F2%c", u(k).charAt(0));
        out.printf("    public static %s f2%s(@Slot(0) float value) {%n", rs(k), opPrefix(k));
        generateBeforeAdvice(k);
        String arg2 = k == DOUBLE ? "value" : "T1XRuntime.f2" + opPrefix(k) + "(value)";
        out.printf("        return %s;%n", arg2);
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> NEG_TEMPLATE_TAGS = EnumSet.of(INEG, LNEG, FNEG, DNEG);

    /**
     * Generate all {@link #NEG_TEMPLATE_TAGS}.
     */
    public void generateNegTemplates() {
        for (Kind k : kinds) {
            if (hasArithTemplates(k)) {
                generateNegTemplate(k);
            }
        }
    }

    /**
     * Generate specific {@code NEG} template.
     * @param k type
     */
    public void generateNegTemplate(Kind k) {
        final String op = "neg";
        startMethodGeneration();
        generateTemplateTag("%s%s", tagPrefix(k), op.toUpperCase());
        out.printf("    public static %s %s%s(@Slot(0) %s value, %s zero) {%n", k, opPrefix(k), op, k, k);
        generateBeforeAdvice(k);
        if (k == DOUBLE) {
            out.printf("        double res;%n");
            out.printf("        if (Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(zero)) {%n");
            out.printf("            res = -0.0d;%n");
            out.printf("        } else {%n");
            out.printf("            res = zero - value;%n");
            out.printf("        }%n");
            out.printf("        return res;%n");
        } else if (k == FLOAT) {
            out.printf("        float res;%n");
            out.printf("        if (Float.floatToRawIntBits(value) == Float.floatToRawIntBits(zero)) {%n");
            out.printf("            res = -0.0f;%n");
            out.printf("        } else {%n");
            out.printf("            res = zero - value;%n");
            out.printf("        }%n");
            out.printf("        return res;%n");
        } else {
            out.printf("        return zero - value;%n");
        }
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> DYADIC_TEMPLATE_TAGS = EnumSet.of(IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, ISHL, LSHL, ISHR,
                    LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR);

    /**
     * Generate all the {@link #DYADIC_TEMPLATE_TAGS).
     */
    public void generateDyadicTemplates() {
        for (Kind k : kinds) {
            if (hasArithTemplates(k)) {
                for (String op : new String[] {"add", "sub", "mul", "div", "rem"}) {
                    generateDyadicTemplate(k, op);
                }
            }
            if (hasLogTemplates(k)) {
                for (String op : new String[] {"or", "and", "xor"}) {
                    generateDyadicTemplate(k, op);
                }
                for (String op : new String[] {"shl", "shr", "ushr"}) {
                    generateDyadicTemplate(k, op);
                }
            }
        }
    }

    /**
     * Generate a specific dyadic operation template.
     * @param k type
     * @param op one of "add", "sub", "mul", "div", "rem", "or", "and", "xor", "shl", "shr", "ushr"
     */
    public void generateDyadicTemplate(Kind k, String op) {
        final boolean arg2IsInt = isShift(op);
        final int arg1Slot = isShift(op) || k.stackSlots == 1 ? 1 : 2;
        startMethodGeneration();
        generateTemplateTag("%s%s", tagPrefix(k), op.toUpperCase());
        out.printf("    public static %s %s%s(@Slot(%d) %s value1, @Slot(0) %s value2) {%n", s(k), opPrefix(k), op, arg1Slot, s(k), arg2IsInt ? "int" : s(k));
        generateBeforeAdvice(k);
        out.printf("        return value1 %s value2;%n", algOp(op));
        out.printf("    }%n");
        newLine();
    }

    /**
     * Generate a specific {@code RETURN} template.
     * @param k type
     * @param unlock one of "", {@link #lockVariants} or "registerFinalizer"
     */
    public void generateReturnTemplate(Kind k, String unlock) {
        // Admittedly, the readability goal is a stretch here!
        final String arg1 = unlock.equals("") ? "" : "Reference object";
        final String arg2 = k == VOID ? "" : "@Slot(0) " + rs(k) + " value";
        final String sep = !arg1.isEmpty() && !arg2.isEmpty() ? ", " : "";
        startMethodGeneration();
        generateTemplateTag("%sRETURN%s", tagPrefix(k), prefixDollar(unlock));
        out.printf("    @Slot(-1)%n");
        out.printf("    public static %s %sreturn%s(%s%s%s) {%n", rs(k), opPrefix(k), toFirstUpper(unlock), arg1, sep, arg2);
        if (unlock.equals("registerFinalizer")) {
            out.printf("        if (ObjectAccess.readClassActor(object).hasFinalizer()) {%n");
            out.printf("            SpecialReferenceManager.registerFinalizee(object);%n");
            out.printf("        }%n");
        } else {
            if (unlock.length() > 0) {
                // need to advise the implicit monitorexit
                generateBeforeAdvice(MONITOREXIT);
                out.printf("        Monitor.noninlineExit(object);%n");
            }
            if (k != VOID) {
                generateBeforeAdvice(k);
                out.printf("        return value;%n");
            } else {
                generateBeforeAdvice(k);
            }
        }
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> CMP_TEMPLATE_TAGS = EnumSet.of(LCMP, FCMPL, FCMPG, DCMPL, DCMPG);

    /**
     * Generate all the {@link #CMP_TEMPLATE_TAGS}.
     */
    public void generateCmpTemplates() {
        generateCmpTemplate(LONG, "LCMP");
        generateCmpTemplate(FLOAT, "FCMPL");
        generateCmpTemplate(FLOAT, "FCMPG");
        generateCmpTemplate(DOUBLE, "DCMPL");
        generateCmpTemplate(DOUBLE, "DCMPG");
    }

    /**
     * Generate a specific {@link #CMP_TEMPLATE_TAGS} template.
     * @param k type one of "long", "float" or "double"
     * @param variant "g" or "l"
     */
    public void generateCmpTemplate(Kind k, String opcode) {
        startMethodGeneration();
        generateTemplateTag(opcode);
        out.printf("    public static int %s(@Slot(%d) %s value1, @Slot(0) %s value2) {%n", opcode.toLowerCase(), k.stackSlots, s(k), s(k));
        out.printf("        int result = rawCompare(Bytecodes.%s, value1, value2);%n", opcode);
        generateBeforeAdvice(k);
        out.printf("        return result;%n");
        out.printf("    }%n");
        newLine();
    }

    /**
     * Generate the complete template source code using the provided {@link AdviceHook} to the standard output.
     * The order of the output is first the typed operations, per type, followed by the untyped operations.
     * @param hook the advice hook or {@code null} if no advice
     */
    public void generateAll(AdviceHook hook) {
        adviceHook = hook;
        for (Kind k : kinds) {
            if (hasGetPutTemplates(k)) {
                generateGetFieldTemplate(k);
                generateGetStaticTemplate(k);
                generatePutFieldTemplate(k);
                generatePutStaticTemplate(k);
            }
            if (hasI2Templates(k)) {
                generateI2Template(k);
            }
            if (hasL2Templates(k)) {
                generateL2Template(k);
            }
            if (hasF2Templates(k)) {
                generateF2Template(k);
            }
            if (hasD2Templates(k)) {
                generateD2Template(k);
            }
            if (hasArithTemplates(k)) {
                for (String op : new String[] {"add", "sub", "mul", "div", "rem"}) {
                    generateDyadicTemplate(k, op);
                }
                generateNegTemplate(k);
            }
            if (hasLogTemplates(k)) {
                for (String op : new String[] {"or", "and", "xor"}) {
                    generateDyadicTemplate(k, op);
                }
                for (String op : new String[] {"shl", "shr", "ushr"}) {
                    generateDyadicTemplate(k, op);
                }
            }
            if (hasReturnTemplates(k)) {
                generateReturnTemplate(k, "");
                generateReturnTemplate(k, "unlock");
            }
            if (hasArrayTemplates(k)) {
                generateArrayLoadTemplate(k);
                generateArrayStoreTemplate(k);
            }
            if (hasInvokeTemplates(k))  {
                for (String s : new String[] {"virtual", "interface"}) {
                    generateUnresolvedInvokeVITemplate(k, s);
                    generateInvokeVITemplate(k, s, false);
                    generateInvokeVITemplate(k, s, true);
                }
                for (String s : new String[] {"special", "static"}) {
                    generateInvokeSSTemplate(k, s);
                }
            }
        }
        // Special cases
        generateCmpTemplates();
        generateNewTemplates();
        generateNewArrayTemplate();
        generateANewArrayTemplates();
        generateMultiANewArrayTemplates();
        generateCheckcastTemplates();
        generateArraylengthTemplate();
        generateAThrowTemplate();
        generateMonitorTemplates();
        generateInstanceofTemplates();
        generateReturnTemplate(VOID, "registerFinalizer");
        generateLockTemplates();
        generateTraceMethodEntryTemplate();
    }

    public static void main(String[] args) throws Exception {
        if (generate(false, T1XTemplateSource.class)) {
            System.out.println("Source for " + T1XTemplateSource.class + " was updated");
            System.exit(1);
        }
    }

    /**
     * Inserts or updates generated source into {@code target}. The generated source is derived from
     * {@code source} and is delineated in {@code target} by the following lines:
     * <pre>
     * // START GENERATED CODE
     *
     * ...
     *
     * // END GENERATED CODE
     * </pre>

     *
     * @param checkOnly if {@code true}, then {@code target} is not updated; the value returned by this method indicates
     *            whether it would have been updated were this argument {@code true}
     * @return {@code true} if {@code target} was modified (or would have been if {@code checkOnly} was {@code false}); {@code false} otherwise
     */
    static boolean generate(boolean checkOnly, Class target) throws Exception {
        File base = new File(JavaProject.findWorkspaceDirectory(), "com.oracle.max.vm.ext.t1x/src");
        File outputFile = new File(base, target.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        new T1XTemplateGenerator(out).generateAll(null);
        ReadableSource content = ReadableSource.Static.fromString(baos.toString());
        return Files.updateGeneratedContent(outputFile, content, "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
    }


}

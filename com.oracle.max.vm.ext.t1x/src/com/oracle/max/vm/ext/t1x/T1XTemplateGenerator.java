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
import static com.sun.cri.ci.CiKind.*;

import java.io.*;
import java.util.*;

import com.sun.cri.ci.*;
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
    public static final CiKind[] kinds = {Boolean, Byte, Char, Short, Int, Float, Long, Double, Object, Word, Void};

    private static final String[] conditions = new String[] {"eq", "ne", "lt", "ge", "gt", "le"};

    /**
     * Returns {@code k.javaName} with first character lower cased.
     */
    public static String l(CiKind k) {
        if (k == Object) {
            return "object";
        } else if (k == Word) {
            return "word";
        }
        return k.javaName;
    }

    /**
     * Returns {@code k.javaName} with first character upper cased.
     */
    public static String u(CiKind k) {
        return k.name();
    }

    /**
     * Returns {@code k} except with "Object" replaced by "Reference".
     */
    public static String r(CiKind k) {
        return k == Object ? "Reference" : k.toString();
    }

    /**
     * Returns {@code k.stackKind().javaName}.
     */
    public static String s(CiKind k) {
        return k.stackKind().javaName;
    }

    /**
     * Returns {@code k.stackKind().javaName} except with "Object" replaced by "Reference".
     */
    public static String rs(CiKind k) {
        return k == Object ? "Reference" : k.stackKind().toString();
    }

    /**
     * Returns {@code k.javaName} with first letter upper-cased and "Object" replaced by "Reference".
     */
    public static String ur(CiKind k) {
        return k == Object ? "Reference" : u(k);
    }

    /**
     * Returns {@code k.javaName} with first letter lower-cased and "object" replaced by "reference".
     */
    public static String lr(CiKind k) {
        return k == Object ? "reference" : l(k);
    }

    /**
     * Returns {@code k.javaName} all upper-cased and "OBJECT" replaced by "REFERENCE".
     */
    public static String au(CiKind k) {
        return k == Object ? "REFERENCE" : k.javaName.toUpperCase();
    }

    public static String toStackKindCast(CiKind k, String var) {
        if (k == Boolean) {
            return "UnsafeCast.asByte(" + var + ")";
        } else if (k == Object) {
            return "Reference.fromJava(" + var + ")";
        } else {
            return var;
        }
    }

    public static String fromStackKindCast(CiKind k, String var) {
        if (k == Boolean) {
            return "UnsafeCast.asBoolean((byte) " + var + ")";
        } else if (k.stackKind() == Int && k != Int) {
            return "(" + k + ") " + var;
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

    public static boolean isRefOrWord(CiKind k) {
        return k == Object || k == Word;
    }

    /**
     * The string that precedes the generic template tag name to indicate type.
     * E.g. The {@code A} in {@code ALOAD}.
     * @param k
     * @return
     */
    public static String tagPrefix(CiKind k) {
        return k == Void ? "" : String.valueOf(Character.toUpperCase(k.typeChar));
    }

    /**
     * The string that precedes the generic template method name to indicate type.
     * @param k
     * @return
     */
    public static String opPrefix(CiKind k) {
        return "" + k.typeChar;
    }

    public static boolean hasGetPutTemplates(CiKind k) {
        return k != Void;
    }
    public static boolean hasArrayTemplates(CiKind k) {
        return !(k == Void || k == Boolean || k == Word);
    }

    public static boolean hasPTemplates(CiKind k) {
        return !(k == Void || k == Boolean);
    }

    public static boolean hasPCmpSwpTemplates(CiKind k) {
        return k == Int || k == Object || k == Word;
    }

    public static boolean hasI2Templates(CiKind k) {
        return !(k == Int || k == Void || k == Object || k == Word || k == Boolean);
    }

    public static boolean hasL2Templates(CiKind k) {
        return k == Int || k == Float || k == Double;
    }

    public static boolean hasF2Templates(CiKind k) {
        return k == Int || k == Long || k == Double;
    }

    public static boolean hasD2Templates(CiKind k) {
        return k == Int || k == Long || k == Float;
    }

    public static boolean hasArithTemplates(CiKind k) {
        return k == Int || k == Long || k == Float || k == Double;
    }

    public static boolean hasLogTemplates(CiKind k) {
        return k == Int || k == Long;
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

    public static boolean hasReturnTemplates(CiKind k) {
        return k == k.stackKind() && k.isValidReturnType();
    }

    public static boolean hasInvokeTemplates(CiKind k) {
        return k == Void || k == Float || k == Long || k == Double || k == Object || k == Word;
    }

    public static boolean hasCmpTemplates(CiKind k) {
        return k == Float || k == Double || k == Long;
    }

    /**
     * The stream to use for the generated output.
     */
    public final PrintStream out;

    // end of static declarations

    private AdviceHook adviceHook;

    private String generatingClassName;

    public T1XTemplateGenerator(Class<?> generatingClass, PrintStream out) {
        generatingClassName = generatingClass.getSimpleName();
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
     * Generate a // GENERATED comment with T1XTemplateGenerator to rerun.
     */
    public void generateAutoComment() {
        if (adviceHook != null) {
            adviceHook.startMethodGeneration();
        }
        //out.printf("    // GENERATED -- EDIT AND RUN %s.main() TO MODIFY%n", generatingClassName);
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
        generateAutoComment();
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
        for (CiKind k : kinds) {
            if (hasGetPutTemplates(k)) {
                generatePutFieldTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code PUTFIELD} template tag for given type.
     */
    public void generatePutFieldTemplate(CiKind k) {
        final int objectSlot = k.isDoubleWord() ? 2 : 1;
        final String m = k == Object ? "noninlineW" : "w";
        generateAutoComment();
        generateTemplateTag("PUTFIELD$%s$resolved", lr(k));
        out.printf("    public static void putfield%s(@Slot(%d) Object object, int offset, @Slot(0) %s value) {%n", ur(k), objectSlot, rs(k));
        generateBeforeAdvice(k);
        out.printf("        TupleAccess.%srite%s(object, offset, %s);%n", m, u(k), fromStackKindCast(k, "value"));
        out.printf("    }%n");
        newLine();

        generateAutoComment();
        generateTemplateTag("PUTFIELD$%s", lr(k));
        out.printf("    public static void putfield%s(ResolutionGuard.InPool guard, @Slot(%d) Object object, @Slot(0) %s value) {%n", ur(k), objectSlot, rs(k));
        out.printf("        resolveAndPutField%s(guard, object, value);%n", ur(k));
        out.printf("    }%n");
        newLine();

        generateAutoComment();
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
        for (CiKind k : kinds) {
            if (hasGetPutTemplates(k)) {
                generatePutStaticTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code PUTSTATIC} template tag for given type.
     * @param k type
     */
    public void generatePutStaticTemplate(CiKind k) {
        final String m = k == Object ? "noninlineW" : "w";
        generateAutoComment();
        generateTemplateTag("PUTSTATIC$%s$init", lr(k));
        out.printf("    public static void putstatic%s(Object staticTuple, int offset, @Slot(0) %s value) {%n", ur(k), rs(k));
        generateBeforeAdvice(k);
        out.printf("        TupleAccess.%srite%s(staticTuple, offset, %s);%n", m, u(k), fromStackKindCast(k, "value"));
        out.printf("    }%n");
        newLine();

        generateAutoComment();
        generateTemplateTag("PUTSTATIC$%s", lr(k));
        out.printf("    public static void putstatic%s(ResolutionGuard.InPool guard, @Slot(0) %s value) {%n", ur(k), rs(k));
        out.printf("        resolveAndPutStatic%s(guard, value);%n", ur(k));
        out.printf("    }%n");
        newLine();

        generateAutoComment();
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
        for (CiKind k : kinds) {
            if (hasGetPutTemplates(k)) {
                generateGetFieldTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code GETFIELD} template tag for given type.
     * @param k type
     */
    public void generateGetFieldTemplate(CiKind k) {
        generateAutoComment();
        generateTemplateTag("GETFIELD$%s$resolved", lr(k));
        out.printf("    public static %s getfield%s(@Slot(0) Object object, int offset) {%n", rs(k), u(k));
        generateBeforeAdvice(k);
        out.printf("        %s result = TupleAccess.read%s(object, offset);%n", k, u(k));
        out.printf("        return %s;%n", toStackKindCast(k, "result"));
        out.printf("    }%n");
        newLine();

        generateAutoComment();
        generateTemplateTag("GETFIELD$%s", lr(k));
        out.printf("    public static %s getfield%s(ResolutionGuard.InPool guard, @Slot(0) Object object) {%n", rs(k), ur(k));
        out.printf("        return resolveAndGetField%s(guard, object);%n", ur(k), u(k));
        out.printf("    }%n");
        newLine();

        generateAutoComment();
        out.printf("    @NEVER_INLINE%n");
        out.printf("    public static %s resolveAndGetField%s(ResolutionGuard.InPool guard, Object object) {%n", rs(k), ur(k));
        out.printf("        FieldActor f = Snippets.resolveInstanceFieldForReading(guard);%n");
        generateBeforeAdvice(k);
        out.printf("        if (f.isVolatile()) {%n");
        out.printf("            preVolatileRead();%n");
        out.printf("            %s value = TupleAccess.read%s(object, f.offset());%n", k, u(k));
        out.printf("            postVolatileRead();%n");
        out.printf("            return %s;%n", toStackKindCast(k, "value"));
        out.printf("        } else {%n");
        out.printf("            %s result = TupleAccess.read%s(object, f.offset());%n", k, u(k));
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
        for (CiKind k : kinds) {
            if (hasGetPutTemplates(k)) {
                generateGetStaticTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code GETFIELD} template tag for given type.
     * @param k type
     */
    public void generateGetStaticTemplate(CiKind k) {
        generateAutoComment();
        generateTemplateTag("GETSTATIC$%s", lr(k));
        out.printf("    public static %s getstatic%s(ResolutionGuard.InPool guard) {%n", rs(k), ur(k));
        out.printf("        return resolveAndGetStatic%s(guard);%n", ur(k), u(k));
        out.printf("    }%n");
        newLine();

        generateAutoComment();
        out.printf("    @NEVER_INLINE%n");
        out.printf("    public static %s resolveAndGetStatic%s(ResolutionGuard.InPool guard) {%n", rs(k), ur(k));
        out.printf("        FieldActor f = Snippets.resolveStaticFieldForReading(guard);%n");
        out.printf("        Snippets.makeHolderInitialized(f);%n");
        generateBeforeAdvice(k);
        out.printf("        if (f.isVolatile()) {%n");
        out.printf("            preVolatileRead();%n");
        out.printf("            %s value = TupleAccess.read%s(f.holder().staticTuple(), f.offset());%n", k, u(k));
        out.printf("            postVolatileRead();%n");
        out.printf("            return %s;%n", toStackKindCast(k, "value"));
        out.printf("        } else {%n");
        out.printf("            %s result = TupleAccess.read%s(f.holder().staticTuple(), f.offset());%n", k, u(k));
        out.printf("            return %s;%n", toStackKindCast(k, "result"));
        out.printf("        }%n");
        out.printf("    }%n");
        out.printf("%n");

        generateAutoComment();
        generateTemplateTag("GETSTATIC$%s$init", lr(k));
        out.printf("    public static %s getstatic%s(Object staticTuple, int offset) {%n", rs(k), u(k));
        generateBeforeAdvice(k);
        out.printf("        %s result = TupleAccess.read%s(staticTuple, offset);%n", k, u(k));
        out.printf("        return %s;%n", toStackKindCast(k, "result"));
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> ALOAD_TEMPLATE_TAGS = EnumSet.of(IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD);

    /**
     * Generate all the {@link #ALOAD_TEMPLATE_TAGS}.
     */
    public void generateArrayLoadTemplates() {
        for (CiKind k : kinds) {
            if (hasArrayTemplates(k)) {
                generateArrayLoadTemplate(k);
            }
        }
    }

    /**
     * Generate the {@code ALOAD} template(s) for given type.
     */
    public void generateArrayLoadTemplate(CiKind k) {
        generateAutoComment();
        generateTemplateTag("%sALOAD", tagPrefix(k));
        out.printf("    public static %s %saload(@Slot(1) Object array, @Slot(0) int index) {%n", rs(k), opPrefix(k));
        out.printf("        ArrayAccess.checkIndex(array, index);%n");
        generateBeforeAdvice(k);
        out.printf("        %s result = ArrayAccess.get%s(array, index);%n", k, u(k));
        out.printf("        return %s;%n", toStackKindCast(k, "result"));
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> ASTORE_TEMPLATE_TAGS = EnumSet.of(IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE);

    /**
     * Generate all the {@link #ASTORE_TEMPLATE_TAGS}.
     */
    public void generateArrayStoreTemplates() {
        for (CiKind k : kinds) {
            if (hasArrayTemplates(k)) {
                generateArrayStoreTemplate(k);
            }
        }
    }

    /**
     * Generate the {@code ALOAD} template(s) for given type.
     * @param k type
     */
    public void generateArrayStoreTemplate(CiKind k) {
        final int arraySlot = k.isDoubleWord() ? 3 : 2;
        final int indexSlot = k.isDoubleWord() ? 2 : 1;
        generateAutoComment();
        generateTemplateTag("%sASTORE", tagPrefix(k));
        out.printf("    public static void %sastore(@Slot(%d) Object array, @Slot(%d) int index, @Slot(0) %s value) {%n", opPrefix(k), arraySlot, indexSlot, rs(k));
        out.printf("        ArrayAccess.checkIndex(array, index);%n");
        generateBeforeAdvice(k);
        if (k == Object) {
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
        generateNewTemplate("");
        generateNewTemplate("init");
    }

    /**
     * Generate the requested {@code NEW} template.
     * @param init if "" generate {@code NEW} template, else {@code NEW$init}.
     */
    public void generateNewTemplate(String init) {
        String t;
        String m;
        String a;
        if (init.equals("")) {
            t = "ResolutionGuard";
            m = "resolveClassForNewAndCreate";
            a = "guard";
        } else {
            t = "ClassActor";
            m = "createTupleOrHybrid";
            a = "classActor";
        }
        generateAutoComment();
        generateTemplateTag("NEW%s", prefixDollar(init));
        out.printf("    public static Object new_(%s %s) {%n", t, a);
        out.printf("        Object object = %s(%s);%n", m, a);
        generateAfterAdvice(NULL_ARGS);
        out.printf("        return object;%n");
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> NEWARRAY_TEMPLATE_TAGS = EnumSet.of(NEWARRAY);

    public void generateNewArrayTemplate() {
        generateAutoComment();
        generateTemplateTag("NEWARRAY");
        out.printf("    public static Object newarray(Kind<?> kind, @Slot(0) int length) {%n");
        out.printf("        Object array = createPrimitiveArray(kind, length);%n");
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
        generateAutoComment();
        generateTemplateTag("ANEWARRAY%s", prefixDollar(resolved));
        out.printf("    public static Object anewarray(%s %s, @Slot(0) int length) {%n", t, v);
        if (resolved.equals("")) {
            out.printf("        ArrayClassActor<?> arrayClassActor = UnsafeCast.asArrayClassActor(Snippets.resolveArrayClass(arrayType));%n");
        } else {
            out.printf("        ArrayClassActor<?> arrayClassActor = arrayType;%n");
        }
        out.printf("        Object array = T1XRuntime.createReferenceArray(arrayClassActor, length);%n");
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
        generateAutoComment();
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
        generateAutoComment();
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
        generateAutoComment();
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
        generateAutoComment();
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
        generateAutoComment();
        generateTemplateTag("ARRAYLENGTH");
        out.printf("    public static int arraylength(@Slot(0) Object array) {%n");
        out.printf("        int length = ArrayAccess.readArrayLength(array);%n");
        generateBeforeAdvice(NULL_ARGS);
        out.printf("        return length;%n");
        out.printf("    }%n");
        newLine();
    }

    public void generateAThrowTemplate() {
        generateAutoComment();
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
        generateAutoComment();
        generateTemplateTag("MONITOR%s", tag.toUpperCase());
        out.printf("    public static void monitor%s(@Slot(0) Object object) {%n", tag);
        generateBeforeAdvice(NULL_ARGS);
        out.printf("        T1XRuntime.monitor%s(object);%n", tag);
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
        for (CiKind k : kinds) {
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
        for (CiKind k : kinds) {
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
    public void generateUnresolvedInvokeVITemplate(CiKind k, String variant) {
        generateAutoComment();
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
    public void generateInvokeVITemplate(CiKind k, String variant, boolean instrumented) {
        String params = variant.equals("interface") ? "InterfaceMethodActor interfaceMethodActor" : "int vTableIndex";
        if (instrumented) {
            params += ", MethodProfile mpo, int mpoIndex";
        }
        generateAutoComment();
        out.printf("    /**%n");
        out.printf("     * Selects the correct implementation of a resolved method referenced by an INVOKE%s instruction.%n", variant.toUpperCase());
        out.printf("     *%n");
        if (variant.equals("interface")) {
            out.printf("     * @param interfaceMethodActor the resolved interface method being invoked%n");
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
                out.printf("        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor).asAddress().%n");
            } else {
                out.printf("        return Snippets.selectInterfaceMethod(receiver, interfaceMethodActor, mpo, mpoIndex).%n");
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
        for (CiKind k : kinds) {
            if (hasInvokeTemplates(k)) {
                generateInvokeSSTemplate(k, "static");
            }
        }
    }

    /**
     * Generate all the {@link #INVOKE_SPECIAL_TEMPLATE_TAGS}.
     */
    public void generateInvokeSpecialTemplates() {
        for (CiKind k : kinds) {
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
    public void generateInvokeSSTemplate(CiKind k, String variant) {
        String params = "ResolutionGuard.InPool guard";
        if (variant.equals("special")) {
            params += ", Reference receiver";
        }
        generateAutoComment();
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
        for (CiKind k : kinds) {
            if (hasI2Templates(k)) {
                generateI2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code I2} template.
     * @param k target type
     */
    public void generateI2Template(CiKind k) {
        generateAutoComment();
        generateTemplateTag("I2%c", u(k).charAt(0));
        out.printf("    public static %s i2%c(@Slot(0) int value) {%n", rs(k), k.typeChar);
        String cast = k == Char || k == Byte || k == Short ? "(" + k + ") " : "";
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
        for (CiKind k : kinds) {
            if (hasL2Templates(k)) {
                generateL2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code L2} template.
     * @param k target type
     */
    public void generateL2Template(CiKind k) {
        generateAutoComment();
        generateTemplateTag("L2%c", u(k).charAt(0));
        out.printf("    public static %s l2%c(@Slot(0) long value) {%n", rs(k), k.typeChar);
        generateBeforeAdvice(k);
        String cast = k == Int ? "(int) " : "";
        out.printf("        return %svalue;%n", cast);
        out.printf("    }%n");
        newLine();
    }


    public static final EnumSet<T1XTemplateTag> D2_TEMPLATE_TAGS = EnumSet.of(D2I, D2L, D2F);

    /**
     * Generate all the {@link #D2_TEMPLATE_TAGS}.
     */
    public void generateD2Templates() {
        for (CiKind k : kinds) {
            if (hasD2Templates(k)) {
                generateD2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code D2} template.
     * @param k target type
     */
    public void generateD2Template(CiKind k) {
        generateAutoComment();
        generateTemplateTag("D2%c", u(k).charAt(0));
        out.printf("    public static %s d2%c(@Slot(0) double value) {%n", rs(k), k.typeChar);
        generateBeforeAdvice(k);
        String arg2 = k == Float ? "(float) value" : "T1XRuntime.d2" + k.typeChar + "(value)";
        out.printf("        return %s;%n", arg2);
        out.printf("    }%n");
        newLine();
    }


    public static final EnumSet<T1XTemplateTag> F2_TEMPLATE_TAGS = EnumSet.of(F2I, F2L, F2D);

    /**
     * Generate all the {@link #F2_TEMPLATE_TAGS}.
     */
    public void generateF2Templates() {
        for (CiKind k : kinds) {
            if (hasF2Templates(k)) {
                generateF2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code F2} template.
     * @param k target type
     */
    public void generateF2Template(CiKind k) {
        generateAutoComment();
        generateTemplateTag("F2%c", u(k).charAt(0));
        out.printf("    public static %s f2%c(@Slot(0) float value) {%n", rs(k), k.typeChar);
        generateBeforeAdvice(k);
        String arg2 = k == Double ? "value" : "T1XRuntime.f2" + k.typeChar + "(value)";
        out.printf("        return %s;%n", arg2);
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> MOV_TEMPLATE_TAGS = EnumSet.of(MOV_I2F, MOV_F2I, MOV_L2D, MOV_D2L);

    /**
     * Generate all the {@link #MOV_TEMPLATE_TAGS}.
     */
    public void generateMovTemplates() {
        generateMovTemplate(Float, Int);
        generateMovTemplate(Int, Float);
        generateMovTemplate(Double, Long);
        generateMovTemplate(Long, Double);
    }

    /**
     * Generate a given {@code MOV} template.
     * @param from source type
     * @param target type
     */
    public void generateMovTemplate(CiKind from, CiKind to) {
        generateAutoComment();
        generateTemplateTag("MOV_%s2%s", u(from).charAt(0), u(to).charAt(0));
        out.printf("    public static %s mov_%s2%s(@Slot(0) %s value) {%n", rs(to), from.typeChar, to.typeChar, rs(from));
        generateBeforeAdvice(from, to);
        out.printf("        return Intrinsics.%sTo%s(value);%n", from, u(to));
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> NEG_TEMPLATE_TAGS = EnumSet.of(INEG, LNEG, FNEG, DNEG);

    /**
     * Generate all {@link #NEG_TEMPLATE_TAGS}.
     */
    public void generateNegTemplates() {
        for (CiKind k : kinds) {
            if (hasArithTemplates(k)) {
                generateNegTemplate(k);
            }
        }
    }

    /**
     * Generate specific {@code NEG} template.
     * @param k type
     */
    public void generateNegTemplate(CiKind k) {
        final String op = "neg";
        generateAutoComment();
        generateTemplateTag("%s%s", tagPrefix(k), op.toUpperCase());
        out.printf("    public static %s %s%s(@Slot(0) %s value, %s zero) {%n", k, opPrefix(k), op, k, k);
        generateBeforeAdvice(k);
        out.printf("        return zero - value;%n");
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> WDIVREM_TEMPLATE_TAGS = EnumSet.of(WDIV, WDIVI, WREM, WREMI);

    /**
     * Generate all the {@link #WDIVREM_TEMPLATE_TAGS}.
     */
    public void generateWordDivRemTemplates() {
        for (String s : new String[] {"div", "rem"}) {
            generateWordDivRemTemplate(s, "");
            generateWordDivRemTemplate(s, "i");
        }
    }

    /**
     * Generate a specific {@link #WDIVREM_TEMPLATE_TAGS} template.
     * @param op "div" or "rem"
     * @param iTag "" or "i"
     */
    public void generateWordDivRemTemplate(String op, String iTag) {
        final String m = op.equals("div") ? "dividedBy" : "remainder";
        final String t = op.equals("rem") && iTag.equals("i") ? "int" : "Address";
        generateAutoComment();
        generateTemplateTag("W%s%s", op.toUpperCase(), iTag.toUpperCase());
        out.printf("    public static %s w%s%s(@Slot(1) Address value1, @Slot(0) %s value2) {%n", t, op, iTag, iTag.equals("i") ? "int" : "Address");
        generateBeforeAdvice(op, iTag);
        out.printf("        return value1.%s(value2);%n", m);
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> DYADIC_TEMPLATE_TAGS = EnumSet.of(IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, ISHL, LSHL, ISHR,
                    LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR);

    /**
     * Generate all the {@link #DYADIC_TEMPLATE_TAGS).
     */
    public void generateDyadicTemplates() {
        for (CiKind k : kinds) {
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
    public void generateDyadicTemplate(CiKind k, String op) {
        final boolean arg2IsInt = isShift(op);
        final int arg1Slot = isShift(op) || k.jvmSlots == 1 ? 1 : 2;
        generateAutoComment();
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
    public void generateReturnTemplate(CiKind k, String unlock) {
        // Admittedly, the readability goal is a stretch here!
        final String arg1 = unlock.equals("") ? "" : "Reference object";
        final String arg2 = k == Void ? "" : "@Slot(0) " + rs(k) + " value";
        final String sep = !arg1.isEmpty() && !arg2.isEmpty() ? ", " : "";
        generateAutoComment();
        generateTemplateTag("%sRETURN%s", tagPrefix(k), prefixDollar(unlock));
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
            if (k != Void) {
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
        generateCmpTemplate(Long, "");
        for (CiKind k : new CiKind[] {Float, Double}) {
            for (String s : new String[] {"g", "l"}) {
                generateCmpTemplate(k, s);
            }
        }
    }

    /**
     * Generate a specific {@link #CMP_TEMPLATE_TAGS} template.
     * @param k type one of "long", "float" or "double"
     * @param variant "g" or "l"
     */
    public void generateCmpTemplate(CiKind k, String variant) {
        generateAutoComment();
        generateTemplateTag("%sCMP%s", tagPrefix(k), variant.toUpperCase());
        out.printf("    public static int %scmp%sOp(@Slot(%d) %s value1, @Slot(0) %s value2) {%n", opPrefix(k), variant, k.isDoubleWord() ? 2 : 1, s(k), s(k));
        out.printf("        int result = %scmp%s(value1, value2);%n", opPrefix(k), variant);
        generateBeforeAdvice(k);
        out.printf("        return result;%n");
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> PSET_TEMPLATE_TAGS = EnumSet.of(PSET_BYTE, PSET_SHORT, PSET_INT, PSET_FLOAT, PSET_LONG, PSET_DOUBLE, PSET_WORD, PSET_REFERENCE);

    /**
     * Generate all the {@link #PSET_TEMPLATE_TAGS}.
     */
    public void generatePSetTemplates() {
        for (CiKind k : kinds) {
            if (hasPTemplates(k)) {
                generatePSetTemplate(k);
            }
        }
    }

    /**
     * Generate a specific {@link #PSET_TEMPLATE_TAGS) template.
     * @param k type
     */
    public void generatePSetTemplate(CiKind k) {
        if (k == Char) {
            return;
        }
        final int indexSlot = k.isDoubleWord() ? 2 : 1;
        generateAutoComment();
        generateTemplateTag("PSET_%s", au(k));
        out.printf("    public static void pset_%s(@Slot(%d) Pointer pointer, @Slot(%d) int disp, @Slot(%d) int index, @Slot(0) %s value) {%n", l(k), indexSlot + 2, indexSlot + 1, indexSlot, rs(k));
        generateBeforeAdvice(k);
        out.printf("        pointer.set%s(disp, index, %s);%n", ur(k), fromStackKindCast(k, "value"));
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> PGET_TEMPLATE_TAGS = tags("PGET_");

    /**
     * Generate all the {@link #PGET_TEMPLATE_TAGS}.
     */
    public void generatePGetTemplates() {
        for (CiKind k : kinds) {
            if (hasPTemplates(k)) {
                generatePGetTemplate(k);
            }
        }
    }

    /**
     * Generate a specific {@link #PSET_TEMPLATE_TAGS) template.
     * @param k type
     */
    public void generatePGetTemplate(CiKind k) {
        generateAutoComment();
        generateTemplateTag("PGET_%s", au(k));
        out.printf("    public static %s pget_%s(@Slot(2) Pointer pointer, @Slot(1) int disp, @Slot(0) int index) {%n", rs(k), l(k));
        generateBeforeAdvice(k);
        out.printf("        return pointer.get%s(disp, index);%n", ur(k));
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> PREAD_TEMPLATE_TAGS = tags("PREAD_");

    /**
     * Generate all the {@link #PREAD_TEMPLATE_TAGS}.
     */
    public void generatePReadTemplates() {
        for (CiKind k : kinds) {
            if (hasPTemplates(k)) {
                generatePReadTemplate(k, false);
                generatePReadTemplate(k, true);
            }
        }
    }

    /**
     * Generate a specific {@link #PSET_TEMPLATE_TAGS) template.
     * @param k type
     * @param isI true iff {@code PREAD_XXX_I} variant
     */
    public void generatePReadTemplate(CiKind k, boolean isI) {
        generateAutoComment();
        generateTemplateTag("PREAD_%s%s", au(k), isI ? "_I" : "");
        out.printf("    public static %s pread_%s%s(@Slot(2) Pointer pointer, @Slot(1) %s offset) {%n", k.stackKind(), l(k), isI ? "_i" : "", isI ? "int" : "Offset");
        generateBeforeAdvice(k);
        out.printf("        return pointer.read%s(offset);%n", ur(k));
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> PWRITE_TEMPLATE_TAGS = tags("PWRITE_");

    /**
     * Generate all the {@link #PWRITE_TEMPLATE_TAGS}.
     */
    public void generatePWriteTemplates() {
        for (CiKind k : kinds) {
            if (hasPTemplates(k)) {
                generatePWriteTemplate(k, false);
                generatePWriteTemplate(k, true);
            }
        }
    }

    /**
     * Generate a specific {@link #PSET_TEMPLATE_TAGS) template.
     * @param k type
     * @param isI true iff {@code PWRITE_XXX_I} variant
     */
    public void generatePWriteTemplate(CiKind k, boolean isI) {
        if (k == Char) {
            return;
        }
        final int offsetSlot = k.isDoubleWord() ? 2 : 1;
        generateAutoComment();
        generateTemplateTag("PWRITE_%s%s", au(k), isI ? "_I" : "");
        out.printf("    public static void pwrite_%s%s(@Slot(%d) Pointer pointer, @Slot(%d) %s offset, @Slot(0) %s value) {%n", l(k), isI ? "_i" : "", offsetSlot + 1, offsetSlot, isI ? "int" : "Offset", rs(k));
        generateBeforeAdvice(k);
        out.printf("        pointer.write%s(offset, %s);%n", ur(k), fromStackKindCast(k, "value"));
        out.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> PCMPSWP_TEMPLATE_TAGS = tags("PCMPSWP_");

    /**
     * Generate all the {@link #PCMPSWP_TEMPLATE_TAGS}.
     */
    public void generatePCmpSwpTemplates() {
        for (CiKind k : kinds) {
            if (hasPCmpSwpTemplates(k)) {
                generatePCmpSwpTemplate(k, false);
                generatePCmpSwpTemplate(k, true);
            }
        }
    }

    /**
     * Generate a specific {@link #PCMPSWP_TEMPLATE_TAGS) template.
     * @param k type
     * @param isI true iff {@code PCMPSWP_XXX_I} variant
     */
    public void generatePCmpSwpTemplate(CiKind k, boolean isI) {
        generateAutoComment();
        generateTemplateTag("PCMPSWP_%s%s", au(k), isI ? "_I" : "");
        out.printf("    public static %s pcmpswp_%s%s(@Slot(3) Pointer ptr, @Slot(2) %s off, @Slot(1) %s expectedValue, @Slot(0) %s newValue) {%n", rs(k), lr(k), isI ? "_i" : "", isI ? "int" : "Offset", rs(k), rs(k));
        generateBeforeAdvice(k);
        out.printf("        return ptr.compareAndSwap%s(off, expectedValue, newValue);%n", ur(k));
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
        for (CiKind k : kinds) {
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
            if (hasCmpTemplates(k)) {
                if (k == Long) {
                    generateCmpTemplate(k, "");
                } else {
                    for (String s : new String[] {"g", "l"}) {
                        generateCmpTemplate(k, s);
                    }
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
            if (hasPTemplates(k)) {
                generatePGetTemplate(k);
                generatePSetTemplate(k);
                generatePReadTemplate(k, false);
                generatePWriteTemplate(k, false);
                generatePReadTemplate(k, true);
                generatePWriteTemplate(k, true);
            }
            if (hasPCmpSwpTemplates(k)) {
                generatePCmpSwpTemplate(k, false);
                generatePCmpSwpTemplate(k, true);
            }
        }
        // Special cases
        for (String s : new String[] {"div", "rem"}) {
            generateWordDivRemTemplate(s, "");
            generateWordDivRemTemplate(s, "i");
        }
        generateMovTemplates();
        generateNewTemplates();
        generateNewArrayTemplate();
        generateANewArrayTemplates();
        generateMultiANewArrayTemplates();
        generateCheckcastTemplates();
        generateArraylengthTemplate();
        generateAThrowTemplate();
        generateMonitorTemplates();
        generateInstanceofTemplates();
        generateReturnTemplate(Void, "registerFinalizer");
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
        new T1XTemplateGenerator(T1XTemplateGenerator.class, out).generateAll(null);
        ReadableSource content = ReadableSource.Static.fromString(baos.toString());
        return Files.updateGeneratedContent(outputFile, content, "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
    }


}

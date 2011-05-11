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

import static com.sun.max.vm.t1x.T1XTemplateTag.*;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;

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
 * The generated code is written to the {@link #o} {@link PrintStream}, which defaults to {@link System#out).
 * However, this stream can be changed before invoking any of the generating methods.
 * The default behavior is to generate all methods using the {@link #main} method.
 *
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
    public enum AdviceType {BEFORE, AFTER}

    private static AdviceHook adviceHook;

    private static final String[] NULL_ARGS = new String[] {};

    /**
     * {@link String} equivalent of {@link KindEnum} with standard case rules.
     * Arguably this class could use {@link KindEnum} more, but it is mostly doing string processing.
     */
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

    private static final String[] conditions = new String[] {"eq", "ne", "lt", "ge", "gt", "le"};

    static {
        for (String type : types) {
            uTypes.put(type, type.substring(0, 1).toUpperCase() + type.substring(1));
            lTypes.put(type, type.substring(0, 1).toLowerCase() + type.substring(1));
            oTypes.put(type, type.equals("Reference") ? "Object" : type);
            uoTypes.put(type, type.equals("Reference") ? "Object" : uTypes.get(type));
            auTypes.put(type, type.toUpperCase());
        }
    }

    /**
     * The stream to use for the generated output.
     */
    public static PrintStream o = System.out;

    /**
     * Set the advice hook explicitly, which may be necessary when generating individual templates
     * with advice.
     * @param adviceHook
     */
    public static void setAdviceHook(AdviceHook hook) {
        adviceHook = hook;
    }

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

    public static boolean hasGetPutTemplates(String k) {
        return !k.equals("void");
    }
    public static boolean hasArrayTemplates(String k) {
        return !(k.equals("void") || k.equals("byte") || k.equals("Word"));
    }

    public static boolean hasPTemplates(String k) {
        return !(k.equals("void") || k.equals("boolean"));
    }

    public static boolean hasPCmpSwpTemplates(String k) {
        return k.equals("int") || k.equals("Reference") || k.equals("Word");
    }

    public static boolean hasI2Templates(String k) {
        return !(k.equals("int") || k.equals("void") || k.equals("Reference") || k.equals("Word") || k.equals("boolean"));
    }

    public static boolean hasL2Templates(String k) {
        return k.equals("int") || k.equals("float") || k.equals("double");
    }

    public static boolean hasF2Templates(String k) {
        return k.equals("int") || k.equals("long") || k.equals("double");
    }

    public static boolean hasD2Templates(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float");
    }

    public static boolean hasArithTemplates(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float") || k.equals("double");
    }

    public static boolean hasLogTemplates(String k) {
        return k.equals("int") || k.equals("long");
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

    public static boolean hasLoadStoreTemplates(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float") || k.equals("double") || k.equals("Reference") || k.equals("Word");
    }

    public static boolean hasLDCTemplates(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float") || k.equals("double") || k.equals("Reference");
    }

    public static boolean hasReturnTemplates(String k) {
        return k.equals("int") || k.equals("long") || k.equals("float") || k.equals("double") ||
               k.equals("Word") || k.equals("Reference") || k.equals("void");
    }

    public static boolean hasInvokeTemplates(String k) {
        return k.equals("void") || k.equals("float") || k.equals("long") || k.equals("double") || k.equals("Word");
    }

    public static boolean hasIfCmpTemplates(String k) {
        return k.equals("int") || k.equals("Reference");
    }

    public static boolean hasCmpTemplates(String k) {
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

    private static void generateBeforeAdvice(String ... args) {
        if (adviceHook != null) {
            adviceHook.generate(currentTemplateTag, AdviceType.BEFORE, args);
        }
    }

    private static void generateAfterAdvice(String ... args) {
        if (adviceHook != null) {
            adviceHook.generate(currentTemplateTag, AdviceType.AFTER, args);
        }
    }

    public static final EnumSet<T1XTemplateTag> SHORT_CONST_TEMPLATES = EnumSet.of(WCONST_0, ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5);

    /**
     * Generate all the {@link SHORT_CONST_TEMPLATES}.
     */
    public static void generateShortConstTemplates() {
        for (String k : new String[] {"int", "Reference", "Word"}) {
            if (k.equals("int")) {
                for (String s : new String[] {"0", "1", "2", "3", "4", "5", "M1"}) {
                    generateShortConstTemplate(k, s);
                }
            } else if (k.equals("Reference")) {
                generateShortConstTemplate(k, "NULL");
            } else if (k.equals("Word")) {
                generateShortConstTemplate(k, "0");
            }
        }
    }

    /**
     * Generates one of the {@link #SHORT_CONST_TEMPLATES}.
     * @param k type, one of {@code int}, {@code Word} or {@code Reference}
     * @param arg extra argument specifying the constant, {@code 0-5, M1, NULL}
     */
    public static void generateShortConstTemplate(String k, String arg) {
        generateAutoComment();
        generateTemplateTag("%sCONST_%s", tagPrefix(k), arg);
        o.printf("    public static void %sconst_%s() {%n", opPrefix(k), arg.toLowerCase());
        String argVal = k.equals("Word") ? "Address.zero()" : (arg.equals("M1") ? "-1" : arg.toLowerCase());
        generateBeforeAdvice(k);
        o.printf("        push%s(%s);%n", uoType(k), argVal);
        o.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> CONST_TEMPLATES = EnumSet.of(LCONST, FCONST, DCONST);

    /**
     * Generate all the {@link #CONST_TEMPLATES}.
     */
    public static void generateConstTemplates() {
        for (String k : new String[] {"float", "double", "long"}) {
            generateConstTemplate(k);
        }
    }

    /**
     * Generates one of the {@link #CONST_TEMPLATES}.
     * @param k type, one of {@code float}, {@code double} or {@code long}
     */
    public static void generateConstTemplate(String k) {
        generateAutoComment();
        generateTemplateTag("%sCONST", tagPrefix(k));
        o.printf("    public static void %sconst(%s constant) {%n", opPrefix(k), k);
        generateBeforeAdvice(k);
        o.printf("        push%s(constant);%n", uType(k));
        o.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> PUTFIELD_TEMPLATE_TAGS = EnumSet.of(PUTFIELD$byte, PUTFIELD$boolean, PUTFIELD$char, PUTFIELD$short, PUTFIELD$int, PUTFIELD$float, PUTFIELD$long,
                    PUTFIELD$double, PUTFIELD$reference, PUTFIELD$word, PUTFIELD$byte$resolved, PUTFIELD$boolean$resolved, PUTFIELD$char$resolved, PUTFIELD$short$resolved, PUTFIELD$int$resolved,
                    PUTFIELD$float$resolved, PUTFIELD$long$resolved, PUTFIELD$double$resolved, PUTFIELD$reference$resolved, PUTFIELD$word$resolved);

    /**
     * Generate all the {@link #PUTFIELD_TEMPLATE_TAGS}.
     */
    public static void generatePutFieldTemplates() {
        for (String k : types) {
            if (hasGetPutTemplates(k)) {
                generatePutFieldTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code PUTFIELD} template tag for given type.
     * @param k type
     */
    public static void generatePutFieldTemplate(String k) {
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
    }

    public static final EnumSet<T1XTemplateTag> PUTSTATIC_TEMPLATE_TAGS = EnumSet.of(PUTSTATIC$byte, PUTSTATIC$boolean, PUTSTATIC$char, PUTSTATIC$short, PUTSTATIC$int, PUTSTATIC$float,
                    PUTSTATIC$long, PUTSTATIC$double, PUTSTATIC$reference, PUTSTATIC$word, PUTSTATIC$byte$init, PUTSTATIC$boolean$init, PUTSTATIC$char$init, PUTSTATIC$short$init, PUTSTATIC$int$init,
                    PUTSTATIC$float$init, PUTSTATIC$long$init, PUTSTATIC$double$init, PUTSTATIC$reference$init, PUTSTATIC$word$init);
    /**
    * Generate all the {@link #PUTSTATIC_TEMPLATE_TAGS}.
    */
    public static void generatePutStaticTemplates() {
        for (String k : types) {
            if (hasGetPutTemplates(k)) {
                generatePutStaticTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code PUTSTATIC} template tag for given type.
     * @param k type
     */
    public static void generatePutStaticTemplate(String k) {
        final String m = uoType(k).equals("Object") ? "noninlineW" : "w";
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

    public static final EnumSet<T1XTemplateTag> GETFIELD_TEMPLATE_TAGS = EnumSet.of(GETFIELD$byte, GETFIELD$boolean, GETFIELD$char, GETFIELD$short, GETFIELD$int, GETFIELD$float, GETFIELD$long,
                    GETFIELD$double, GETFIELD$reference, GETFIELD$word, GETFIELD$byte$resolved, GETFIELD$boolean$resolved, GETFIELD$char$resolved, GETFIELD$short$resolved, GETFIELD$int$resolved,
                    GETFIELD$float$resolved, GETFIELD$long$resolved, GETFIELD$double$resolved, GETFIELD$reference$resolved, GETFIELD$word$resolved);

    /**
    * Generate all the {@link #GETFIELD_TEMPLATE_TAGS}.
    */
    public static void generateGetFieldTemplates() {
        for (String k : types) {
            if (hasGetPutTemplates(k)) {
                generateGetFieldTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code GETFIELD} template tag for given type.
     * @param k type
     */
    public static void generateGetFieldTemplate(String k) {
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
        o.printf("    @NEVER_INLINE%n");
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
    }

    public static final EnumSet<T1XTemplateTag> GETSTATIC_TEMPLATE_TAGS = EnumSet.of(GETSTATIC$byte, GETSTATIC$boolean, GETSTATIC$char, GETSTATIC$short, GETSTATIC$int, GETSTATIC$float,
                    GETSTATIC$long, GETSTATIC$double, GETSTATIC$reference, GETSTATIC$word, GETSTATIC$byte$init, GETSTATIC$boolean$init, GETSTATIC$char$init, GETSTATIC$short$init, GETSTATIC$int$init,
                    GETSTATIC$float$init, GETSTATIC$long$init, GETSTATIC$double$init, GETSTATIC$reference$init, GETSTATIC$word$init);

    /**
    * Generate all the {@link #GETSTATIC_TEMPLATE_TAGS}.
    */
    public static void generateGetStaticTemplates() {
        for (String k : types) {
            if (hasGetPutTemplates(k)) {
                generateGetStaticTemplate(k);
            }
        }
    }

    /**
     * Generate the resolved and unresolved {@code GETFIELD} template tag for given type.
     * @param k type
     */
    public static void generateGetStaticTemplate(String k) {
        generateAutoComment();
        generateTemplateTag("GETSTATIC$%s", lType(k));
        o.printf("    public static void getstatic%s(ResolutionGuard.InPool guard) {%n", uType(k));
        o.printf("        push%s(resolveAndGetStatic%s(guard));%n", uoType(k), uType(k));
        o.printf("    }%n");
        newLine();

        generateAutoComment();
        o.printf("    @NEVER_INLINE%n");
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


    public static final EnumSet<T1XTemplateTag> LOAD_TEMPLATE_TAGS = EnumSet.of(ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, WLOAD);

    /**
     * Generate all the {@link #LOAD_TEMPLATE_TAGS}.
     */
    public static void generateLoadTemplates() {
        for (String k : types) {
            if (hasLoadStoreTemplates(k)) {
                generateLoadTemplate(k);
            }
        }
    }

    /**
     * Generate the {@code LOAD} template for given type.
     * @param k type
     */
    public static void generateLoadTemplate(String k) {
        generateAutoComment();
        generateTemplateTag("%sLOAD", tagPrefix(k));
        o.printf("    public static void %sload(int dispToLocalSlot) {%n", opPrefix(k));
        o.printf("        %s value = getLocal%s(dispToLocalSlot);%n", oType(k), uoType(k));
        generateBeforeAdvice(k);
        o.printf("        push%s(value);%n", uoType(k));
        o.printf("    }%n");
        newLine();
    }


    public static final EnumSet<T1XTemplateTag> STORE_TEMPLATE_TAGS = EnumSet.of(ISTORE, LSTORE, FSTORE, DSTORE, ASTORE, WSTORE);

    /**
     * Generate all the {@link #STORE_TEMPLATE_TAGS}.
     */
    public static void generateStoreTemplates() {
        for (String k : types) {
            if (hasLoadStoreTemplates(k)) {
                generateStoreTemplate(k);
            }
        }
    }

    /**
     * Generate the {@code STORE} template for given type.
     * @param k type
     */
    public static void generateStoreTemplate(String k) {
        generateAutoComment();
        generateTemplateTag("%sSTORE", tagPrefix(k));
        o.printf("    public static void %sstore(int dispToLocalSlot) {%n", opPrefix(k));
        o.printf("        %s value = pop%s();%n", oType(k), uoType(k));
        generateBeforeAdvice(k);
        o.printf("        setLocal%s(dispToLocalSlot, value);%n", uoType(k));
        o.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> LDC_TEMPLATE_TAGS = EnumSet.of(LDC$int, LDC$long, LDC$float, LDC$double, LDC$reference, LDC$reference$resolved);

    /**
     * Generate all the {@link #LDC_TEMPLATE_TAGS}.
     */
    public static void generateLDCTemplates() {
        for (String k : types) {
            if (hasLDCTemplates(k)) {
                generateLDCTemplate(k);
            }
        }
    }

    /**
     * Generate the {@code LDC} template(s) for given type.
     * @param k type
     */
    public static void generateLDCTemplate(String k) {
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

    public static final EnumSet<T1XTemplateTag> ALOAD_TEMPLATE_TAGS = EnumSet.of(IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD);

    /**
     * Generate all the {@link #ALOAD_TEMPLATE_TAGS}.
     */
    public static void generateArrayLoadTemplates() {
        for (String k : types) {
            if (hasArrayTemplates(k)) {
                generateArrayLoadTemplate(k);
            }
        }
    }

    /**
     * Generate the {@code ALOAD} template(s) for given type.
     * @param k type
     */
    public static void generateArrayLoadTemplate(String k) {
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

    public static final EnumSet<T1XTemplateTag> ASTORE_TEMPLATE_TAGS = EnumSet.of(IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE);

    /**
     * Generate all the {@link #ASTORE_TEMPLATE_TAGS}.
     */
    public static void generateArrayStoreTemplates() {
        for (String k : types) {
            if (hasArrayTemplates(k)) {
                generateArrayStoreTemplate(k);
            }
        }
    }

    /**
     * Generate the {@code ALOAD} template(s) for given type.
     * @param k type
     */
    public static void generateArrayStoreTemplate(String k) {
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

    public static final EnumSet<T1XTemplateTag> IPUSH_TEMPLATE_TAGS = EnumSet.of(BIPUSH, SIPUSH);

    /**
     * Generate all the {@link #IPUSH_TEMPLATE_TAGS).
     */
    public static void generateIPushTemplates() {
        generateIPushTemplate("byte");
        generateIPushTemplate("short");
    }

    /**
     *
     * Generate the {@code IPUSH} template(s) for given type.
     * @param k type
     */
    public static void generateIPushTemplate(String k) {
        generateAutoComment();
        generateTemplateTag("%sIPUSH", tagPrefix(k));
        o.printf("    public static void %sipush(%s value) {%n", opPrefix(k), k);
        generateBeforeAdvice(k);
        o.printf("        pushInt(value);%n");
        o.printf("    }%n");
        newLine();

    }

    public static final EnumSet<T1XTemplateTag> NEW_TEMPLATE_TAGS = EnumSet.of(NEW, NEW$init);

    /**
     * Generate all the {@link #NEW_TEMPLATE_TAGS}.
     */
    public static void generateNewTemplates() {
        generateNewTemplate("");
        generateNewTemplate("init");
    }

    /**
     * Generate the requested {@code NEW} template.
     * @param init if "" generate {@code NEW} template, else {@code NEW$init}.
     */
    public static void generateNewTemplate(String init) {
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

    public static final EnumSet<T1XTemplateTag> NEWARRAY_TEMPLATE_TAGS = EnumSet.of(NEWARRAY);

    public static void generateNewArrayTemplate() {
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

    public static final EnumSet<T1XTemplateTag> ANEWARRAY_TEMPLATE_TAGS = EnumSet.of(ANEWARRAY, ANEWARRAY$resolved);

    /**
     * Generate all the {@link #NEWARRAY_TEMPLATE_TAGS}.
     */
    public static void generateANewArrayTemplates() {
        generateANewArrayTemplate("");
        generateANewArrayTemplate("resolved");
    }

    /**
     * Generate the requested {@code ANEWARRAY} template.
     * @param resolved if "" generate {@code ANEWARRAY} template, else {@code ANEWARRAY$resolved}.
     */
    public static void generateANewArrayTemplate(String resolved) {
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

    public static final EnumSet<T1XTemplateTag> MULTIANEWARRAY_TEMPLATE_TAGS = EnumSet.of(MULTIANEWARRAY, MULTIANEWARRAY$resolved);

    /**
     * Generate all the {@link #MULTINEWARRAY_TEMPLATE_TAGS}.
     */
    public static void generateMultiANewArrayTemplates() {
        generateMultiANewArrayTemplate("");
        generateMultiANewArrayTemplate("resolved");
    }

    /**
     * Generate the requested {@code MULTIANEWARRAY} template.
     * @param resolved if "" generate {@code MULTIANEWARRAY} template, else {@code MULTIANEWARRAY$resolved}.
     */
    public static void generateMultiANewArrayTemplate(String resolved) {
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

    public static final EnumSet<T1XTemplateTag> CHECKCAST_TEMPLATE_TAGS = EnumSet.of(CHECKCAST, CHECKCAST$resolved);

    /**
     * Generate all the {@link #CHECKCAST_TEMPLATE_TAGS}.
     */
    public static void generateCheckcastTemplates() {
        generateCheckcastTemplate("");
        generateCheckcastTemplate("resolved");
    }

    /**
     * Generate the requested {@code CHECKCAST} template.
     * @param resolved if "" generate {@code CHECKCAST} template, else {@code CHECKCAST$resolved}.
     */
    public static void generateCheckcastTemplate(String resolved) {
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

    public static final EnumSet<T1XTemplateTag> INSTANCEOF_TEMPLATE_TAGS = EnumSet.of(INSTANCEOF, INSTANCEOF$resolved);

    /**
     * Generate all the {@link #INSTANCEOF_TEMPLATE_TAGS}.
     */
    public static void generateInstanceofTemplates() {
        generateInstanceofTemplate("");
        generateInstanceofTemplate("resolved");
    }

    /**
     * Generate the requested {@code INSTANCEOF} template.
     * @param resolved if "" generate {@code INSTANCEOF} template, else {@code INSTANCEOF$resolved}.
     */
    public static void generateInstanceofTemplate(String resolved) {
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

    public static void generateArraylengthTemplate() {
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

    public static void generateAThrowTemplate() {
        generateAutoComment();
        generateTemplateTag("ATHROW");
        o.printf("    public static void athrow() {%n");
        o.printf("        Object object = peekObject(0);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        Throw.raise(object);%n");
        o.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> MONITOR_TEMPLATE_TAGS = EnumSet.of(MONITORENTER, MONITOREXIT);

    /**
     * Generate all the {@link #MONITOR_TEMPLATE_TAGS}.
     */
    public  static void generateMonitorTemplates() {
        generateMonitorTemplate("enter");
        generateMonitorTemplate("exit");
    }

    /**
     * Generate the requested {@code MONITOR} template.
     * @param tag one of "enter" or "exit":
     */
    public static void generateMonitorTemplate(String tag) {
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

    public static final EnumSet<T1XTemplateTag> INVOKE_VIRTUAL_TEMPLATE_TAGS = EnumSet.of(INVOKEVIRTUAL$void, INVOKEVIRTUAL$float, INVOKEVIRTUAL$long, INVOKEVIRTUAL$double, INVOKEVIRTUAL$word,
                    INVOKEVIRTUAL$void$resolved, INVOKEVIRTUAL$float$resolved, INVOKEVIRTUAL$long$resolved, INVOKEVIRTUAL$double$resolved, INVOKEVIRTUAL$word$resolved,
                    INVOKEVIRTUAL$void$instrumented, INVOKEVIRTUAL$float$instrumented, INVOKEVIRTUAL$long$instrumented, INVOKEVIRTUAL$double$instrumented, INVOKEVIRTUAL$word$instrumented);

    public static final EnumSet<T1XTemplateTag> INVOKE_INTERFACE_TEMPLATE_TAGS = EnumSet.of(INVOKEINTERFACE$void, INVOKEINTERFACE$float, INVOKEINTERFACE$long, INVOKEINTERFACE$double,
                    INVOKEINTERFACE$word, INVOKEINTERFACE$void$resolved, INVOKEINTERFACE$float$resolved, INVOKEINTERFACE$long$resolved, INVOKEINTERFACE$double$resolved, INVOKEINTERFACE$word$resolved,
                    INVOKEINTERFACE$void$instrumented, INVOKEINTERFACE$float$instrumented, INVOKEINTERFACE$long$instrumented, INVOKEINTERFACE$double$instrumented, INVOKEINTERFACE$word$instrumented);

    /**
     * Generate all the {@link #INVOKE_VIRTUAL_TEMPLATE_TAGS}.
     */
    public static void generateInvokeVirtualTemplates() {
        for (String k : types) {
            if (hasInvokeTemplates(k)) {
                for (String t : new String[] {"", "resolved", "instrumented"}) {
                    generateInvokeVITemplate(k, "virtual", t);
                }
            }
        }
    }

    /**
     * Generate all the {@link #INVOKE_INTERFACE_TEMPLATE_TAGS}.
     */
    public static void generateInvokeInterfaceTemplates() {
        for (String k : types) {
            if (hasInvokeTemplates(k)) {
                for (String t : new String[] {"", "resolved", "instrumented"}) {
                    generateInvokeVITemplate(k, "interface", t);
                }
            }
        }
    }

    /**
     * Generate a specific {@code INVOKE} template.
     * @param k type
     * @param variant one of "virtual" or "interface"
     * @param tag one of "", "resolved" or "instrumented"
     */
    public static void generateInvokeVITemplate(String k, String variant, String tag) {
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


    public static final EnumSet<T1XTemplateTag> INVOKE_STATIC_TEMPLATE_TAGS = EnumSet.of(INVOKESPECIAL$void, INVOKESPECIAL$float, INVOKESPECIAL$long, INVOKESPECIAL$double, INVOKESPECIAL$word,
                    INVOKESPECIAL$void$resolved, INVOKESPECIAL$float$resolved, INVOKESPECIAL$long$resolved, INVOKESPECIAL$double$resolved, INVOKESPECIAL$word$resolved);

    public static final EnumSet<T1XTemplateTag> INVOKE_SPECIAL_TEMPLATE_TAGS = EnumSet.of(INVOKESTATIC$void, INVOKESTATIC$float, INVOKESTATIC$long, INVOKESTATIC$double, INVOKESTATIC$word,
                    INVOKESTATIC$void$init, INVOKESTATIC$float$init, INVOKESTATIC$long$init, INVOKESTATIC$double$init, INVOKESTATIC$word$init);


    /**
     * Generate all the {@link #INVOKE_STATIC_TEMPLATE_TAGS}.
     */
    public static void generateInvokeStaticTemplates() {
        for (String k : types) {
            if (hasInvokeTemplates(k)) {
                for (String t : new String[] {"", "init"}) {
                    generateInvokeSSTemplate(k, "static", t);
                }
            }
        }
    }

    /**
     * Generate all the {@link #INVOKE_SPECIAL_TEMPLATE_TAGS}.
     */
    public static void generateInvokeSpecialTemplates() {
        for (String k : types) {
            if (hasInvokeTemplates(k)) {
                for (String t : new String[] {"", "resolved"}) {
                    generateInvokeSSTemplate(k, "special", t);
                }
            }
        }
    }

    /**
     * Generate a specific {@code INVOKE} template.
     * @param k type
     * @param variant one of "special" or "static"
     * @param tag one of "" or "init" or "resolved"
     */
    public static void generateInvokeSSTemplate(String k, String variant, String xtag) {
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


    public static final EnumSet<T1XTemplateTag> I2_TEMPLATE_TAGS = EnumSet.of(I2L, I2F, I2D);

    /**
     * Generate all the {@link #I2_TEMPLATE_TAGS}.
     */
    public static void generateI2Templates() {
        for (String k : types) {
            if (hasI2Templates(k)) {
                generateI2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code I2} template.
     * @param k target type
     */
    public static void generateI2Template(String k) {
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


    public static final EnumSet<T1XTemplateTag> L2_TEMPLATE_TAGS = EnumSet.of(L2I, L2F, L2D);

    /**
     * Generate all the {@link #L2_TEMPLATE_TAGS}.
     */
    public static void generateL2Templates() {
        for (String k : types) {
            if (hasL2Templates(k)) {
                generateL2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code L2} template.
     * @param k target type
     */
    public static void generateL2Template(String k) {
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


    public static final EnumSet<T1XTemplateTag> D2_TEMPLATE_TAGS = EnumSet.of(D2I, D2L, D2F);

    /**
     * Generate all the {@link #D2_TEMPLATE_TAGS}.
     */
    public static void generateD2Templates() {
        for (String k : types) {
            if (hasD2Templates(k)) {
                generateD2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code D2} template.
     * @param k target type
     */
    public static void generateD2Template(String k) {
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


    public static final EnumSet<T1XTemplateTag> F2_TEMPLATE_TAGS = EnumSet.of(F2I, F2L, F2D);

    /**
     * Generate all the {@link #F2_TEMPLATE_TAGS}.
     */
    public static void generateF2Templates() {
        for (String k : types) {
            if (hasF2Templates(k)) {
                generateF2Template(k);
            }
        }
    }

    /**
     * Generate a given {@code F2} template.
     * @param k target type
     */
    public static void generateF2Template(String k) {
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

    public static final EnumSet<T1XTemplateTag> MOV_TEMPLATE_TAGS = EnumSet.of(MOV_I2F, MOV_F2I, MOV_L2D, MOV_D2L);

    /**
     * Generate all the {@link #MOV_TEMPLATE_TAGS}.
     */
    public static void generateMovTemplates() {
        generateMovTemplate("float", "int");
        generateMovTemplate("int", "float");
        generateMovTemplate("double", "long");
        generateMovTemplate("long", "double");
    }

    /**
     * Generate a given {@code MOV} template.
     * @param from source type
     * @param target type
     */
    public static void generateMovTemplate(String from, String to) {
        generateAutoComment();
        generateTemplateTag("MOV_%s2%s", uType(from).charAt(0), uType(to).charAt(0));
        o.printf("    public static void mov_%s2%s() {%n", from.charAt(0), to.charAt(0));
        o.printf("        %s value = peek%s(0);%n", from, uType(from));
        generateBeforeAdvice(from, to);
        o.printf("        poke%s(0, Intrinsics.%sTo%s(value));%n", uType(to), from, uType(to));
        o.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> POP_TEMPLATE_TAGS = EnumSet.of(POP, POP2);

    /**
     * Generate all {@link #POP_TEMPLATE_TAGS}.
     */
    public static void generatePopTemplates() {
        generatePopTemplate(1);
        generatePopTemplate(2);
    }

    /**
     * Generate specific {@code POP} template.
     * @param arg 1 or 2
     */
    public static void generatePopTemplate(int arg) {
        generateAutoComment();
        final String tag = arg == 1 ? "" : Integer.toString(arg);
        generateTemplateTag("POP%s", tag);
        o.printf("    public static void pop%s() {%n", tag);
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        removeSlots(%d);%n", arg);
        o.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> NEG_TEMPLATE_TAGS = EnumSet.of(INEG, LNEG, FNEG, DNEG);

    /**
     * Generate all {@link #NEG_TEMPLATE_TAGS}.
     */
    public static void generateNegTemplates() {
        for (String k : types) {
            if (hasArithTemplates(k)) {
                generateNegTemplate(k);
            }
        }
    }

    /**
     * Generate specific {@code NEG} template.
     * @param k type
     */
    public static void generateNegTemplate(String k) {
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

    public static final EnumSet<T1XTemplateTag> WDIVREM_TEMPLATE_TAGS = EnumSet.of(WDIV, WDIVI, WREM, WREMI);

    /**
     * Generate all the {@link #WDIVREM_TEMPLATE_TAGS}.
     */
    public static void generateWordDivRemTemplates() {
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
    public static void generateWordDivRemTemplate(String op, String iTag) {
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

    public static final EnumSet<T1XTemplateTag> DYADIC_TEMPLATE_TAGS = EnumSet.of(IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, ISHL, LSHL, ISHR,
                    LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR);

    /**
     * Generate all the {@link #DYADIC_TEMPLATE_TAGS).
     */
    public static void generateDyadicTemplates() {
        for (String k : types) {
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
    public static void generateDyadicTemplate(String k, String op) {
        final boolean forceInt = isShift(op);
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

    /**
     * Generate the {@code IINC} template.
     */
    public static void generateIIncTemplate() {
        generateAutoComment();
        generateTemplateTag("IINC");
        o.printf("    public static void iinc(int dispToLocalSlot, int increment) {%n");
        o.printf("        int value = getLocalInt(dispToLocalSlot);%n");
        generateBeforeAdvice(NULL_ARGS);
        o.printf("        setLocalInt(dispToLocalSlot, value  + increment);%n");
        o.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> RETURN_TEMPLATE_TAGS = EnumSet.of(IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN, IRETURN$unlockClass, IRETURN$unlockReceiver,
                    LRETURN$unlockClass, LRETURN$unlockReceiver, FRETURN$unlockClass, FRETURN$unlockReceiver, DRETURN$unlockClass, DRETURN$unlockReceiver, ARETURN$unlockClass, ARETURN$unlockReceiver,
                    RETURN$unlockClass, RETURN$unlockReceiver, RETURN$registerFinalizer);

    /**
     * Generate all the {@link #RETURN_TEMPLATE_TAGS}.
     */
    public static void generateReturnTemplates() {
        for (String k : types) {
            if (hasReturnTemplates(k)) {
                for (String lockType : lockVariants) {
                    generateReturnTemplate(k, lockType);
                }
            }
        }
        generateReturnTemplate("void", "registerFinalizer");
    }

    /**
     * Generate a specific {@code RETURN} template.
     * @param k type
     * @param unlock one of "", {@link #lockVariants} or "registerFinalizer"
     */
    public static void generateReturnTemplate(String k, String unlock) {
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

    public static final EnumSet<T1XTemplateTag> IFCMP_TEMPLATE_TAGS = EnumSet.of(IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE);

    /**
     * Generate all the {@link #IFCMP_TEMPLATE_TAGS}.
     */
    public static void generateIfCmpTemplates() {
        for (String k : types) {
            if (hasIfCmpTemplates(k)) {
                for (String s : conditions) {
                    if (k.equals("Reference") && !(s.equals("eq") || s.equals("ne"))) {
                        continue;
                    }
                    generateIfCmpTemplate(k, s);
                }
            }
        }
    }

    /**
     * Generate a specific {@link #IFCMP_TEMPLATE_TAGS} template.
     * @param k type
     * @param op one of "eq", "ne", "lt", "ge", "gt", "le"
     */
    public static void generateIfCmpTemplate(String k, String op) {
        generateAutoComment();
        generateTemplateTag("IF_%sCMP%s", tagPrefix(k), op.toUpperCase());
        o.printf("    public static void if_%scmp%s() {%n", opPrefix(k), op);
        generateBeforeAdvice(k);
        o.printf("        %scmp_prefix();%n", opPrefix(k));
        o.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> IF_TEMPLATE_TAGS = EnumSet.of(IFEQ, IFNE, IFLT, IFGE, IFGT);

    /**
     * Generate all the {@link #IF_TEMPLATE_TAGS}.
     *
     */
    public static void generateIfTemplates() {
        for (String s : conditions) {
            generateIfTemplate("int", s);
        }
        for (String s : new String[] {"null", "nonnull"}) {
            generateIfTemplate("Reference", s);
        }
    }

    /**
     * Generate a specific {@link #IF_TEMPLATE_TAGS} template.
     * @param k type one of "int" or "Reference"
     * @param op one of "eq", "ne", "lt", "ge", "gt", "le" ("null" or "nonnull" for k == "Reference")
     */
    public static void generateIfTemplate(String k, String op) {
        generateAutoComment();
        generateTemplateTag("IF%s", op.toUpperCase());
        o.printf("    public static void if%s() {%n", op);
        generateBeforeAdvice(k);
        o.printf("        %scmp0_prefix();%n", opPrefix(k));
        o.printf("    }%n");
        newLine();
    }

    public static final EnumSet<T1XTemplateTag> CMP_TEMPLATE_TAGS = EnumSet.of(LCMP, FCMPL, FCMPG, DCMPL, DCMPG);

    /**
     * Generate all the {@link #CMP_TEMPLATE_TAGS}.
     */
    public static void generateCmpTemplates() {
        generateCmpTemplate("long", "");
        for (String k : new String[] {"float", "double"}) {
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
    public static void generateCmpTemplate(String k, String variant) {
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

    public static final EnumSet<T1XTemplateTag> PSET_TEMPLATE_TAGS = EnumSet.of(PSET_BYTE, PSET_SHORT, PSET_INT, PSET_FLOAT, PSET_LONG, PSET_DOUBLE, PSET_WORD, PSET_REFERENCE);

    /**
     * Generate all the {@link #PSET_TEMPLATE_TAGS}.
     */
    public static void generatePSetTemplates() {
        for (String k : types) {
            if (hasPTemplates(k)) {
                generatePSetTemplate(k);
            }
        }
    }

    /**
     * Generate a specific {@link #PSET_TEMPLATE_TAGS) template.
     * @param k type
     */
    public static void generatePSetTemplate(String k) {
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

    public static final EnumSet<T1XTemplateTag> PGET_TEMPLATE_TAGS = EnumSet.of(PGET_BYTE, PGET_CHAR, PGET_SHORT, PGET_INT, PGET_FLOAT, PGET_LONG, PGET_DOUBLE, PGET_WORD, PGET_REFERENCE);

    /**
     * Generate all the {@link #PGET_TEMPLATE_TAGS}.
     */
    public static void generatePGetTemplates() {
        for (String k : types) {
            if (hasPTemplates(k)) {
                generatePGetTemplate(k);
            }
        }
    }

    /**
     * Generate a specific {@link #PSET_TEMPLATE_TAGS) template.
     * @param k type
     */
    public static void generatePGetTemplate(String k) {
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

    public static final EnumSet<T1XTemplateTag> PREAD_TEMPLATE_TAGS = EnumSet.of(PREAD_BYTE, PREAD_CHAR, PREAD_SHORT, PREAD_INT, PREAD_FLOAT, PREAD_LONG, PREAD_DOUBLE, PREAD_WORD, PREAD_REFERENCE,
                    PREAD_BYTE_I, PREAD_CHAR_I, PREAD_SHORT_I, PREAD_INT_I, PREAD_FLOAT_I, PREAD_LONG_I, PREAD_DOUBLE_I, PREAD_WORD_I, PREAD_REFERENCE_I);

    /**
     * Generate all the {@link #PREAD_TEMPLATE_TAGS}.
     */
    public static void generatePReadTemplates() {
        for (String k : types) {
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
    public static void generatePReadTemplate(String k, boolean isI) {
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

    public static final EnumSet<T1XTemplateTag> PWRITE_TEMPLATE_TAGS = EnumSet.of(PWRITE_BYTE, PWRITE_SHORT, PWRITE_INT, PWRITE_FLOAT, PWRITE_LONG, PWRITE_DOUBLE, PWRITE_WORD, PWRITE_REFERENCE,
                    PWRITE_BYTE_I, PWRITE_SHORT_I, PWRITE_INT_I, PWRITE_FLOAT_I, PWRITE_LONG_I, PWRITE_DOUBLE_I, PWRITE_WORD_I, PWRITE_REFERENCE_I);

    /**
     * Generate all the {@link #PWRITE_TEMPLATE_TAGS}.
     */
    public static void generatePWriteTemplates() {
        for (String k : types) {
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
    public static void generatePWriteTemplate(String k, boolean isI) {
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

    public static final EnumSet<T1XTemplateTag> PCMPSWP_TEMPLATE_TAGS = EnumSet.of(PCMPSWP_INT, PCMPSWP_WORD, PCMPSWP_REFERENCE, PCMPSWP_INT_I, PCMPSWP_WORD_I, PCMPSWP_REFERENCE_I);

    /**
     * Generate all the {@link #PCMPSWP_TEMPLATE_TAGS}.
     */
    public static void generatePCmpSwpTemplates() {
        for (String k : types) {
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
    public static void generatePCmpSwpTemplate(String k, boolean isI) {
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
     * Generate the complete template source code using the provided {@link AdviceHook} to the standard output.
     * The order of the output is first the typed operations, per type, followed by the untyped operations.
     * @param hook the advice hook or {@code null} if no advice
     */
    public static void generateAll(AdviceHook hook) {
        adviceHook = hook;
        for (String k : types) {
            // constants
            if (k.equals("int")) {
                for (String s : new String[] {"0", "1", "2", "3", "4", "5", "M1"}) {
                    generateShortConstTemplate(k, s);
                }
            } else if (k.equals("Reference")) {
                generateShortConstTemplate(k, "NULL");
            } else if (k.equals("Word")) {
                generateShortConstTemplate(k, "0");
            } else if (k.equals("float") || k.equals("double") || k.equals("long")) {
                generateConstTemplate(k);
            }
            if (hasGetPutTemplates(k)) {
                generateGetFieldTemplate(k);
                generateGetStaticTemplate(k);
                generatePutFieldTemplate(k);
                generatePutStaticTemplate(k);
            }
            if (hasLoadStoreTemplates(k)) {
                generateLoadTemplate(k);
                generateStoreTemplate(k);
            }
            if (hasLDCTemplates(k)) {
                generateLDCTemplate(k);
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
            if (hasIfCmpTemplates(k)) {
                for (String s : conditions) {
                    if (k.equals("Reference") && !(s.equals("eq") || s.equals("ne"))) {
                        continue;
                    }
                    generateIfCmpTemplate(k, s);
                    if (!k.equals("Reference")) {
                        generateIfTemplate(k, s);
                    }
                }
                if (k.equals("Reference")) {
                    generateIfTemplate(k, "null");
                    generateIfTemplate(k, "nonnull");
                }
            }
            if (hasCmpTemplates(k)) {
                if (k.equals("long")) {
                    generateCmpTemplate(k, "");
                } else {
                    for (String s : new String[] {"g", "l"}) {
                        generateCmpTemplate(k, s);
                    }
                }
            }
            if (hasReturnTemplates(k)) {
                for (String lockType : lockVariants) {
                    generateReturnTemplate(k, lockType);
                }
            }
            if (hasArrayTemplates(k)) {
                generateArrayLoadTemplate(k);
                generateArrayStoreTemplate(k);
            }
            if (hasInvokeTemplates(k))  {
                for (String s : new String[] {"virtual", "interface"}) {
                    for (String t : new String[] {"", "resolved", "instrumented"}) {
                        generateInvokeVITemplate(k, s, t);
                    }
                }
                for (String s : new String[] {"special", "static"}) {
                    for (String t : new String[] {"", "resolved"}) {
                        generateInvokeSSTemplate(k, s, t);
                    }
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
        generateIIncTemplate();
        generateMovTemplates();
        generateIPushTemplates();
        generatePopTemplates();
        generateNewTemplates();
        generateNewArrayTemplate();
        generateANewArrayTemplates();
        generateMultiANewArrayTemplates();
        generateCheckcastTemplates();
        generateArraylengthTemplate();
        generateAThrowTemplate();
        generateMonitorTemplates();
        generateInstanceofTemplates();
        generateReturnTemplate("void", "registerFinalizer");


    }

    public static void main(String[] args) {
        generateAll(null);
    }

}

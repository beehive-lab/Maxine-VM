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
package com.sun.max.asm.gen;

import com.sun.max.asm.*;
import com.sun.max.collect.*;

/**
 * An expression can provide the value of an operand field. This enables synthetic instructions to be generated where
 * the parameters of the generated assembler method are part of an expression whose value is encoded into an operand
 * field.
 * 
 * @author Doug Simon
 * @author Sumeet Panchal
 */
public interface Expression {

    /**
     * Evaluates the expression given a template and a set of arguments.
     */
    long evaluate(Template template, IndexedSequence<Argument> arguments);

    /**
     * @return a Java expression that performs the {@link #evaluate evaluation}
     */
    String valueString();

    public static final class Static {

        private Static() {
        }

        /**
         * Evaluates a given expression term to a {@code long} value.
         * 
         * @param term
         *            a {@link Number}, {@link Expression} or {@link Parameter} instance
         */
        public static long evaluateTerm(Object term, Template template, IndexedSequence<Argument> arguments) {
            if (term instanceof Number) {
                return ((Number) term).longValue();
            }
            if (term instanceof Expression) {
                return ((Expression) term).evaluate(template, arguments);
            }
            assert term instanceof Parameter;
            return template.bindingFor((Parameter) term, arguments).asLong();
        }

        /**
         * Gets the Java source code representation of a given expression term.
         * 
         * @param term
         *            a {@link Number}, {@link Expression} or {@link Parameter} instance
         */
        public static String termValueString(Object term) {
            if (term instanceof Parameter) {
                return ((Parameter) term).valueString();
            }
            if (term instanceof Expression) {
                return "(" + ((Expression) term).valueString() + ")";
            }
            assert term instanceof Number;
            return term.toString();
        }

        /**
         * Creates and returns an expression that adds its two terms.
         * 
         * @param first
         *            the first term of the addition
         * @param second
         *            the second term of the addition
         */
        public static Expression add(final Object first, final Object second) {
            return new Expression() {

                public long evaluate(Template template, IndexedSequence<Argument> arguments) {
                    return evaluateTerm(first, template, arguments) + evaluateTerm(second, template, arguments);
                }

                public String valueString() {
                    return termValueString(first) + " + " + termValueString(second);
                }
            };
        }

        /**
         * Creates and returns an expression that subtracts its second term from its first term.
         * 
         * @param first
         *            the first term of the subtraction
         * @param second
         *            the second term of the subtraction
         */
        public static Expression sub(final Object first, final Object second) {
            return new Expression() {

                public long evaluate(Template template, IndexedSequence<Argument> arguments) {
                    return evaluateTerm(first, template, arguments) - evaluateTerm(second, template, arguments);
                }

                public String valueString() {
                    return termValueString(first) + " - " + termValueString(second);
                }
            };
        }

        /**
         * Creates and returns an expression that negates its term.
         * 
         * @param term
         *            the term of the negation
         */
        public static Expression neg(final Object term) {
            return new Expression() {

                public long evaluate(Template template, IndexedSequence<Argument> arguments) {
                    return -evaluateTerm(term, template, arguments);
                }

                public String valueString() {
                    return "-" + termValueString(term);
                }
            };
        }

        /**
         * Creates and returns an expression that is term1/term2.
         * 
         * @param term1
         *            dividend
         * 
         * @param term2
         *            divider
         */
        public static Expression div(final Object term1, final Object term2) {
            return new Expression() {

                public long evaluate(Template template, IndexedSequence<Argument> arguments) {
                    return evaluateTerm(term1, template, arguments) / evaluateTerm(term2, template, arguments);
                }

                public String valueString() {
                    return termValueString(term1) + " / " + termValueString(term2);
                }
            };
        }

        /**
         * Creates and returns an expression that is term1 % term2.
         * 
         */
        public static Expression mod(final Object term1, final Object term2) {
            return new Expression() {

                public long evaluate(Template template, IndexedSequence<Argument> arguments) {
                    return evaluateTerm(term1, template, arguments) % evaluateTerm(term2, template, arguments);
                }

                public String valueString() {
                    return termValueString(term1) + " % " + termValueString(term2);
                }
            };
        }

        /**
         * Creates and returns an expression that is term1 >> term2.
         * 
         */
        public static Expression rightShift(final Object term1, final Object term2) {
            return new Expression() {

                public long evaluate(Template template, IndexedSequence<Argument> arguments) {
                    return evaluateTerm(term1, template, arguments) >> evaluateTerm(term2, template, arguments);
                }

                public String valueString() {
                    return termValueString(term1) + " >> " + termValueString(term2);
                }
            };
        }

        /**
         * Creates and returns an expression that is term1 & term2.
         * 
         */
        public static Expression and(final Object term1, final Object term2) {
            return new Expression() {

                public long evaluate(Template template, IndexedSequence<Argument> arguments) {
                    return evaluateTerm(term1, template, arguments) & evaluateTerm(term2, template, arguments);
                }

                public String valueString() {
                    return "(" + termValueString(term1) + " & " + termValueString(term2) + ")";
                }
            };
        }
    }
}

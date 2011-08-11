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
package com.sun.max.tele.debug;

import static java.io.StreamTokenizer.*;

import java.io.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * Simple conditional breakpoints.
 */
public class BreakpointCondition extends AbstractTeleVMHolder implements VMTriggerEventHandler {

    private String condition;
    private StreamTokenizer streamTokenizer;
    private TeleIntegerRegisters integerRegisters;
    private static Map<String, CiRegister> integerRegisterSymbols;
    private Expression expression;

    public BreakpointCondition(TeleVM teleVM, String condition) throws ExpressionException {
        super(teleVM);
        this.condition = condition;
        if (integerRegisterSymbols == null) {
            integerRegisterSymbols = new HashMap<String, CiRegister>();
            for (CiRegister reg : TeleIntegerRegisters.getIntegerRegisters()) {
                integerRegisterSymbols.put(reg.name.toUpperCase(), reg);
                integerRegisterSymbols.put(reg.name.toLowerCase(), reg);
            }
        }
        this.expression = parse();
    }

    public boolean handleTriggerEvent(TeleNativeThread teleNativeThread) {
        return evaluate(vm().teleProcess(), teleNativeThread);
    }

    public boolean evaluate(TeleProcess teleProcess, TeleNativeThread teleNativeThread) {
        if (expression == null) {
            return false;
        }
        integerRegisters = teleNativeThread.registers().teleIntegerRegisters();
        if (integerRegisters == null) {
            return false;
        }
        try {
            final Expression result = expression.evaluate();
            if (result instanceof BooleanExpression) {
                return ((BooleanExpression) result).value();
            }
            return false;
        } catch (ExpressionException ex) {
            return false;
        }
    }

    private void initializeTokenizer() {
        streamTokenizer = new StreamTokenizer(new StringReader(condition));
        streamTokenizer.ordinaryChars('0', '9');
        streamTokenizer.wordChars('0', '9');
    }

    Expression parse()  throws ExpressionException {
        try {
            initializeTokenizer();
            streamTokenizer.nextToken();
            final Expression result = parseExpression();
            // Must be empty or a BinaryExpr
            if (result == null || result instanceof BinaryExpression) {
                return result;
            }
            throw new ExpressionException("expression must return boolean result");
        } catch (IOException ex) {
            throw new ExpressionException("syntax error");
        }
    }

    // CheckStyle: stop inner assignment checks

    Expression parseExpression() throws ExpressionException, IOException {
        Expression result = null;
        while (streamTokenizer.ttype != TT_EOF) {
            Operator op;
            Expression expression = null;
            if (streamTokenizer.ttype == '(') {
                streamTokenizer.nextToken();
                expression = parseExpression();
                streamTokenizer.nextToken();
                expectChar(')');
            } else if (streamTokenizer.ttype == '[') {
                expression = expectMemory();
            } else if ((expression = isRegister()) != null) {
               //
            } else if ((expression = isNumber()) != null) {
                //
            } else if ((op = isBinaryOperator()) != null) {
                if (result == null) {
                    throw new ExpressionException("no left operand for binary operator");
                }
                streamTokenizer.nextToken();
                expression = new BinaryExpression(op, result, parseExpression());
            }
            result = expression;
            streamTokenizer.nextToken();
        }
        return result;
    }

    // CheckStyle: resume inner assignment checks

    private RegisterExpression isRegister() {
        if (streamTokenizer.ttype == TT_WORD) {
            final CiRegister register = integerRegisterSymbols.get(streamTokenizer.sval);
            if (register != null) {
                return new RegisterExpression(register);
            }
        }
        return null;
    }

    private Operator isBinaryOperator() throws IOException, ExpressionException {
        switch (streamTokenizer.ttype) {
            case '+':
                return Operator.PLUS;
            case '-':
                return Operator.MINUS;
            case '=':
                streamTokenizer.nextToken();
                if (streamTokenizer.ttype == '=') {
                    return Operator.EQ;
                }
                streamTokenizer.pushBack();
                break;
            case '!':
                streamTokenizer.nextToken();
                if (streamTokenizer.ttype == '=') {
                    return Operator.NE;
                }
                streamTokenizer.pushBack();
                break;
            case '>':
                streamTokenizer.nextToken();
                if (streamTokenizer.ttype == '=') {
                    return Operator.GE;
                }
                streamTokenizer.pushBack();
                return Operator.GT;
            case '<':
                streamTokenizer.nextToken();
                if (streamTokenizer.ttype == '=') {
                    return Operator.LE;
                }
                streamTokenizer.pushBack();
                return Operator.LT;
            case '&':
                streamTokenizer.nextToken();
                if (streamTokenizer.ttype == '&') {
                    return Operator.LOGAND;
                }
                streamTokenizer.pushBack();
                break;
            case '|':
                streamTokenizer.nextToken();
                if (streamTokenizer.ttype == '|') {
                    return Operator.LOGOR;
                }
                streamTokenizer.pushBack();
                break;
            default:
                break;
        }
        return null;
    }

    private Expression expectMemory() throws ExpressionException, IOException {
        if (streamTokenizer.ttype == '[') {
            Expression result = null;
            streamTokenizer.nextToken();
            final Expression addressExpression = expectRegisterOrNumber();
            streamTokenizer.nextToken();
            final Operator op = isBinaryOperator();
            if (op != null) {
                if (op == Operator.PLUS || op == Operator.MINUS) {
                    streamTokenizer.nextToken();
                    final Expression addressOffsetExpression = expectRegisterOrNumber();
                    if (addressExpression  instanceof RegisterExpression && addressOffsetExpression instanceof NumberExpression) {
                        result = new OffsetRegisterMemoryExpression((RegisterExpression) addressExpression, op, (NumberExpression) addressOffsetExpression);
                    } else if (addressOffsetExpression  instanceof RegisterExpression && addressExpression instanceof NumberExpression) {
                        result = new OffsetRegisterMemoryExpression((RegisterExpression) addressOffsetExpression, op, (NumberExpression) addressExpression);
                    } else if (addressOffsetExpression  instanceof NumberExpression && addressExpression instanceof NumberExpression) {
                        Address address1 = Address.fromLong(((NumberExpression) addressExpression).value());
                        final Address address2 = Address.fromLong(((NumberExpression) addressOffsetExpression).value());
                        switch (op) {
                            case PLUS:
                                address1 = address1.plus(address2);
                                break;
                            case MINUS:
                                address1 = address1.minus(address2);
                                break;
                            default:
                                throw new ExpressionException("illegal operator in memory expression");
                        }
                        result = new AddressMemoryExpression(address1);
                    } else {
                        throw new ExpressionException("illegal memory expression");
                    }
                    streamTokenizer.nextToken();
                } else {
                    streamTokenizer.pushBack();
                }
            } else {
                if (addressExpression instanceof RegisterExpression) {
                    result = new RegisterMemoryExpression((RegisterExpression) addressExpression);
                } else {
                    result = new AddressMemoryExpression(Address.fromLong(((NumberExpression) addressExpression).value()));
                }
            }
            expectChar(']');
            return result;
        }
        throw new ExpressionException("syntax error: [ expected");
    }

    private void expectChar(char ch) throws ExpressionException {
        if (streamTokenizer.ttype != ch) {
            throw new ExpressionException("syntax error: " + ch + " expected");
        }
    }

    private Expression expectRegisterOrNumber() throws ExpressionException {
        Expression result = isRegister();
        if (result != null) {
            return result;
        }
        result = isNumber();
        if (result != null) {
            return result;
        }
        throw new ExpressionException("register or number expected");
    }

    private Expression isNumber() {
        if (streamTokenizer.ttype == TT_WORD) {
            final int length = streamTokenizer.sval.length();
            if (length > 0) {
                final char firstChar = streamTokenizer.sval.charAt(0);
                if ((firstChar >= '0') && (firstChar <= '9')) {
                    final boolean isHex = (firstChar == '0') && (length > 1 && Character.toUpperCase(streamTokenizer.sval.charAt(1)) == 'X');
                    final int radix = isHex ? 16 : 10;
                    String toParse = streamTokenizer.sval;
                    if (isHex) {
                        toParse = toParse.substring(2);
                    }
                    return new NumberExpression(Long.parseLong(toParse, radix));
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return condition;
    }

    public static class ExpressionException extends Exception {
        ExpressionException(String s) {
            super(s);
        }
    }

    abstract static class Expression {
        private static final String ILLEGAL_OPERATION = "illegal operator for type";
        abstract Expression evaluate() throws ExpressionException;

        boolean lt(Object other) throws ExpressionException {
            throw new ExpressionException(ILLEGAL_OPERATION);
        }

        boolean le(Object other) throws ExpressionException {
            throw new ExpressionException(ILLEGAL_OPERATION);
        }

        boolean ge(Object other) throws ExpressionException {
            throw new ExpressionException(ILLEGAL_OPERATION);
        }

        boolean gt(Object other) throws ExpressionException {
            throw new ExpressionException(ILLEGAL_OPERATION);
        }
    }

    class BinaryExpression extends Expression {
        private Operator operator;
        private Expression left;
        private Expression right;

        BinaryExpression(Operator operator, Expression left, Expression right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        Operator operator() {
            return operator;
        }

        Expression left() {
            return left;
        }

        Expression right() {
            return right;
        }

        @Override
        Expression evaluate()  throws ExpressionException {
            final Expression leftValue = left.evaluate();
            final Expression rightValue = right.evaluate();

            switch (operator) {
                case LT:
                    return new BooleanExpression(leftValue.lt(rightValue));
                case LE:
                    return new BooleanExpression(leftValue.le(rightValue));
                case EQ:
                    return new BooleanExpression(leftValue.equals(rightValue));
                case NE:
                    return new BooleanExpression(!leftValue.equals(rightValue));
                case GE:
                    return new BooleanExpression(leftValue.ge(rightValue));
                case GT:
                    return new BooleanExpression(leftValue.gt(rightValue));
                case LOGAND:
                case LOGOR:
                    final BooleanExpression leftBoolean = checkBoolean(leftValue);
                    final BooleanExpression rightBoolean = checkBoolean(rightValue);
                    if (operator == Operator.LOGAND) {
                        return new BooleanExpression(leftBoolean.value() && rightBoolean.value());
                    }
                    return new BooleanExpression(leftBoolean.value() || rightBoolean.value());
                default:
            }
            return new BooleanExpression(false);
        }

        private BooleanExpression checkBoolean(Expression expr) throws ExpressionException {
            if (expr instanceof BooleanExpression) {
                return (BooleanExpression) expr;
            }
            throw new ExpressionException("operands must be boolean");
        }
    }

    class RegisterExpression extends Expression {
        CiRegister register;

        RegisterExpression(CiRegister register) {
            this.register = register;
        }

        CiRegister register() {
            return register;
        }

        @Override
        Expression evaluate() {
            return new NumberExpression(integerRegisters.getValue(register).toLong());
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof RegisterExpression) {
                final RegisterExpression otherNumber = (RegisterExpression) other;
                return evaluate().equals(otherNumber.evaluate());
            }
            return false;
        }
    }

    class NumberExpression extends Expression {
        long value;

        NumberExpression(long value) {
            this.value = value;
        }

        long value() {
            return value;
        }

        @Override
        Expression evaluate() {
            return this;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof NumberExpression) {
                final NumberExpression otherNumber    = (NumberExpression) other;
                return otherNumber.value == value;
            }
            return false;
        }

        @Override
        boolean lt(Object other) {
            if (other instanceof NumberExpression) {
                final NumberExpression otherNumber    = (NumberExpression) other;
                return otherNumber.value < value;
            }
            return false;
        }

        @Override
        boolean le(Object other) {
            if (other instanceof NumberExpression) {
                final NumberExpression otherNumber    = (NumberExpression) other;
                return otherNumber.value <= value;
            }
            return false;
        }

        @Override
        boolean gt(Object other) {
            if (other instanceof NumberExpression) {
                final NumberExpression otherNumber    = (NumberExpression) other;
                return otherNumber.value > value;
            }
            return false;
        }

        @Override
        boolean ge(Object other) {
            if (other instanceof NumberExpression) {
                final NumberExpression otherNumber    = (NumberExpression) other;
                return otherNumber.value >= value;
            }
            return false;
        }
    }

    static class BooleanExpression extends Expression {
        boolean value;

        BooleanExpression(boolean value) {
            this.value = value;
        }

        boolean value() {
            return value;
        }

        @Override
        Expression evaluate() {
            return this;
        }
    }

    abstract static class MemoryExpression extends Expression {
    }

    class AddressMemoryExpression extends MemoryExpression {
        Address address;

        AddressMemoryExpression(Address address) {
            this.address = address;
        }

        void setAddress(Address address) {
            this.address = address;
        }

        Address address() {
            return address;
        }

        @Override
        Expression evaluate() {
            return new NumberExpression(vm().dataAccess().readLong(address));
        }
    }

    class RegisterMemoryExpression extends MemoryExpression {
        RegisterExpression registerExpression;
        AddressMemoryExpression addressMemoryExpression = new AddressMemoryExpression(Address.zero());

        RegisterMemoryExpression(RegisterExpression registerExpression) {
            this.registerExpression = registerExpression;
        }

        @Override
        Expression evaluate() {
            final NumberExpression addressExpression = (NumberExpression) registerExpression.evaluate();
            addressMemoryExpression.setAddress(Address.fromLong(addressExpression.value()));
            return addressMemoryExpression.evaluate();
        }
    }

    class OffsetRegisterMemoryExpression extends RegisterMemoryExpression {
        int offset;
        Operator operator;

        OffsetRegisterMemoryExpression(RegisterExpression registerExpression, Operator operator, NumberExpression offset) {
            super(registerExpression);
            this.offset = (int) offset.value();
            this.operator = operator;
        }

        @Override
        Expression evaluate() {
            final NumberExpression addressExpression = (NumberExpression) registerExpression.evaluate();
            Address address = Address.fromLong(addressExpression.value());
            switch (operator) {
                case PLUS:
                    address = address.plus(offset);
                    break;
                case MINUS:
                    address = address.minus(offset);
                    break;
                default:
            }
            return new AddressMemoryExpression(address).evaluate();
        }
    }

    enum Operator {
        LT,
        LE,
        EQ,
        NE,
        GE,
        GT,
        PLUS,
        MINUS,
        LOGAND,
        LOGOR
    }
}

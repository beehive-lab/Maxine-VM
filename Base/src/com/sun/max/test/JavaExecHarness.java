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
package com.sun.max.test;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;

public class JavaExecHarness implements TestHarness<JavaExecHarness.JavaTestCase> {

    private static final char SQUOTE = '\'';
    private static final char BACKSLASH = '\\';
    private static final char QUOTE = '"';
    private static final String ESCAPED_QUOTE = "\\\"";

    private final Executor _executor;

    public interface Executor {
        void initialize(JavaTestCase testCase, boolean loadingPackages);

        Object execute(JavaTestCase c, Object[] vals) throws InvocationTargetException;
    }

    public JavaExecHarness(Executor exec) {
        _executor = exec;
    }

    public static class Run {
        public final Object[] _input;
        public final Object _expectedValue;
        public final Class<? extends Throwable> _expectedException;

        Object _returnVal;
        Throwable _returnExc;
        Throwable _thrown;

        Run(Object[] input, Object expected, Class<? extends Throwable> expectedException) {
            _input = input;
            _expectedValue = expected;
            _expectedException = expectedException;
        }
    }

    public class JavaTestCase extends TestCase {
        public final Class _class;
        public final List<Run> _runs;
        public final Executor _exec;
        public final String _execName;

        public Object _slot1;
        public Object _slot2;

        protected JavaTestCase(String execName, Executor executor, File file, Class testClass, List<Run> runs, boolean loadingPackages) {
            super(JavaExecHarness.this, file, null);
            _execName = execName;
            _exec = executor;
            _runs = runs;
            _class = testClass;
            executor.initialize(this, loadingPackages);
        }

        @Override
        public void run() throws Throwable {
            for (Run run : _runs) {
                try {
                    run._returnVal = _exec.execute(this, run._input);
                } catch (InvocationTargetException t) {
                    run._returnExc = t.getTargetException();
                } catch (Throwable t) {
                    run._thrown = t;
                }
            }
        }
    }

    public static class ExecFailure extends TestResult.Failure {
        protected final Run _run;
        protected final String _result;
        protected final String _expect;
        protected ExecFailure(Run run, String result) {
            _run = run;
            this._expect = resultToString(_run._expectedValue, _run._expectedException);
            this._result = result;
        }
        @Override
        public String failureMessage(TestCase testCase) {
            final JavaTestCase javaTestCase = (JavaTestCase) testCase;
            return inputToString(javaTestCase._class, _run, false) + " failed with " + _result + " (expected " + _expect + ")";
        }

    }

    public TestResult evaluateTest(TestEngine engine, JavaTestCase testCase) {
        if (testCase._thrown != null) {
            return new TestResult.UnexpectedException(testCase._thrown);
        }
        for (Run run : testCase._runs) {
            if (run._thrown != null) {
                return new ExecFailure(run, "unexpected " + run._thrown.getClass().getName());
            }
            final String result = valueToString(run._returnVal, run._returnExc);
            if (run._expectedException != null) {
                if (run._returnExc == null || run._returnExc.getClass() != run._expectedException) {
                    return new ExecFailure(run, result);
                }
            } else if (run._returnExc != null) {
                return new ExecFailure(run, result);
            } else if (run._expectedValue == null) {
                if (run._returnVal != null) {
                    return new ExecFailure(run, result);
                }
            } else if (!run._expectedValue.equals(run._returnVal)) {
                return new ExecFailure(run, result);
            }
        }
        return TestResult.SUCCESS;
    }

    public void parseTests(TestEngine engine, File file, Properties props) {
        try {
            // 1. find the class
            final Class testClass = findClass(file, props);
            // 2. parse the runs
            final List<Run> runs = parseRuns(testClass, file, props);
            if (runs != null) {
                // 3. add a test case to the engine
                engine.addTest(new JavaTestCase("exec", _executor, file, testClass, runs, engine.loadingPackages()));
            } else {
                engine.skipFile(file);
            }
        } catch (Exception e1) {
            ProgramError.unexpected(e1);
        }
    }

    private Class findClass(File file, Properties props) throws Exception {
        final BufferedReader r = new BufferedReader(new FileReader(file));

        // search for the package statement in the file.
        for (String line = r.readLine().trim(); line != null; line = r.readLine().trim()) {
            if (line.startsWith("package")) {
                r.close();
                int indx = line.indexOf(' ');
                while (line.charAt(indx) == ' ') {
                    indx++;
                }
                final String packageName = line.substring(indx, line.indexOf(';'));
                String className = file.getName();
                indx = className.indexOf(".java");
                if (indx >= 0) {
                    className = className.substring(0, indx);
                }
                // use the package name plus the name of the file to load the class.
                return Class.forName(packageName + "." + className);
            }
        }
        r.close();
        throw ProgramError.unexpected("could not find package statement in " + file);
    }

    private List<Run> parseRuns(Class testClass, File file, Properties props) {
        final String rstr = props.getProperty("Runs");
        if (rstr == null) {
            return null;
        }
        final List<Run> runs = new LinkedList<Run>();
        final CharacterIterator i = new StringCharacterIterator(rstr);
        while (i.getIndex() < i.getEndIndex()) {
            runs.add(parseRun(i));
            if (!skipPeekAndEat(i, ';')) {
                break;
            }
        }
        return runs;
    }

    private Run parseRun(CharacterIterator iterator) {
        // parses strings of the form:
        // ()=value
        // (value,...)=result
        // value=result
        Object[] vals = new Object[1];
        if (skipPeekAndEat(iterator, '(')) {
            final List<Object> inputValues = new LinkedList<Object>();
            if (!skipPeekAndEat(iterator, ')')) {
                while (true) {
                    inputValues.add(parseValue(iterator));
                    if (!skipPeekAndEat(iterator, ',')) {
                        break;
                    }
                }
                skipPeekAndEat(iterator, ')');
            }
            vals = inputValues.toArray(vals);
        } else {
            vals[0] = parseValue(iterator);
        }
        skipPeekAndEat(iterator, '=');
        if (skipPeekAndEat(iterator, '!')) {
            return new Run(vals, null, parseException(iterator));
        }
        return new Run(vals, parseValue(iterator), null);
    }

    private Object parseValue(CharacterIterator iterator) {
        // parses strings of the form:
        // <integer> | <long> | <string> | true | false | null
        skip(iterator);
        if (iterator.current() == '-' || Character.isDigit(iterator.current())) {
            // parse a number.
            return parseNumber(iterator);
        } else if (iterator.current() ==  QUOTE) {
            // a string constant.
            return parseStringLiteral(iterator);
        } else if (peekAndEat(iterator, "true")) {
            // the boolean value (true)
            return Boolean.valueOf(true);
        } else if (peekAndEat(iterator, "false")) {
            // the boolean value (false)
            return Boolean.valueOf(false);
        } else if (peekAndEat(iterator, "null")) {
            // the null value (null)
            return null;
        }
        throw ProgramError.unexpected("invalid value at " + iterator.getIndex());
    }

    private Object parseNumber(CharacterIterator iterator) {
        // an integer.
        final StringBuffer buf = new StringBuffer();
        buf.append(iterator.current());
        iterator.next();
        appendDigits(buf, iterator);
        if (peekAndEat(iterator, '.')) {
            // parse the fractional suffix of a float or double
            buf.append('.');
            appendDigits(buf, iterator);
            if (peekAndEat(iterator, 'f') || peekAndEat(iterator, "F")) {
                return Float.valueOf(buf.toString());
            }
            if (peekAndEat(iterator, 'd') || peekAndEat(iterator, "D")) {
                return Double.valueOf(buf.toString());
            }
            return Float.valueOf(buf.toString());
        }
        if (peekAndEat(iterator, 'f') || peekAndEat(iterator, "F")) {
            return Float.valueOf(buf.toString());
        }
        if (peekAndEat(iterator, 'd') || peekAndEat(iterator, "D")) {
            return Double.valueOf(buf.toString());
        }
        if (peekAndEat(iterator, 'l') || peekAndEat(iterator, "L")) {
            return Long.valueOf(buf.toString());
        }
        if (peekAndEat(iterator, 's') || peekAndEat(iterator, "S")) {
            return Short.valueOf(buf.toString());
        }
        if (peekAndEat(iterator, 'b') || peekAndEat(iterator, "B")) {
            return Byte.valueOf(buf.toString());
        }
        if (peekAndEat(iterator, 'c') || peekAndEat(iterator, "C")) {
            return Character.valueOf((char) Integer.valueOf(buf.toString()).intValue());
        }
        return Integer.valueOf(buf.toString());
    }

    private void appendDigits(final StringBuffer buf, CharacterIterator iterator) {
        while (Character.isDigit(iterator.current())) {
            buf.append(iterator.current());
            iterator.next();
        }
    }

    private Class<? extends Throwable> parseException(CharacterIterator iterator) {
        final StringBuffer buf = new StringBuffer();
        while (true) {
            final char ch = iterator.current();
            if (Character.isJavaIdentifierPart(ch) || ch == '.') {
                buf.append(ch);
                iterator.next();
            } else {
                break;
            }
        }
        try {
            return Class.forName(buf.toString()).asSubclass(Throwable.class);
        } catch (ClassNotFoundException e) {
            throw ProgramError.unexpected("unknown exception " + buf.toString());
        }
    }

    private boolean skipPeekAndEat(CharacterIterator iterator, char c) {
        skip(iterator);
        return peekAndEat(iterator, c);
    }

    private boolean peekAndEat(CharacterIterator iterator, char c) {
        if (iterator.current() == c) {
            iterator.next();
            return true;
        }
        return false;
    }

    private boolean peekAndEat(CharacterIterator iterator, String string) {
        final int indx = iterator.getIndex();
        for (int j = 0; j < string.length(); j++) {
            if (iterator.current() != string.charAt(j)) {
                iterator.setIndex(indx);
                return false;
            }
            iterator.next();
        }
        return true;
    }

    private void skip(CharacterIterator iterator) {
        while (true) {
            if (!Character.isWhitespace(iterator.current())) {
                break;
            }
            iterator.next();
        }
    }

    private void expectChar(CharacterIterator i, char c) {
        final char r = i.current();
        i.next();
        if (r != c) {
            ProgramError.unexpected("parse error at " + i.getIndex() + ", expected character '" + c + "'");
        }
    }

    private char parseCharLiteral(CharacterIterator i) throws Exception {

        expectChar(i, SQUOTE);

        char ch;
        if (peekAndEat(i, BACKSLASH)) {
            ch = parseEscapeChar(i);
        } else {
            ch = i.current();
            i.next();
        }

        expectChar(i, SQUOTE);

        return ch;
    }

    private char parseEscapeChar(CharacterIterator i) {
        final char c = i.current();
        switch (c) {
            case 'f':
                i.next();
                return '\f';
            case 'b':
                i.next();
                return '\b';
            case 'n':
                i.next();
                return '\n';
            case 'r':
                i.next();
                return '\r';
            case BACKSLASH:
                i.next();
                return BACKSLASH;
            case SQUOTE:
                i.next();
                return SQUOTE;
            case QUOTE:
                i.next();
                return QUOTE;
            case 't':
                i.next();
                return '\t';
            case 'x':
                return (char) readHexValue(i, 4);
            case '0': // fall through
            case '1': // fall through
            case '2': // fall through
            case '3': // fall through
            case '4': // fall through
            case '5': // fall through
            case '6': // fall through
            case '7':
                return (char) readOctalValue(i, 3);

        }
        return c;
    }

    private String parseStringLiteral(CharacterIterator i) {
        final StringBuffer buffer = new StringBuffer(i.getEndIndex() - i.getBeginIndex() + 1);

        expectChar(i, QUOTE);
        while (true) {
            if (peekAndEat(i, QUOTE)) {
                break;
            }
            char c = i.current();
            i.next();

            if (c == CharacterIterator.DONE) {
                break;
            }
            if (c == BACKSLASH) {
                c = parseEscapeChar(i);
            }

            buffer.append(c);
        }

        return buffer.toString();
    }

    public static int readHexValue(CharacterIterator i, int maxchars) {
        int accumul = 0;

        for (int cntr = 0; cntr < maxchars; cntr++) {
            final char c = i.current();

            if (c == CharacterIterator.DONE || !Chars.isHexDigit(c)) {
                break;
            }

            accumul = (accumul << 4) | Character.digit(c, 16);
            i.next();
        }

        return accumul;
    }

    public static int readOctalValue(CharacterIterator i, int maxchars) {
        int accumul = 0;

        for (int cntr = 0; cntr < maxchars; cntr++) {
            final char c = i.current();

            if (!Chars.isOctalDigit(c)) {
                break;
            }

            accumul = (accumul << 3) | Character.digit(c, 8);
            i.next();
        }

        return accumul;
    }

    public static String inputToString(Class testClass, Run run, boolean asJavaString) {
        final StringBuffer buffer = new StringBuffer();
        if (asJavaString) {
            buffer.append(QUOTE);
        }
        buffer.append("test(");
        for (int i = 0; i < run._input.length; i++) {
            if (i > 0) {
                buffer.append(',');
            }
            final Object val = run._input[i];
            if (val instanceof Character) {
                buffer.append(Chars.toJavaLiteral(((Character) val).charValue()));
            } else if (val instanceof String) {
                buffer.append(asJavaString ? ESCAPED_QUOTE : QUOTE);
                buffer.append(val);
                buffer.append(asJavaString ? ESCAPED_QUOTE : QUOTE);
            } else {
                buffer.append(String.valueOf(val));
            }
        }
        buffer.append(')');
        if (asJavaString) {
            buffer.append(QUOTE);
        }
        return buffer.toString();
    }

    public static String resultToString(Object val, Class<? extends Throwable> throwable) {
        if (throwable != null) {
            return "!" + throwable.getName();
        }
        if (val instanceof Character) {
            return Chars.toJavaLiteral(((Character) val).charValue());
        }
        return String.valueOf(val);
    }

    public static String valueToString(Object val, Throwable thrown) {
        if (thrown == null) {
            return resultToString(val, null);
        }
        return resultToString(val, thrown.getClass());
    }
}

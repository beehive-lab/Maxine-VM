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
package com.sun.max.program.option;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.program.option.Option.Error;

public class OptionTypes {

    protected static class LongType extends Option.Type<Long> {

        protected LongType() {
            super(Long.class, "long");
        }

        @Override
        public Long parseValue(String string) {
            if (string.length() == 0) {
                return 0L;
            }
            try {
                return Long.valueOf(string);
            } catch (NumberFormatException e) {
                throw new Option.Error("invalid long value: " + string);
            }
        }

        @Override
        public String getValueFormat() {
            return "<long>";
        }
    }

    protected static class ScaledLongType extends LongType {

        protected ScaledLongType() {
            super();
        }
        @Override
        public Long parseValue(String string) {
            final char last = string.charAt(string.length() - 1);
            String s = string;
            int multiplier = 1;
            if (last == 'k' || last == 'K') {
                multiplier = 1024;
                s = s.substring(0, s.length() - 1);
            } else if (last == 'm' || last == 'M') {
                multiplier = 1024 * 1024;
                s = s.substring(0, s.length() - 1);
            } else if (last == 'g' || last == 'G') {
                multiplier = 1024 * 1024 * 1024;
                s = s.substring(0, s.length() - 1);
            }
            final long value = super.parseValue(s);
            if (value > Long.MAX_VALUE / multiplier || value < Long.MIN_VALUE / multiplier) {
                throw new Option.Error("invalid long value: " + s);
            }
            return value * multiplier;
        }
    }

    public static class ConfigFile extends Option.Type<File> {
        protected final OptionSet optionSet;

        public ConfigFile(OptionSet set) {
            super(File.class, "file");
            optionSet = set;
        }
        @Override
        public File parseValue(String string) {
            if (string != null) {
                final File f = new File(string);
                if (!f.exists()) {
                    throw new Option.Error("configuration file does not exist: " + string);
                }
                try {
                    optionSet.loadFile(string, true);
                } catch (IOException e) {
                    throw new Option.Error("error loading config from " + string + ": " + e.getMessage());
                }
            }
            return null;
        }

        @Override
        public String getValueFormat() {
            return "<file>";
        }
    }

    public static class ListType<T> extends Option.Type<List<T>> {
        protected final char separator;
        public final Option.Type<T> elementOptionType;

        private static <T> Class<List<T>> listClass(Class<T> valueClass) {
            final Class<Class<List<T>>> type = null;
            return Utils.cast(type, List.class);
        }

        public ListType(char separator, Option.Type<T> elementOptionType) {
            super(listClass(elementOptionType.type), "list");
            this.separator = separator;
            this.elementOptionType = elementOptionType;
        }

        @Override
        public String unparseValue(List<T> value) {
            final StringBuilder buffer = new StringBuilder();
            boolean previous = false;
            for (Object object : value) {
                if (previous) {
                    buffer.append(separator);
                }
                previous = true;
                buffer.append(object.toString());
            }
            return buffer.toString();
        }

        @Override
        public List<T> parseValue(String val) {
            final List<T> list = new LinkedList<T>();
            if (val.isEmpty()) {
                return list;
            }

            final CharacterIterator i = new StringCharacterIterator(val);
            StringBuilder buffer = new StringBuilder(32);
            while (i.current() != CharacterIterator.DONE) {
                if (i.current() == separator) {
                    list.add(elementOptionType.parseValue(buffer.toString().trim()));
                    buffer = new StringBuilder(32);
                } else {
                    buffer.append(i.current());
                }
                i.next();
            }
            list.add(elementOptionType.parseValue(buffer.toString().trim()));
            return list;
        }

        @Override
        public String getValueFormat() {
            return "[<arg>{" + separator + "<arg>}*]";
        }
    }

    public static class EnumType<T extends Enum<T>> extends Option.Type<T> {
        public final T[] values;

        public EnumType(Class<T> enumClass) {
            super(enumClass, enumClass.getName());
            values = enumClass.getEnumConstants();
        }

        @Override
        public T parseValue(String string) {
            if (string == null) {
                return null;
            }
            for (T value : values) {
                if (value.name().equalsIgnoreCase(string)) {
                    return value;
                }
            }
            throw new Option.Error("invalid " + typeName);
        }

        @Override
        public String getValueFormat() {
            return Utils.toString(values, "|");
        }
    }

    public static final Option.Type<Long> LONG_TYPE = new LongType();
    public static final Option.Type<Long> SCALED_LONG_TYPE = new ScaledLongType();

    public static final Option.Type<String> STRING_TYPE = new Option.Type<String>(String.class, "string") {
        @Override
        public String parseValue(String string) {
            return string;
        }

        @Override
        public String getValueFormat() {
            return "<arg>";
        }
    };

    /**
     * @author Thomas Wuerthinger
     *
     * @return An option type that takes a class name as its value. It reflectively creates an instance
     * of the specified class. If the class is not found, it tries to prefix the class name with "com.sum.max.".
     */
    public static final <T> Option.Type<T> createInstanceOptionType(final Class<T> klass) {
        return new Option.Type<T>(klass, "instance") {

            @Override
            public String getValueFormat() {
                return "<class>";
            }

            @Override
            public T parseValue(String string) throws Error {
                try {
                    try {
                        return Utils.cast(klass, Class.forName(string).newInstance());
                    } catch (ClassNotFoundException e) {
                        return Utils.cast(klass, Class.forName("com.sun.max." + string).newInstance());
                    }
                } catch (InstantiationException e) {
                    ProgramError.unexpected("Could not instantiate class " + string, e);
                } catch (IllegalAccessException e) {
                    ProgramError.unexpected("Could not access class " + string, e);
                } catch (ClassNotFoundException e) {
                    ProgramError.unexpected("Could not find class " + string, e);
                }
                return null;
            }
        };
    }

    /**
     * @author Thomas Wuerthinger
     *
     * @return An option type that takes a list of class names as its value. Then it reflectively creates instances
     * of these classes and returns them as a list.
     */
    public static final <T> ListType<T> createInstanceListOptionType(final Class<T> klass, char separator) {
        return new ListType<T>(separator, createInstanceOptionType(klass));
    }

    public static final Option.Type<Double> DOUBLE_TYPE = new Option.Type<Double>(Double.class, "double") {
        @Override
        public Double parseValue(String string) {
            if (string.length() == 0) {
                return 0.0d;
            }
            try {
                return Double.valueOf(string);
            } catch (NumberFormatException e) {
                throw new Option.Error("invalid double value: " + string);
            }
        }

        @Override
        public String getValueFormat() {
            return "<double>";
        }
    };
    public static final Option.Type<Float> FLOAT_TYPE = new Option.Type<Float>(Float.class, "float") {
        @Override
        public Float parseValue(String string) {
            if (string.length() == 0) {
                return 0.0f;
            }
            try {
                return Float.valueOf(string);
            } catch (NumberFormatException e) {
                throw new Option.Error("invalid float value: " + string);
            }
        }

        @Override
        public String getValueFormat() {
            return "<float>";
        }
    };
    public static final Option.Type<Integer> INT_TYPE = new Option.Type<Integer>(Integer.class, "int") {
        @Override
        public Integer parseValue(String string) {
            if (string.length() == 0) {
                return 0;
            }
            try {
                return Integer.valueOf(string);
            } catch (NumberFormatException e) {
                throw new Option.Error("invalid int value: " + string);
            }
        }

        @Override
        public String getValueFormat() {
            return "<int>";
        }
    };
    public static final Option.Type<Boolean> BOOLEAN_TYPE = new Option.Type<Boolean>(Boolean.class, "boolean") {
        @Override
        public Boolean parseValue(String string) {
            if (string.isEmpty() || string.equalsIgnoreCase("true") || string.equalsIgnoreCase("t") || string.equalsIgnoreCase("y")) {
                return Boolean.TRUE;
            } else if (string.equalsIgnoreCase("false") || string.equalsIgnoreCase("f") || string.equalsIgnoreCase("n")) {
                return Boolean.FALSE;
            }
            throw new Option.Error("invalid boolean value: " + string);
        }

        @Override
        public String getValueFormat() {
            return "true|false, t|f, y|n";
        }
    };
    public static final Option.Type<Boolean> BLANK_BOOLEAN_TYPE = new Option.Type<Boolean>(Boolean.class, "boolean") {
        @Override
        public Boolean parseValue(String string) {
            // blank boolean always returns null
            return null;
        }

        @Override
        public String getValueFormat() {
            return "true|false, t|f, y|n";
        }
    };
    public static final Option.Type<File> FILE_TYPE = new Option.Type<File>(File.class, "file") {
        @Override
        public File parseValue(String string) {
            if (string == null || string.length() == 0) {
                return null;
            }
            return new File(string);
        }

        @Override
        public String getValueFormat() {
            return "<file>";
        }
    };
    public static final Option.Type<URL> URL_TYPE = new Option.Type<URL>(URL.class, "URL") {
        @Override
        public URL parseValue(String string) {
            if (string == null || string.length() == 0) {
                return null;
            }
            try {
                return new URL(string);
            } catch (MalformedURLException e) {
                throw new Option.Error("invalid URL: " + string);
            }
        }

        @Override
        public String getValueFormat() {
            return "<URL>";
        }
    };

    public static class StringListType extends ListType<String> {
        public StringListType(char separator) {
            super(separator, STRING_TYPE);
        }
    }

    public static class EnumListType<T extends Enum<T>> extends ListType<T> {
        public EnumListType(Class<T> enumClass, char separator) {
            super(separator, new EnumType<T>(enumClass));
        }
    }

    public static final StringListType COMMA_SEPARATED_STRING_LIST_TYPE = new StringListType(',');

}

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
/*VCSID=5d6b5978-07a5-46bf-9a17-085c941bc9eb*/
package com.sun.max.program.option;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
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
            return "<n>";
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
        protected final OptionSet _optionSet;

        public ConfigFile(OptionSet set) {
            super(File.class, "file");
            _optionSet = set;
        }
        @Override
        public File parseValue(String string) {
            if (string != null) {
                final File f = new File(string);
                if (!f.exists()) {
                    throw new Option.Error("configuration file does not exist: " + string);
                }
                try {
                    _optionSet.loadFile(string, true);
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

    public static class ListType<Value_Type> extends Option.Type<List<Value_Type>> {
        protected final char _separator;
        public final Option.Type<Value_Type> _elementOptionType;

        private static <Value_Type> Class<List<Value_Type>> listClass(Class<Value_Type> valueClass) {
            final Class<Class<List<Value_Type>>> type = null;
            return StaticLoophole.cast(type, List.class);
        }

        public ListType(char separator, Option.Type<Value_Type> elementOptionType) {
            super(listClass(elementOptionType._type), "list");
            _separator = separator;
            _elementOptionType = elementOptionType;
        }

        @Override
        public String unparseValue(List<Value_Type> value) {
            final StringBuilder buffer = new StringBuilder();
            boolean previous = false;
            for (Object object : value) {
                if (previous) {
                    buffer.append(_separator);
                }
                previous = true;
                buffer.append(object.toString());
            }
            return buffer.toString();
        }

        @Override
        public List<Value_Type> parseValue(String val) {
            final List<Value_Type> list = new LinkedList<Value_Type>();
            if (val.isEmpty()) {
                return list;
            }

            final CharacterIterator i = new StringCharacterIterator(val);
            StringBuilder buffer = new StringBuilder(32);
            while (i.current() != CharacterIterator.DONE) {
                if (i.current() == _separator) {
                    list.add(_elementOptionType.parseValue(buffer.toString().trim()));
                    buffer = new StringBuilder(32);
                } else {
                    buffer.append(i.current());
                }
                i.next();
            }
            list.add(_elementOptionType.parseValue(buffer.toString().trim()));
            return list;
        }

        @Override
        public String getValueFormat() {
            return "[<arg>{" + _separator + "<arg>}*]";
        }
    }

    public static class EnumType<Enum_Type extends Enum<Enum_Type>> extends Option.Type<Enum_Type> {
        public final Enum_Type[] _values;

        public EnumType(Class<Enum_Type> enumClass) {
            super(enumClass, enumClass.getName());
            _values = enumClass.getEnumConstants();
        }

        @Override
        public Enum_Type parseValue(String string) {
            if (string == null) {
                return null;
            }
            for (Enum_Type value : _values) {
                if (value.toString().equalsIgnoreCase(string)) {
                    return value;
                }
            }
            throw new Option.Error("invalid " + _typeName);
        }

        @Override
        public String getValueFormat() {
            return Arrays.toString(_values, "|");
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
    public static final <Instance_Type> Option.Type<Instance_Type> createInstanceOptionType(final Class<Instance_Type> klass) {
        return new Option.Type<Instance_Type>(klass, "instance") {

            @Override
            public String getValueFormat() {
                return "<class>";
            }

            @Override
            public Instance_Type parseValue(String string) throws Error {
                try {
                    try {
                        return StaticLoophole.cast(klass, Class.forName(string).newInstance());
                    } catch (ClassNotFoundException e) {
                        return StaticLoophole.cast(klass, Class.forName("com.sun.max." + string).newInstance());
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
    public static final <Instance_Type> ListType<Instance_Type> createInstanceListOptionType(final Class<Instance_Type> klass, char separator) {
        return new ListType<Instance_Type>(separator, createInstanceOptionType(klass));
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
            return "<n>";
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
            return "<n>";
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
            return "<n>";
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

    public static class EnumListType<Enum_Type extends Enum<Enum_Type>> extends ListType<Enum_Type> {
        public EnumListType(Class<Enum_Type> enumClass, char separator) {
            super(separator, new EnumType<Enum_Type>(enumClass));
        }
    }

    public static final StringListType COMMA_SEPARATED_STRING_LIST_TYPE = new StringListType(',');


}

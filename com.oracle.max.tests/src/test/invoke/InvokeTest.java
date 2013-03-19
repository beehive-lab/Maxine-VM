package test.invoke;

import java.lang.reflect.*;


public class InvokeTest {

    /**
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("invokee missing");
        }

        Method testMethod = findMethod(args[0], "test");

        Class<?>[] params = testMethod.getParameterTypes();
        Object[] callArgs = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            String argI = args[i + 1];
            Class<?> param = params[i];
            if (param == int.class) {
                callArgs[i] = Integer.parseInt(argI);
            } else if (param == long.class) {
                callArgs[i] = Long.parseLong(argI);
            } else if (param == String.class) {
                callArgs[i] = argI;
            } else if (param == Object.class) {
                callArgs[i] = argI;
            } else if (Object.class.isAssignableFrom(param)) {
                callArgs[i] = Class.forName(argI).newInstance();
            } else if (param == float.class) {
                callArgs[i] = Float.parseFloat(argI);
            } else if (param == double.class) {
                callArgs[i] = Double.parseDouble(argI);
            }
        }

        testMethod.invoke(null, callArgs);

    }

    private static Method findMethod(String className, String methodName) throws Exception {
        Class<?> testClass = Class.forName(className);
        Method[] methods = testClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new Exception("method: " + methodName + " not found");
    }

}

package test.invoke;

import java.lang.reflect.*;


public class InvokeTest {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("invokee missing");
        }
        Class<?> testClass = Class.forName(args[0]);
        Method[] methods = testClass.getDeclaredMethods();
        Method testMethod = null;
        for (Method method : methods) {
            if (method.getName().equals("test")) {
                testMethod = method;
                break;
            }
        }
        if (testMethod == null) {
            throw new IllegalArgumentException("test method not found");
        }
        Class<?>[] params = testMethod.getParameterTypes();
        Object[] callArgs = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            String argI = args[i + 1];
            Class<?> param = params[i];
            if (param == int.class) {
                callArgs[i] = Integer.parseInt(argI);
            } else if (param == long.class) {
                callArgs[i] = Long.parseLong(argI);
            } else if (param == Object.class) {
                callArgs[i] = argI;
            }
        }
        testMethod.invoke(null, callArgs);

    }

}

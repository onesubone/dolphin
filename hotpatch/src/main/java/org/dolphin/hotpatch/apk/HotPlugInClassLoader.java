package org.dolphin.hotpatch.apk;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import dalvik.system.DexClassLoader;

/**
 * Created by yananh on 2015/11/19.
 *
 * {@see DexClassLoader}可以加载dex/jar/apk
 */
public class HotPlugInClassLoader extends DexClassLoader {
    public HotPlugInClassLoader(String dexPath, String optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
    }


    /**
     * 与一般的三层架构不同，全盘双亲委托并不适用于次场景，此ClassLoader当需要加载class时，步骤如下：<br>
     *  1. 从当前ClassLoader中寻找已经加载过的class，如果找到，则返回给当前用户，否则进入第二步；
     *  2. 与一般的双亲委托会先尝试从parent中查找，但是此HotPlugInClassLoader会直接从当前结合中尝试加载，如果有，直接返回，否则进入第三步；
     *  3. 从parent ClassLoader中查找；
     *  以上意味着当前的apk会覆盖host classLoader的class，需要注意安全性，之所以这样做，就是为了使之更灵活
     * @param className 需要查找的className
     * @param resolve 在android中无效
     * @return  查询到的class
     * @throws ClassNotFoundException 没有找到时，则直接抛出异常
     */
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(className);

        if (clazz == null) {
            ClassNotFoundException suppressed = null;
            try {
                clazz = findClass(className);
            } catch (ClassNotFoundException e) {
                suppressed = e;
            }

            if (clazz == null) {
                try {
                    clazz = getParent().loadClass(className);
                } catch (ClassNotFoundException e) {
//                    e.addSuppressed(suppressed);
                    throw e;
                }
            }
        }

        return clazz;
    }






    /**
     * Locates a given field anywhere in the class inheritance hierarchy.
     *
     * @param instance an object to search the field into.
     * @param name     field name
     * @return a field object
     * @throws NoSuchFieldException if the field cannot be located
     */
    private static Field findField(Object instance, String name) throws NoSuchFieldException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Field field = clazz.getDeclaredField(name);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                return field;
            } catch (NoSuchFieldException e) {
                // ignore and search next
            }
        }
        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass());
    }

    /**
     * Locates a given method anywhere in the class inheritance hierarchy.
     *
     * @param instance       an object to search the method into.
     * @param name           method name
     * @param parameterTypes method parameter types
     * @return a method object
     * @throws NoSuchMethodException if the method cannot be located
     */
    private static Method findMethod(Object instance, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                return method;
            } catch (NoSuchMethodException e) {
                // ignore and search next
            }
        }
        throw new NoSuchMethodException("Method " + name + " with parameters " +
                Arrays.asList(parameterTypes) + " not found in " + instance.getClass());
    }
}

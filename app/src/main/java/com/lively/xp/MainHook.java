package com.lively.xp;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CopyOnWriteArrayList;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String BUILD_ID = "20260222-1322-fhc-dualplus-tailguard";

    private static final String FLOAT_HANDLE_CONTROLLER =
            "com.android.server.wm.FloatHandleController";
    private static final String CONFIG_HELPER =
            "com.android.server.wm.floathandle.OplusLivelyFloatConfigHelper";
    private static final String TIMEOUT_POLICY =
            "com.android.server.wm.floathandle.LivelyFloatHandleViewManager$TimeOutPolicy";

    private volatile boolean mControllerHookInstalled;
    private volatile boolean mTimeoutHookInstalled;
    private volatile boolean mConfigHookInstalled;
    private volatile boolean mClassLoaderHookInstalled;
    private volatile boolean mListHookInstalled;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"android".equals(lpparam.packageName) || !"android".equals(lpparam.processName)) {
            return;
        }

        ClassLoader classLoader = lpparam.classLoader;
        XposedBridge.log("[LivelyXP] load package android, build=" + BUILD_ID);
        installImmediateHooks(classLoader);
        hookClassLoading(classLoader);
    }

    private void installImmediateHooks(ClassLoader classLoader) {
        hookTimeoutPolicy(classLoader);
        hookLivelyLimits(classLoader);
        hookStaticLimits(classLoader);
        hookDeepLimitGuards(classLoader);
        hookConfigGuards(classLoader);
        hookListRemoveGuard();
    }

    private void hookClassLoading(ClassLoader classLoader) {
        if (mClassLoaderHookInstalled) {
            return;
        }
        mClassLoaderHookInstalled = true;
        try {
            XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object result = param.getResult();
                            if (!(result instanceof Class)) {
                                return;
                            }
                            Class<?> clazz = (Class<?>) result;
                            String name = clazz.getName();
                            if (FLOAT_HANDLE_CONTROLLER.equals(name)) {
                                hookLivelyLimits(clazz);
                                hookStaticLimits(clazz);
                                hookDeepLimitGuards(clazz);
                            } else if (TIMEOUT_POLICY.equals(name)) {
                                hookTimeoutPolicy(clazz);
                            } else if (CONFIG_HELPER.equals(name)) {
                                hookConfigGuards(clazz);
                            }
                        }
                    });
        } catch (Throwable t) {
            mClassLoaderHookInstalled = false;
            XposedBridge.log("[LivelyXP] loadClass hook install failed: " + t);
        }
    }

    private void hookTimeoutPolicy(ClassLoader classLoader) {
        Class<?> timeoutClass = findClass(TIMEOUT_POLICY, classLoader);
        if (timeoutClass == null) {
            return;
        }
        hookTimeoutPolicy(timeoutClass);
    }

    private void hookTimeoutPolicy(Class<?> timeoutClass) {
        if (mTimeoutHookInstalled) {
            return;
        }
        mTimeoutHookInstalled = true;
        hookVoidSkip(timeoutClass, "checkTimeout");
        XposedBridge.log("[LivelyXP] timeout hooks installed");
    }

    private void hookLivelyLimits(ClassLoader classLoader) {
        Class<?> controllerClass = findClass(FLOAT_HANDLE_CONTROLLER, classLoader);
        if (controllerClass == null) {
            return;
        }
        hookLivelyLimits(controllerClass);
    }

    private void hookLivelyLimits(Class<?> controllerClass) {
        if (mControllerHookInstalled) {
            return;
        }
        mControllerHookInstalled = true;
        hookBooleanReturn(controllerClass, "isSupporLivelyWithToast", true);
        hookBooleanReturn(controllerClass, "isSupporLively", true);
        hookVoidSkip(controllerClass, "checkLivelySizeMaybeChangeToStatic");
        XposedBridge.log("[LivelyXP] lively hooks installed");
    }

    private void hookStaticLimits(ClassLoader classLoader) {
        Class<?> controllerClass = findClass(FLOAT_HANDLE_CONTROLLER, classLoader);
        if (controllerClass == null) {
            return;
        }
        hookStaticLimits(controllerClass);
    }

    private void hookStaticLimits(Class<?> controllerClass) {
        hookMethods(controllerClass, "checkStaticSizeMaybeRemove", new MethodFilter() {
            @Override
            public boolean accept(Method method) {
                return method.getReturnType() == Void.TYPE && method.getParameterTypes().length == 0;
            }
        }, new MethodHooker() {
            @Override
            public void before(Method method, XC_MethodHook.MethodHookParam param) {
                XposedBridge.log("[LivelyXP] rewrite checkStaticSizeMaybeRemove");
                param.setResult(null);
            }
        });

        hookMethods(controllerClass, "checkIfNeedExitStatic", new MethodFilter() {
            @Override
            public boolean accept(Method method) {
                Class<?>[] ps = method.getParameterTypes();
                return method.getReturnType() == Void.TYPE && ps.length == 1 && ps[0] == Integer.TYPE;
            }
        }, new MethodHooker() {
            @Override
            public void before(Method method, XC_MethodHook.MethodHookParam param) {
                XposedBridge.log("[LivelyXP] rewrite checkIfNeedExitStatic");
                param.setResult(null);
            }
        });

        XposedBridge.log("[LivelyXP] static limit rewrite installed");
    }

    private void hookDeepLimitGuards(ClassLoader classLoader) {
        Class<?> controllerClass = findClass(FLOAT_HANDLE_CONTROLLER, classLoader);
        if (controllerClass == null) {
            return;
        }
        hookDeepLimitGuards(controllerClass);
    }

    private void hookDeepLimitGuards(Class<?> controllerClass) {
        hookMethods(controllerClass, "changeToStatic", new MethodFilter() {
            @Override
            public boolean accept(Method method) {
                return method.getReturnType() == Integer.TYPE && method.getParameterTypes().length == 2;
            }
        }, new MethodHooker() {
            @Override
            public void before(Method method, XC_MethodHook.MethodHookParam param) {
                if (isAnyCallerMethod("checkLivelySizeMaybeChangeToStatic", "updateInfoMapForAdd")) {
                    param.setResult(0);
                }
            }
        });

        XposedBridge.log("[LivelyXP] deep lively guard installed");
    }

    private void hookConfigGuards(ClassLoader classLoader) {
        Class<?> configClass = findClass(CONFIG_HELPER, classLoader);
        if (configClass == null) {
            return;
        }
        hookConfigGuards(configClass);
    }

    private void hookConfigGuards(Class<?> configClass) {
        if (mConfigHookInstalled) {
            return;
        }
        mConfigHookInstalled = true;
        hookBooleanReturn(configClass, "isUnLivelyPackage", false);
        hookBooleanReturn(configClass, "isUnLivelyWithActivity", false);
        hookBooleanReturn(configClass, "isUnLivelyWithComponentName", false);
        hookBooleanReturn(configClass, "getLivelyEnable", true);
        XposedBridge.log("[LivelyXP] config hooks installed");
    }

    private void hookBooleanReturn(Class<?> clazz, String methodName, final boolean value) {
        hookMethods(clazz, methodName, new MethodFilter() {
            @Override
            public boolean accept(Method method) {
                Class<?> ret = method.getReturnType();
                return ret == Boolean.TYPE || ret == Boolean.class;
            }
        }, new MethodHooker() {
            @Override
            public void after(Method method, XC_MethodHook.MethodHookParam param) {
                param.setResult(value);
            }
        });
    }

    private void hookVoidSkip(Class<?> clazz, String methodName) {
        hookMethods(clazz, methodName, new MethodFilter() {
            @Override
            public boolean accept(Method method) {
                Class<?> ret = method.getReturnType();
                return ret == Void.TYPE;
            }
        }, new MethodHooker() {
            @Override
            public void before(Method method, XC_MethodHook.MethodHookParam param) {
                param.setResult(null);
            }
        });
    }

    private void hookMethods(Class<?> clazz, final String methodName, final MethodHooker hooker) {
        hookMethods(clazz, methodName, MethodFilter.ACCEPT_ALL, hooker);
    }

    private void hookMethods(Class<?> clazz, final String methodName, final MethodFilter filter,
                             final MethodHooker hooker) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!methodName.equals(method.getName())) {
                continue;
            }
            if (Modifier.isAbstract(method.getModifiers()) || Modifier.isNative(method.getModifiers())) {
                continue;
            }
            if (!filter.accept(method)) {
                continue;
            }

            try {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        try {
                            hooker.before(method, param);
                        } catch (Throwable t) {
                            XposedBridge.log("[LivelyXP] beforeHook error in " + method + ": " + t);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            hooker.after(method, param);
                        } catch (Throwable t) {
                            XposedBridge.log("[LivelyXP] afterHook error in " + method + ": " + t);
                        }
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log("[LivelyXP] hookMethod failed for " + method + ": " + t);
            }
        }
    }

    private Class<?> findClass(String className, ClassLoader classLoader) {
        Class<?> c = XposedHelpers.findClassIfExists(className, classLoader);
        if (c != null) {
            return c;
        }
        return XposedHelpers.findClassIfExists(className, XposedBridge.BOOTCLASSLOADER);
    }

    private void hookListRemoveGuard() {
        if (mListHookInstalled) {
            return;
        }
        mListHookInstalled = true;
        try {
            XposedHelpers.findAndHookMethod(CopyOnWriteArrayList.class, "remove", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!isAnyCallerMethod("updateInfoMapForAdd")
                                    || !isAnyCallerClass(FLOAT_HANDLE_CONTROLLER)) {
                                return;
                            }
                            CopyOnWriteArrayList<?> list = (CopyOnWriteArrayList<?>) param.thisObject;
                            int index = (Integer) param.args[0];
                            int size = list.size();
                            if (size == 12 && index == size - 1) {
                                Object keep = list.get(index);
                                XposedBridge.log("[LivelyXP] skip updateInfoMapForAdd tail remove@12");
                                param.setResult(keep);
                            }
                        }
                    });
            XposedBridge.log("[LivelyXP] list remove guard installed");
        } catch (Throwable t) {
            mListHookInstalled = false;
            XposedBridge.log("[LivelyXP] list remove guard install failed: " + t);
        }
    }

    private boolean isAnyCallerMethod(String... methodNames) {
        if (methodNames == null || methodNames.length == 0) {
            return false;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement e : stack) {
            String m = e.getMethodName();
            for (String target : methodNames) {
                if (target != null && target.equals(m)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAnyCallerClass(String... classNames) {
        if (classNames == null || classNames.length == 0) {
            return false;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement e : stack) {
            String c = e.getClassName();
            for (String target : classNames) {
                if (target != null && target.equals(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    private interface MethodHooker {
        default void before(Method method, XC_MethodHook.MethodHookParam param) {
        }

        default void after(Method method, XC_MethodHook.MethodHookParam param) {
        }
    }

    private interface MethodFilter {
        MethodFilter ACCEPT_ALL = new MethodFilter() {
            @Override
            public boolean accept(Method method) {
                return true;
            }
        };

        boolean accept(Method method);
    }

}

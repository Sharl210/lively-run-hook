package com.lively.xp;

import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String BUILD_ID = "20260222-2236-core-limits";

    private static final String FLOAT_HANDLE_CONTROLLER =
            "com.android.server.wm.FloatHandleController";
    private static final String CONFIG_HELPER =
            "com.android.server.wm.floathandle.OplusLivelyFloatConfigHelper";
    private static final String TIMEOUT_POLICY =
            "com.android.server.wm.floathandle.LivelyFloatHandleViewManager$TimeOutPolicy";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"android".equals(lpparam.packageName) || !"android".equals(lpparam.processName)) {
            return;
        }

        ClassLoader classLoader = lpparam.classLoader;
        hookTimeoutPolicy(classLoader);
        hookLivelyLimits(classLoader);
        hookStaticLimits(classLoader);
        hookConfigGuards(classLoader);
    }

    private void hookTimeoutPolicy(ClassLoader classLoader) {
        Class<?> timeoutClass = findClass(TIMEOUT_POLICY, classLoader);
        if (timeoutClass == null) {
            return;
        }

        hookMethods(timeoutClass, "checkTimeout", new MethodHooker() {
            @Override
            public void before(Method method, XC_MethodHook.MethodHookParam param) {
                param.setResult(defaultValueFor(method.getReturnType()));
            }
        });
    }

    private void hookLivelyLimits(ClassLoader classLoader) {
        Class<?> controllerClass = findClass(FLOAT_HANDLE_CONTROLLER, classLoader);
        if (controllerClass == null) {
            return;
        }

        hookLimitGuard(controllerClass, "checkLivelySizeMaybeChangeToStatic");
        hookBooleanReturn(controllerClass, "isSupporLivelyWithToast", true);
        hookBooleanReturn(controllerClass, "isSupporLively", true);
        hookIntWhenCaller(controllerClass, "getLivelySize", 1,
                "addFloatHandle", "checkLivelySizeMaybeChangeToStatic");
        hookIntWhenCaller(controllerClass, "getStaticSize", 0,
                "addFloatHandle", "checkStaticSizeMaybeRemove", "checkIfNeedExitStatic");
    }

    private void hookStaticLimits(ClassLoader classLoader) {
        Class<?> controllerClass = findClass(FLOAT_HANDLE_CONTROLLER, classLoader);
        if (controllerClass == null) {
            return;
        }

        hookLimitGuard(controllerClass, "checkStaticSizeMaybeRemove");
        hookLimitGuard(controllerClass, "checkIfNeedExitStatic");
    }

    private void hookConfigGuards(ClassLoader classLoader) {
        Class<?> configClass = findClass(CONFIG_HELPER, classLoader);
        if (configClass == null) {
            return;
        }

        hookBooleanReturn(configClass, "isUnLivelyPackage", false);
        hookBooleanReturn(configClass, "isUnLivelyWithActivity", false);
        hookBooleanReturn(configClass, "isUnLivelyWithComponentName", false);
        hookBooleanReturn(configClass, "getLivelyEnable", true);
    }

    private void hookBooleanReturn(Class<?> clazz, String methodName, final boolean value) {
        hookMethods(clazz, methodName, new MethodHooker() {
            @Override
            public void before(Method method, XC_MethodHook.MethodHookParam param) {
                Class<?> ret = method.getReturnType();
                if (ret == Boolean.TYPE || ret == Boolean.class) {
                    param.setResult(value);
                }
            }
        });
    }

    private void hookIntWhenCaller(Class<?> clazz, String methodName, final int value,
                                   final String... callerMethods) {
        hookMethods(clazz, methodName, new MethodHooker() {
            @Override
            public void before(Method method, XC_MethodHook.MethodHookParam param) {
                Class<?> ret = method.getReturnType();
                if ((ret == Integer.TYPE || ret == Integer.class) && isCallerIn(callerMethods)) {
                    param.setResult(value);
                }
            }
        });
    }

    private void hookLimitGuard(Class<?> clazz, String methodName) {
        hookMethods(clazz, methodName, new MethodHooker() {
            @Override
            public void before(Method method, XC_MethodHook.MethodHookParam param) {
                Class<?> ret = method.getReturnType();
                if (ret == Void.TYPE) {
                    param.setResult(null);
                } else if (ret == Boolean.TYPE || ret == Boolean.class) {
                    param.setResult(Boolean.FALSE);
                }
            }
        });
    }

    private void hookMethods(Class<?> clazz, final String methodName, final MethodHooker hooker) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!methodName.equals(method.getName())) {
                continue;
            }

            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        hooker.before(method, param);
                    } catch (Throwable ignored) {
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        hooker.after(method, param);
                    } catch (Throwable ignored) {
                    }
                }
            });
        }
    }

    private Class<?> findClass(String className, ClassLoader classLoader) {
        return XposedHelpers.findClassIfExists(className, classLoader);
    }

    private boolean isCallerIn(String... methodNames) {
        if (methodNames == null || methodNames.length == 0) {
            return false;
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            String current = element.getMethodName();
            for (String candidate : methodNames) {
                if (candidate != null && candidate.equals(current)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Object defaultValueFor(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE || returnType == Boolean.class) {
            return Boolean.FALSE;
        }
        if (returnType == Integer.TYPE || returnType == Integer.class) {
            return 0;
        }
        if (returnType == Long.TYPE || returnType == Long.class) {
            return 0L;
        }
        if (returnType == Float.TYPE || returnType == Float.class) {
            return 0F;
        }
        if (returnType == Double.TYPE || returnType == Double.class) {
            return 0D;
        }
        return null;
    }

    private interface MethodHooker {
        default void before(Method method, XC_MethodHook.MethodHookParam param) {
        }

        default void after(Method method, XC_MethodHook.MethodHookParam param) {
        }
    }
}

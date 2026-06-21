package com.firefont;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Firefont - Xposed module that forces Firefox to use custom fonts
 * by overriding browser.display.use_document_fonts = 0.
 *
 * Hooks multiple points in the GeckoView initialization to ensure
 * web fonts are always disabled, so the user's custom fonts are used.
 *
 * Supports: Firefox Nightly, Firefox, Firefox Beta
 */
public class Firefont implements IXposedHookLoadPackage {

  private static final String TAG = "Firefont";
  private static final String[] TARGET_PACKAGES = {
    "org.mozilla.fenix", // Firefox Nightly
    "org.mozilla.firefox", // Firefox
    "org.mozilla.firefox.beta", // Firefox Beta
  };

  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
    throws Throwable {
    String packageName = lpparam.packageName;

    // Check if this is a Firefox package
    boolean isTarget = false;
    for (String target : TARGET_PACKAGES) {
      if (target.equals(packageName)) {
        isTarget = true;
        break;
      }
    }

    if (!isTarget) {
      return;
    }

    XposedBridge.log(TAG + ": Hooking " + packageName);
    ClassLoader cl = lpparam.classLoader;

    // Hook 1: GeckoRuntimeSettings.setWebFontsEnabled(boolean)
    // This is the primary control point where GeckoEngine applies
    // DefaultSettings to the runtime. We intercept and force false.
    try {
      Class<?> settingsClass = XposedHelpers.findClass(
        "org.mozilla.geckoview.GeckoRuntimeSettings",
        cl
      );

      XposedHelpers.findAndHookMethod(
        settingsClass,
        "setWebFontsEnabled",
        boolean.class,
        new XC_MethodHook() {
          @Override
          protected void beforeHookedMethod(MethodHookParam param)
            throws Throwable {
            param.args[0] = false;
            XposedBridge.log(TAG + ": Forced setWebFontsEnabled to false");
          }
        }
      );

      XposedBridge.log(TAG + ": Hooked setWebFontsEnabled successfully");
    } catch (Throwable t) {
      XposedBridge.log(
        TAG + ": Failed to hook setWebFontsEnabled: " + t.getMessage()
      );
    }

    // Hook 2: GeckoRuntimeSettings(GeckoRuntimeSettings) constructor
    // After construction with existing settings, force mWebFonts pref to 0.
    try {
      Class<?> settingsClass = XposedHelpers.findClass(
        "org.mozilla.geckoview.GeckoRuntimeSettings",
        cl
      );

      XposedHelpers.findAndHookMethod(
        settingsClass,
        "<init>",
        settingsClass,
        new XC_MethodHook() {
          @Override
          protected void afterHookedMethod(MethodHookParam param)
            throws Throwable {
            Object instance = param.thisObject;
            forceWebFontsPrefToZero(instance);
          }
        }
      );

      XposedBridge.log(
        TAG + ": Hooked GeckoRuntimeSettings(GeckoRuntimeSettings) successfully"
      );
    } catch (Throwable t) {
      XposedBridge.log(
        TAG +
          ": Failed to hook GeckoRuntimeSettings constructor: " +
          t.getMessage()
      );
    }

    // Hook 3: GeckoRuntimeSettings() default constructor
    try {
      Class<?> settingsClass = XposedHelpers.findClass(
        "org.mozilla.geckoview.GeckoRuntimeSettings",
        cl
      );

      XposedHelpers.findAndHookMethod(
        settingsClass,
        "<init>",
        new XC_MethodHook() {
          @Override
          protected void afterHookedMethod(MethodHookParam param)
            throws Throwable {
            Object instance = param.thisObject;
            forceWebFontsPrefToZero(instance);
          }
        }
      );

      XposedBridge.log(TAG + ": Hooked GeckoRuntimeSettings() successfully");
    } catch (Throwable t) {
      XposedBridge.log(
        TAG + ": Failed to hook GeckoRuntimeSettings(): " + t.getMessage()
      );
    }

    XposedBridge.log(TAG + ": All hooks installed for " + packageName);
  }

  /**
   * Force the mWebFonts Pref object to value 0 via reflection.
   */
  private void forceWebFontsPrefToZero(Object runtimeSettingsInstance) {
    try {
      // Get the mWebFonts field (it's a RuntimeSettings.Pref<Integer>)
      java.lang.reflect.Field field = runtimeSettingsInstance
        .getClass()
        .getDeclaredField("mWebFonts");
      field.setAccessible(true);
      Object prefObj = field.get(runtimeSettingsInstance);

      // Call commit(0) on the Pref object
      java.lang.reflect.Method commitMethod = prefObj
        .getClass()
        .getDeclaredMethod("commit", Object.class);
      commitMethod.setAccessible(true);
      commitMethod.invoke(prefObj, Integer.valueOf(0));

      XposedBridge.log(TAG + ": Forced mWebFonts pref = 0");
    } catch (Throwable t) {
      XposedBridge.log(TAG + ": Failed to force mWebFonts: " + t.getMessage());
    }
  }
}

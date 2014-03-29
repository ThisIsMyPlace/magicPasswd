package org.thisismyplace.magicpasswd;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedHook implements IXposedHookLoadPackage {
	boolean isMatch = false;
	XSharedPreferences prefs;
	
	public XposedHook() {
		prefs = new XSharedPreferences(XposedHook.class.getPackage().getName());
	}
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		if(prefs.getBoolean("pref_enabled", false) == false)//:(
			return;
		
		if (!lpparam.packageName.equals("com.android.systemui"))//Code to check password is with in this process,
			return;
		
		findAndHookMethod("com.android.internal.widget.LockPatternUtils", lpparam.classLoader, "checkPassword", String.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				String input = (String) param.args[0];//The inputtedPassword
				String hours = input.substring(0, 2);
				String mins = input.substring(input.length() - 2, input.length());
				
				Date now = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
				boolean hoursMatch = sdf.format(now).substring(0, 2).equals(hours);
				boolean minsMatch = sdf.format(now).substring(2, 4).equals(mins);
				//XposedBridge.log(String.valueOf(hoursMatch));
				//XposedBridge.log(String.valueOf(minsMatch));
				
				isMatch = hoursMatch & minsMatch;
				
				param.args[0] = input.substring(2, input.length() - 2);//Pass the middle to the original function
			}
			@Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!isMatch)
					param.setResult(false);
			}
		});
	}
}

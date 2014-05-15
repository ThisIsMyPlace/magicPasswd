package org.thisismyplace.magicpasswd;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Build;
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
		
		if(Build.VERSION.SDK_INT == 19) {
			if (!lpparam.packageName.equals("com.android.systemui"))//Code to check password is with in this process,
				return;
		} else {
			if (!lpparam.packageName.equals("android"))
				return;
		}
		
		findAndHookMethod("com.android.internal.widget.LockPatternUtils", lpparam.classLoader, "checkPassword", String.class, new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				String input = (String) param.args[0];//The inputtedPassword
				
				String hours = "";
				String mins = "";
				
				//get the HHmm from the specific format,
				//and remove from the parameter
				switch(prefs.getString("pref_type", "0")) {
				case "0": //HH{orignalPassword}mm
					hours = input.substring(0, 2);
					mins = input.substring(input.length() - 2, input.length());
					param.args[0] = input.substring(2, input.length() - 2);
					break;
				case "1": //HHmm{orignalPassword}
					hours = input.substring(0, 2);
					mins = input.substring(2, 4);
					param.args[0] = input.substring(4, input.length());
					break;
				case "2": //{orignalPassword}HHmm
					hours = input.substring(input.length() - 4, input.length() - 2);
					mins = input.substring(input.length() - 2, input.length());
					param.args[0] = input.substring(0, input.length() - 4);
					break;
				case "3": //HHmm
					hours = input.substring(0, 2);
					mins = input.substring(2, 4);
					break;
				}
				
				Date now = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
				boolean hoursMatch = sdf.format(now).substring(0, 2).equals(hours);
				boolean minsMatch = sdf.format(now).substring(2, 4).equals(mins);
				
				isMatch = hoursMatch & minsMatch;
				
				if(prefs.getString("pref_type", "0").equals("3")) {
					 if(input.length() != 4)
						 isMatch = false;
					 param.setResult(isMatch);//type 3 dosn't include password, so
					 //don't pass to auth function
				}
				
				/*XposedBridge.log("Input: " + input);
				XposedBridge.log("Type: " + prefs.getString("pref_type", "0"));
				XposedBridge.log("hoursMatch: " + hoursMatch);
				XposedBridge.log("minsMatch: " + minsMatch);
				XposedBridge.log("isMatch: " + isMatch);*/
			}
			@Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if(!isMatch)
					param.setResult(false);
			}
		});
	}
}

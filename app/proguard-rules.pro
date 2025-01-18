# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn com.google.errorprone.annotations.MustBeClosed

-optimizationpasses 5
-repackageclasses
-allowaccessmodification
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}

# For native methods, see https://www.guardsquare.com/manual/configuration/examples#native
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Keep setters in Views so that animations can still work.
#-keepclassmembers public class * extends android.view.View {
#    void set*(***);
#    *** get*();
#}

## We want to keep methods in Activity that could be used in the XML attribute onClick.
#-keepclassmembers class * extends android.app.Activity {
#    public void *(android.view.View);
#}

## For enumeration classes, see https://www.guardsquare.com/manual/configuration/examples#enumerations
#-keepclassmembers enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# The support libraries contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version. We know about them, and they are safe.
-dontnote android.support.**
-dontnote androidx.**
-dontwarn android.support.**
-dontwarn androidx.**

# Understand the @Keep support annotation.
-keep class android.support.annotation.Keep

-keep @android.support.annotation.Keep class * {*;}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <methods>;
}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <fields>;
}

-keepclasseswithmembers class * {
    @android.support.annotation.Keep <init>(...);
}

# These classes are duplicated between android.jar and org.apache.http.legacy.jar.
-dontnote org.apache.http.**
-dontnote android.net.http.**

# These classes are duplicated between android.jar and core-lambda-stubs.jar.
-dontnote java.lang.invoke.**

# Keep Smartspacer's client SDK
-keep class com.kieronquinn.app.smartspacer.sdk.**  { *; }
# Ignore Baloon stuff
-dontwarn com.skydoves.balloon.ArrowPositionRules
-dontwarn com.skydoves.balloon.Balloon$Builder
-dontwarn com.skydoves.balloon.Balloon
-dontwarn com.skydoves.balloon.BalloonAnimation
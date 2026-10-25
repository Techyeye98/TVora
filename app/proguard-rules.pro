# R8 rules for the release build (SECURITY 4.6). Obfuscation is not a security control.
-keepattributes SourceFile,LineNumberTable
# Debug and verbose logging never reach a release build.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
-assumenosideeffects class tv.cleanyour.app.CytLog {
    public void d(java.lang.String);
}
# Conscrypt carries adapters for private pre-Lollipop platform types absent from the public SDK.
# This app supports API 28+, and uses Conscrypt's own TLS sockets. Keep warnings narrow.
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl

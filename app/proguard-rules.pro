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
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.jdom.Document
-dontwarn org.jdom.Element
-dontwarn org.jdom.input.SAXBuilder
# Keep Zstd JNI classes and their fields/methods intact for native code
-keep class com.github.luben.zstd.** { *; }
-keepclassmembers class com.github.luben.zstd.** { *; }
# Also preserve the actual attributes and native method bindings
-keepclasseswithmembernames class com.github.luben.zstd.** {
    native <methods>;
}
-keepclassmembers class com.github.luben.zstd.ZstdInputStreamNoFinalizer {
    long srcPos;
    long dstPos;
}

# Geonames local JAR
-keep class org.geonames.** { *; }
-keep class org.jdom.** { *; }

# Esri geometry (Timeshape dependency): Wkid loads .txt resources relative to its package
-keep class com.esri.core.geometry.** { *; }
-dontwarn com.esri.core.geometry.**

# Timeshape and its protobuf-generated GeoJSON classes
-keep class net.iakovlev.timeshape.** { *; }
-dontwarn net.iakovlev.timeshape.**
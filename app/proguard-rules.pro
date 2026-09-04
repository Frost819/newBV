# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for crash log deobfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# === akdanmaku ===
# akdanmaku ECS framework uses reflection to instantiate component classes.
# Without these rules, R8 strips constructors and the player crashes with
# "constructor not found" when opening the player in release builds.
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication
-dontwarn com.badlogic.gdx.utils.GdxBuild
-dontwarn com.badlogic.gdx.jnigen.BuildTarget*
-dontwarn com.badlogic.gdx.graphics.g2d.freetype.FreetypeBuild
-keep class com.badlogic.gdx.controllers.android.AndroidControllers
-keep class com.kuaishou.akdanmaku.ecs.DanmakuContext
-keepclasseswithmembers class * {
    public <init>(com.kuaishou.akdanmaku.ecs.DanmakuContext);
}
-keepclasseswithmembers class com.kuaishou.akdanmaku.ecs.component.*
-keep class com.kuaishou.akdanmaku.ecs.component.* {
  <init>(...);
}

# === okhttp ===
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# === kotlin serialization ===
# Keep `Companion` object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# === ktor ===
# Ktor's consumer rules keep engine service discovery and required runtime members.
# Do not keep the whole framework here, otherwise R8 cannot remove unused plugins.
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# === gRPC / protobuf ===
# Only the business status message is consumed by name from gRPC trailers.
-keep class bilibili.rpc.Status { *; }
-dontwarn com.google.protobuf.GeneratedMessageV3$Builder
-dontwarn com.google.protobuf.GeneratedMessageV3$BuilderParent
-dontwarn com.google.protobuf.GeneratedMessageV3$FieldAccessorTable
-dontwarn com.google.protobuf.GeneratedMessageV3
-dontwarn com.google.protobuf.RepeatedFieldBuilderV3

# === kotlin-logging ===
-dontwarn ch.qos.logback.classic.Level
-dontwarn ch.qos.logback.classic.Logger
-dontwarn ch.qos.logback.classic.LoggerContext
-dontwarn ch.qos.logback.classic.spi.ILoggingEvent
-dontwarn ch.qos.logback.classic.spi.LogbackServiceProvider
-dontwarn ch.qos.logback.classic.spi.LoggingEvent

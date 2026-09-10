# Compose + Room + CameraX/MLKit are kept via their bundled consumer rules.
# Guard uses reflection-free HMAC; nothing extra needed.
-keep class com.yft.rippleup.data.db.** { *; }
-dontwarn org.slf4j.**

# CameraX + MLKit + credentials: belt-and-braces keeps (they ship consumer rules,
# but the QR scanner crashed on a release build once — don't risk it)
-keep class androidx.camera.** { *; }
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.mlkit.**

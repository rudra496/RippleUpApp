# Compose + Room + CameraX/MLKit are kept via their bundled consumer rules.
# Guard uses reflection-free HMAC; nothing extra needed.
-keep class com.yft.rippleup.data.db.** { *; }
-dontwarn org.slf4j.**

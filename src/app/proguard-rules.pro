# JNI: SQLCipher e Argon2
-keep class net.zetetic.database.** { *; }
-keep class com.lambdapioneer.argon2kt.** { *; }
-keepclasseswithmembernames class * { native <methods>; }

# Non interrompere la build per riferimenti opzionali mancanti nelle librerie
-ignorewarnings
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
# Enum usati con enumValueOf (preferenze)
-keepclassmembers enum com.notium.app.** { public static **[] values(); public static ** valueOf(java.lang.String); }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keep,includedescriptorclasses class masha.pogoda.**$$serializer { *; }
-keepclassmembers class masha.pogoda.** { *** Companion; }
-keepclasseswithmembers class masha.pogoda.** { kotlinx.serialization.KSerializer serializer(...); }


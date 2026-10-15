-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations
-keepclassmembers class com.patbaumgartner.android.spacedrepeater.** {
    *** Companion;
}
-keepclasseswithmembers class com.patbaumgartner.android.spacedrepeater.** {
    kotlinx.serialization.KSerializer serializer(...);
}

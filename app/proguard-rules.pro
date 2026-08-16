# Navigation Compose type-safe routes (kotlinx.serialization) resolve the
# navigation.Route classes and their argument types (e.g. BottomNavTab) by
# fully-qualified class name at runtime. R8 renaming those classes breaks
# that lookup with "Cannot find class with name ...".
-keepnames class iti.grad.nutriscan.navigation.** { *; }
-keepnames class iti.grad.nutriscan.presentation.common.model.** { *; }

# Retrofit builds a dynamic proxy via reflection for every @Provides
# retrofit.create(SomeApiService::class.java) call (see NetworkModule.kt).
# That proxy needs the interface's generic Signature + annotations intact
# at runtime; R8 stripping them caused a ClassCastException crash on every
# launch. Official Retrofit-recommended keep rules:
-keepattributes Signature,*Annotation*,Exceptions,InnerClasses,EnclosingMethod
-keep interface iti.grad.nutriscan.data.remote.api.** { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

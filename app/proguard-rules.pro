# Spendly ProGuard Rules

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# SQLCipher
-keep class net.zetetic.** { *; }
-dontwarn net.zetetic.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep data classes used for serialization
-keepclassmembers class com.spendly.app.domain.model.** { *; }

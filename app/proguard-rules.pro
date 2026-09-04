# PathSathi release R8/ProGuard rules.
# Most androidx/Compose/Room/OkHttp libraries ship their own consumer-proguard
# rules bundled in their AARs, which R8 picks up automatically - the rules
# below cover the handful of cases that commonly need an explicit keep with
# this exact dependency set (Room entities/DAOs via reflection, org.json,
# Kotlin coroutines internals, osmdroid, and this app's own data models that
# get (de)serialized).

# ---- Kotlin / coroutines ----
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepclassmembernames class kotlin.coroutines.jvm.internal.BaseContinuationImpl { *; }

# ---- Room ----
# Room's own consumer rules keep generated *_Impl classes; keep the
# hand-written entities/DAOs too since annotation-processed code reflects
# on their field/method names.
-keep class com.pathsathi.app.data.local.*Entity { *; }
-keep interface com.pathsathi.app.data.local.*Dao { *; }
-keep class com.pathsathi.app.data.local.PathSathiDatabase { *; }
-dontwarn androidx.room.paging.**

# ---- org.json / OkHttp response parsing ----
# The app builds JSONObject/JSONArray field access by string key at runtime
# (Weather, Nearby Help, Routing, Geocoding repositories) - org.json classes
# themselves are part of the Android platform/embedded jar, not shrunk, but
# keep our own network model classes' field names stable for readability of
# stack traces and to avoid over-aggressive optimization of small data classes.
-keep class com.pathsathi.app.data.model.** { *; }
-keep class com.pathsathi.app.data.network.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- osmdroid (map tiles / offline cache) ----
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-dontwarn org.slf4j.**

# ---- Play Services Location ----
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# ---- App's own broadcast receivers / services (referenced only from the
#      manifest, so R8 can't see the reflection-based entry points) ----
-keep class com.pathsathi.app.service.TrackingService { *; }
-keep class com.pathsathi.app.service.CheckInReceiver { *; }
-keep class com.pathsathi.app.service.BootReceiver { *; }
-keep class com.pathsathi.app.MainActivity { *; }

# ---- AI Assistant module (parses/holds free-form command data at runtime) ----
-keep class com.pathsathi.app.ai.** { *; }

# ---- General Android/Compose housekeeping ----
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable
-keep class kotlin.Metadata { *; }

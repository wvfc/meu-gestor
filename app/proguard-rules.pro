# ============================
# Meu Gestor - ProGuard Rules
# ============================

# --- General ---
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# --- Kotlin ---
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- Jetpack Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase {
    public static <methods>;
}
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# --- DataStore ---
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# --- Navigation ---
-keepnames class androidx.navigation.** { *; }

# --- WorkManager ---
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- Coil ---
-dontwarn coil.**

# --- App Models ---
-keep class com.meugestor.app.data.model.** { *; }
-keep class com.meugestor.app.data.local.entity.** { *; }

# --- Enum ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Serializable ---
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

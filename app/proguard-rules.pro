# Proguard / R8 rules for mp1 release builds

# Room rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# Media3 session & service
-keep class androidx.media3.session.** { *; }

# Hilt / Dagger
-keep class * extends dagger.hilt.internal.UnsafeCasts

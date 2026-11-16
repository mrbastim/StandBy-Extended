# Lifecycle 2.8 bridges LocalLifecycleOwner to Compose UI 1.6 through reflection.
# Keep the exact class/getter looked up by LocalLifecycleOwnerKt in minified builds.
-keep class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt {
    public static androidx.compose.runtime.ProvidableCompositionLocal getLocalLifecycleOwner();
}

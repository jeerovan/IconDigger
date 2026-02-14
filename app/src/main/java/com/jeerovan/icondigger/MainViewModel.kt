package com.jeerovan.icondigger

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.TypedValue
import java.io.InputStream

data class IconPackInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)
data class AppInfoUiState(
    val allApps: List<String> = emptyList(),
)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Backing property to avoid state exposure
    private val _iconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    val iconPacks = _iconPacks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val launcherApps = application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    // Triggered on load
    fun installedIconPacks() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val context: Application = getApplication()
            val pm = context.packageManager
            val iconPacks = mutableListOf<IconPackInfo>()

            // Common actions used by icon packs
            val intentActions = listOf(
                "org.adw.launcher.THEMES",
                "com.novalauncher.THEME",
                "com.teslacoilsw.launcher.THEME",
                "com.fede.launcher.THEME_ICONPACK",
                "com.anddoes.launcher.THEME",
                "com.dlto.atom.launcher.THEME"
            )

            val seenPackages = mutableSetOf<String>()

            for (action in intentActions) {
                val intent = Intent(action)
                val resolvedList = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)

                for (info in resolvedList) {
                    val packageName = info.activityInfo.packageName
                    if (packageName !in seenPackages) {
                        seenPackages.add(packageName)
                        try {
                            val appInfo = pm.getApplicationInfo(packageName, 0)
                            val label = pm.getApplicationLabel(appInfo).toString()
                            val icon = pm.getApplicationIcon(appInfo)

                            iconPacks.add(IconPackInfo(label, packageName, icon))
                        } catch (e: Exception) {
                            // Ignore packages that can't be loaded
                        }
                    }
                }
            }

            // Sort alphabetically
            _iconPacks.value = iconPacks.sortedBy { it.name }
            _isLoading.value = false
        }
    }

    fun saveAppFilters(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            IconPackManager.loadIconPack(getApplication(), packageName)

            val context = getApplication<Application>()

            val fileName = "${packageName}.appfilter.xml"
            saveToFolder(context,fileName, IconPackManager.appFilterList)
            _isLoading.value = false
        }
    }

    fun savePackageIconBitmaps(iconPackPackageName: String){
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val context = getApplication<Application>()
            IconPackManager.loadIconPack(context,iconPackPackageName)
            context.savePackageIconBitmaps(iconPackPackageName, IconPackManager.drawableList)
            _isLoading.value = false
        }
    }
    fun saveOriginalDrawables(iconPackPackageName: String){
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val context = getApplication<Application>()
            IconPackManager.loadIconPack(context,iconPackPackageName)
            context.saveOriginalDrawables(iconPackPackageName, IconPackManager.drawableList)
            _isLoading.value = false
        }
    }

    fun saveDeviceAppIcons(){
        viewModelScope.launch(Dispatchers.IO){
            _isLoading.value = true
            val context = getApplication<Application>()
            val activityList = launcherApps.getActivityList(null, android.os.Process.myUserHandle())
            val folderName = "deviceAppIcons"
            activityList.forEach { info ->
                try {
                    val drawable = info.getBadgedIcon(0)
                    val bitmap = drawable.toBitmap(width = 512, height = 512)
                    context.saveBitmapToMediaStore(bitmap, info.componentName.packageName, folderName)
                } catch (e: Exception) {
                    // Log error for specific icon failure and continue
                    e.printStackTrace()
                }
            }
            _isLoading.value = false
        }
    }

    fun saveMyappFilters() {
        viewModelScope.launch(Dispatchers.IO){
            _isLoading.value = true
            val context = getApplication<Application>()
            val activityList = launcherApps.getActivityList(null, android.os.Process.myUserHandle())
            val fileName = "MyAppFilters.txt"
            val componentInfos = activityList.map { "{'package':'${it.componentName.packageName}','activity':'${it.componentName.className}','name':'${it.label}'}" }
            saveToFolder(context,fileName,componentInfos )
            _isLoading.value = false
        }
    }

    suspend fun Context.saveOriginalDrawables(
        iconPackPackageName: String,
        drawableList: List<String>
    ) {
        withContext(Dispatchers.IO) {
            val folderName = iconPackPackageName.replace(".", "_") + "_icon_pack_xml"

            val remoteResources = try {
                packageManager.getResourcesForApplication(iconPackPackageName)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext
            }

            drawableList.forEach { drawableName ->
                try {
                    val resId = remoteResources.getIdentifier(
                        drawableName,
                        "drawable",
                        iconPackPackageName
                    )

                    if (resId != 0) {
                        // 1. Determine the original file extension (xml or png)
                        val value = TypedValue()
                        remoteResources.getValue(resId, value, true)
                        val resourcePath = value.string.toString() // e.g., "res/drawable-nodpi-v4/icon.xml"
                        val extension = resourcePath.substringAfterLast('.', "xml")

                        // 2. Open the raw stream from the APK
                        val inputStream = remoteResources.openRawResource(resId)

                        // 3. Save the stream directly
                        saveStreamToMediaStore(inputStream, drawableName, extension, folderName)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun Context.saveStreamToMediaStore(
        inputStream: InputStream,
        fileName: String,
        extension: String,
        folderName: String
    ) {
        val mimeType = if (extension == "xml") "text/xml" else "image/$extension"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.$extension")
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folderName")
        }

        val resolver = contentResolver
        var uri: Uri? = null

        try {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { outputUri ->
                resolver.openOutputStream(outputUri)?.use { outputStream ->
                    // Copy the raw bytes from the APK stream to the new file
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup on failure
            uri?.let { resolver.delete(it, null, null) }
        } finally {
            inputStream.close()
        }
    }
    suspend fun Context.savePackageIconBitmaps(
        iconPackPackageName: String,
        drawableList: List<String>
    ) {
        withContext(Dispatchers.IO) {
            val folderName = iconPackPackageName.replace(".", "_") + "_icon_pack"

            // 1. Get resources from the Icon Pack application
            val iconPackResources = try {
                packageManager.getResourcesForApplication(iconPackPackageName)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext
            }

            // 2. Iterate and save
            drawableList.forEach { drawableName ->
                try {
                    // Get the resource ID
                    val resId = iconPackResources.getIdentifier(
                        drawableName,
                        "drawable",
                        iconPackPackageName
                    )

                    if (resId != 0) {
                        // Load the drawable and convert to Bitmap
                        // toBitmap() handles VectorDrawables and AdaptiveIconDrawables automatically
                        val drawable = iconPackResources.getDrawable(resId, null)
                        val bitmap = drawable.toBitmap()

                        saveBitmapToMediaStore(bitmap, drawableName, folderName)
                    }
                } catch (e: Exception) {
                    // Log error for specific icon failure and continue
                    e.printStackTrace()
                }
            }
        }
    }
    private fun Context.saveBitmapToMediaStore(
        bitmap: Bitmap,
        fileName: String,
        folderName: String
    ) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            // Relative Path is essential for Android 10+ (Scoped Storage)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folderName")
        }

        val resolver = contentResolver
        var uri: Uri? = null

        try {
            // Insert into MediaStore.Downloads
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { outputUri ->
                resolver.openOutputStream(outputUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If operation fails, clean up the empty entry
            uri?.let { resolver.delete(it, null, null) }
        }
    }
    fun saveToFolder(context: Context, filename: String, listOfStrings: List<String>) {
        val textContent = listOfStrings.joinToString(separator = "\n")

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")

            // For Android 10 (API 29) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                // Mark as pending while writing
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            // Fallback for older devices (technically requires permissions if not using app-specific dirs)
            MediaStore.Files.getContentUri("external")
        }

        try {
            val uri = resolver.insert(collection, contentValues)

            uri?.let { outputUri ->
                resolver.openOutputStream(outputUri)?.use { outputStream ->
                    outputStream.write(textContent.toByteArray())
                }

                // Mark as finished for Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(outputUri, contentValues, null, null)
                }
            }
            println("File saved successfully: $filename")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

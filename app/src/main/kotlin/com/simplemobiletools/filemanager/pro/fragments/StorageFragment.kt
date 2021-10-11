package com.simplemobiletools.filemanager.pro.fragments

import android.annotation.SuppressLint
import android.app.usage.StorageStatsManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.extensions.formatSizeThousand
import kotlinx.android.synthetic.main.storage_fragment.view.*

class StorageFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment(context, attributeSet) {
    override fun setupFragment(activity: SimpleActivity) {
        ensureBackgroundThread {
            getStorageStats(activity)

            val imagesSize = getMediaTypeSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val videosSize = getMediaTypeSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            val audioSize = getMediaTypeSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            val documentsSize = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getProperSize(true)
            val downloadsSize = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getProperSize(true)

            activity.runOnUiThread {
                images_size.text = imagesSize.formatSize()
                images_progressbar.progress = (imagesSize / 1000000).toInt()

                videos_size.text = videosSize.formatSize()
                videos_progressbar.progress = (videosSize / 1000000).toInt()

                audio_size.text = audioSize.formatSize()
                audio_progressbar.progress = (audioSize / 1000000).toInt()
            }
        }
    }

    override fun refreshFragment() {}

    override fun setupColors(textColor: Int, primaryColor: Int) {
        context.updateTextColors(storage_fragment)

        main_storage_usage_progressbar.setIndicatorColor(primaryColor)
        main_storage_usage_progressbar.trackColor = primaryColor.adjustAlpha(0.3f)

        val redColor = context.resources.getColor(R.color.md_red_700)
        images_progressbar.setIndicatorColor(redColor)
        images_progressbar.trackColor = redColor.adjustAlpha(0.3f)

        val greenColor = context.resources.getColor(R.color.md_green_700)
        videos_progressbar.setIndicatorColor(greenColor)
        videos_progressbar.trackColor = greenColor.adjustAlpha(0.3f)

        val blueColor = context.resources.getColor(R.color.md_blue_700)
        audio_progressbar.setIndicatorColor(blueColor)
        audio_progressbar.trackColor = blueColor.adjustAlpha(0.3f)
    }

    private fun getMediaTypeSize(uri: Uri): Long {
        val projection = arrayOf(
            MediaStore.Files.FileColumns.SIZE
        )

        var totalSize = 0L
        try {
            context.queryCursor(uri, projection) { cursor ->
                try {
                    val size = cursor.getLongValue(MediaStore.Files.FileColumns.SIZE)
                    totalSize += size
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }

        return totalSize
    }

    @SuppressLint("NewApi")
    private fun getStorageStats(activity: SimpleActivity) {
        val externalDirs = activity.getExternalFilesDirs(null)
        val storageManager = activity.getSystemService(AppCompatActivity.STORAGE_SERVICE) as StorageManager

        externalDirs.forEach { file ->
            val storageVolume = storageManager.getStorageVolume(file) ?: return
            if (storageVolume.isPrimary) {
                // internal storage
                val storageStatsManager = activity.getSystemService(AppCompatActivity.STORAGE_STATS_SERVICE) as StorageStatsManager
                val uuid = StorageManager.UUID_DEFAULT
                val totalSpace = storageStatsManager.getTotalBytes(uuid)
                val freeSpace = storageStatsManager.getFreeBytes(uuid)

                activity.runOnUiThread {
                    arrayOf(main_storage_usage_progressbar, images_progressbar, videos_progressbar, audio_progressbar).forEach {
                        it.max = (totalSpace / 1000000).toInt()
                    }

                    main_storage_usage_progressbar.progress = ((totalSpace - freeSpace) / 1000000).toInt()

                    main_storage_usage_progressbar.beVisible()
                    free_space_value.text = freeSpace.formatSizeThousand()
                    total_space.text = String.format(context.getString(R.string.total_storage), totalSpace.formatSizeThousand())
                    free_space_label.beVisible()
                }
            } else {
                // sd card
                val totalSpace = file.totalSpace
                val freeSpace = file.freeSpace
            }
        }
    }
}

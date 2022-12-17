package com.aitsuki.compressor

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.*
import androidx.lifecycle.lifecycleScope
import com.aitsuki.compressor.databinding.ActivityMainBinding
import com.aitsuki.util.Compressor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.File

@SuppressLint("SetTextI18n")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val originFileFlow = MutableSharedFlow<File>()

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null)
            lifecycleScope.launch {
                val file = copyToCache(uri) ?: return@launch
                originFileFlow.emit(file)
                showImage(binding.image1, binding.info1, getImageInfo(file))
            }
    }

    @OptIn(FlowPreview::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.resolutionSlider.value = 1200f
        binding.qualitySlider.value = 80f

        lifecycleScope.launch {
            paramsFlow().debounce(300)
                .combine(originFileFlow) { params, originFile ->
                    val start = System.currentTimeMillis()
                    val file = Compressor.compress(
                        this@MainActivity,
                        originFile,
                        params.resolution,
                        params.quality
                    ).getOrThrow()
                    val costTime = System.currentTimeMillis() - start
                    getImageInfo(file) to costTime
                }
                .flowOn(Dispatchers.IO)
                .catch {
                    it.printStackTrace()
                    Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
                }
                .collectLatest {
                    showImage(binding.image2, binding.info2, it.first)
                    binding.costTime.isVisible = true
                    binding.costTime.text = "Compress cost: ${it.second}ms"
                }
        }
    }

    private fun paramsFlow(): Flow<CompressParams> = callbackFlow {
        var resolution = binding.resolutionSlider.value
        var quality = binding.qualitySlider.value
        val callback = fun() {
            binding.params.text = "Resolution: $resolution, Quality: $quality"
            trySend(CompressParams(resolution.toInt(), quality.toInt()))
        }

        binding.resolutionSlider.addOnChangeListener { _, value, _ ->
            resolution = value
            callback()
        }

        binding.qualitySlider.addOnChangeListener { _, value, _ ->
            quality = value
            callback()
        }
        callback()
        awaitClose {
            binding.resolutionSlider.clearOnChangeListeners()
            binding.qualitySlider.clearOnChangeListeners()
        }
    }

    private fun showImage(iv: ImageView, tv: TextView, info: ImageInfo) {
        iv.setImageURI(info.uri)
        tv.isVisible = true
        tv.text = buildString {
            append("Resolution: ${info.width}x${info.height}, Size: ${info.size}")
            append('\n')
            append("Orientation: ${info.orientation}")
        }
        iv.setOnClickListener {
            startActivity(PhotoViewActivity.newIntent(this, info.uri))
        }
    }

    private suspend fun copyToCache(uri: Uri) = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { input ->
            val originFile = File.createTempFile("origin", ".jpg", cacheDir)
            originFile.outputStream().use { output ->
                input.copyTo(output)
            }
            originFile
        }
    }

    private suspend fun getImageInfo(file: File) = withContext(Dispatchers.IO) {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        val w = opts.outWidth
        val h = opts.outHeight
        val size = Formatter.formatShortFileSize(this@MainActivity, file.length())
        val orientation = when (ExifInterface(file).getAttributeInt(TAG_ORIENTATION, 0)) {
            ORIENTATION_FLIP_HORIZONTAL -> "Mirror horizontal"
            ORIENTATION_ROTATE_180 -> "Rotate 180 CW"
            ORIENTATION_FLIP_VERTICAL -> "Mirror horizontal and rotate 180 CW"
            ORIENTATION_TRANSPOSE -> "Mirror horizontal and rotate 270 mirror"
            ORIENTATION_ROTATE_90 -> "Rotate 90 CW"
            ORIENTATION_TRANSVERSE -> "Mirror horizontal and rotate 90 CW"
            ORIENTATION_ROTATE_270 -> "Rotate 270 CW"
            ORIENTATION_NORMAL -> "Normal"
            else -> "Undefined"
        }
        ImageInfo(
            uri = Uri.fromFile(file),
            width = w,
            height = h,
            size = size,
            orientation = orientation
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        pickPhoto.launch("image/*")
        return true
    }

    private class CompressParams(
        val resolution: Int,
        val quality: Int
    )

    private class ImageInfo(
        val uri: Uri,
        val width: Int,
        val height: Int,
        val size: String,
        val orientation: String,
    )
}
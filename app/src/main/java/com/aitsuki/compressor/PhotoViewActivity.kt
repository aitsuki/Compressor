package com.aitsuki.compressor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.aitsuki.compressor.databinding.ActivityPhotoViewBinding

class PhotoViewActivity : AppCompatActivity() {

    companion object {
        fun newIntent(context: Context, imageUri: Uri): Intent {
            return Intent(context, PhotoViewActivity::class.java)
                .putExtra("imageUri", imageUri)
        }
    }

    private lateinit var binding: ActivityPhotoViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val uri = intent.getParcelableExtra<Uri>("imageUri")
        binding.photoView.setImageURI(uri)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }
}
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
        @JvmStatic
        fun start(context: Context, data: Uri) {
            val starter = Intent(context, PhotoViewActivity::class.java).setData(data)
            context.startActivity(starter)
        }
    }

    private lateinit var binding: ActivityPhotoViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.photoView.setImageURI(intent.data)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }
}
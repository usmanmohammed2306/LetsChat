package android.example.com.letschat.Activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.*

class SplashScreen: AppCompatActivity() {

    companion object {
        private const val INITIAL_REQUEST = 1337
        private val INITIAL_PERMS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.INTERNET,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermission()
    }

    fun startApp()  {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    fun checkPermission()  {
        if(checkHasPermission().not())  {
            ActivityCompat.requestPermissions(this, INITIAL_PERMS, INITIAL_REQUEST)
        }    else {
            startApp()
        }
    }

    fun checkHasPermission():Boolean  {
        var done = true
        for(i in INITIAL_PERMS)  {
            if(isPermissionGranted(i).not())  {
                done = false
                break
            }
        }
        return done
    }

    fun isPermissionGranted(i: String): Boolean  {
        return checkSelfPermission(this, i) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode)  {
            INITIAL_REQUEST -> if(checkHasPermission())  {
                startApp()
            }  else {
                Toast.makeText(applicationContext,"All Permissions Required!!",Toast.LENGTH_SHORT)
                Handler().postDelayed({checkPermission()},0)
            }
        }
    }
}
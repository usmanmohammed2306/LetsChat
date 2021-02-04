package android.example.com.letschat.Activities

import android.content.ContentValues.TAG
import android.example.com.letschat.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.lang.Exception

class FullScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen)

        val url: String = intent.getStringExtra("imageUrl").toString()

        val image: ImageView= findViewById(R.id.a_fullscreen_image)
        val message: TextView = findViewById(R.id.a_fullscreen_message)

        message.setText("Loading Picture...")
        message.setVisibility(View.VISIBLE)

        Picasso.get()
            .load(url)
            .networkPolicy(NetworkPolicy.OFFLINE)
            .into(image,object : Callback {
                override fun onSuccess() {
                    message.visibility = View.GONE
                }

                override fun onError(e: Exception?) {
                    Log.e(TAG,"Could not load picture: $e")
                    Picasso.get()
                        .load(url)
                        .into(image,object : Callback {
                            override fun onSuccess() {
                                message.visibility = View.GONE
                            }

                            override fun onError(e: Exception?) {
                                message.text = "Error: Could not load picture."
                            }
                        })
                }
            })
    }
}
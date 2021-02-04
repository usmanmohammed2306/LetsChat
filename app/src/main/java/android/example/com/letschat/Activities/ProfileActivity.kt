package android.example.com.letschat.Activities

import android.content.Intent
import android.example.com.letschat.R
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toolbar
import com.flaviofaria.kenburnsview.KenBurnsView
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ProfileActivity : AppCompatActivity() {

    private val TAG = "CA/ProfileActivity"

    private var userDatabase: DatabaseReference? = null
    private var userListener: ValueEventListener? = null

    private var friendUserId: String = ""
    private var currentUserId: String = ""

    private lateinit var name: TextView
    private lateinit var status: TextView
    private lateinit var image: CircleImageView
    private lateinit var cover: KenBurnsView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setSupportActionBar(findViewById(R.id.friend_toolbar))

        if(supportActionBar != null)  {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }

        name = findViewById(R.id.friend_name)
        status = findViewById(R.id.friend_status)
        image = findViewById(R.id.friend_image)
        cover = findViewById(R.id.friend_cover)

        friendUserId = intent.getStringExtra("userid").toString()
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()
    }

    override fun onStart() {
        super.onStart()

        if(userDatabase != null && userListener != null)  {
            userDatabase?.removeEventListener(userListener!!)
        }

        userDatabase = FirebaseDatabase.getInstance().reference.child("Users").child(friendUserId)
        userDatabase?.keepSynced(true)
        userListener = object : ValueEventListener  {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    val layoutName: String = dataSnapshot.child("name").value.toString()
                    val layoutStatus: String = dataSnapshot.child("status").value.toString()
                    //val layoutImage: String = dataSnapshot.child("image").value.toString()
                    //val layoutCover: String = dataSnapshot.child("cover").value.toString()

                    val profileReference = Firebase.storage.reference.child("profile_images").child(
                            friendUserId + ".jpg"
                    )
                    val coverReference = Firebase.storage.reference.child("profile_covers").child(
                            friendUserId + ".jpg"
                    )

                    name.text = layoutName
                    status.text = "\"" + layoutStatus + "\""

                    profileReference.downloadUrl.addOnSuccessListener(object :
                            OnSuccessListener<Uri> {

                        override fun onSuccess(uri: Uri?) {
                            Picasso.get()
                                    .load(uri.toString())
                                    .config(Bitmap.Config.RGB_565)
                                    .fit()
                                    .centerCrop()
                                    .placeholder(R.drawable.user)
                                    .error(R.drawable.user)
                                    .into(image, object : Callback {
                                        override fun onSuccess() {
                                            Log.i(TAG, "profile upload successful!")
                                        }

                                        override fun onError(e: Exception?) {
                                            Log.e(TAG, "profile upload not successful!$e")
                                            Picasso.get()
                                                    .load(uri.toString())
                                                    .config(Bitmap.Config.RGB_565)
                                                    .fit()
                                                    .centerCrop()
                                                    .placeholder(R.drawable.user)
                                                    .error(R.drawable.user)
                                                    .into(image);
                                        }
                                    })
                            image.setOnClickListener(View.OnClickListener {
                                val intent = Intent(applicationContext, FullScreenActivity::class.java)
                                System.out.println(uri)
                                intent.putExtra("imageUrl", uri.toString())
                                startActivity(intent)
                            })
                        }
                    }).addOnFailureListener(object : OnFailureListener {
                        override fun onFailure(p0: java.lang.Exception) {
                            image.setImageResource(R.drawable.user)
                        }
                    })

                    coverReference.downloadUrl.addOnSuccessListener(object :
                            OnSuccessListener<Uri> {
                        override fun onSuccess(uri: Uri?) {
                            Picasso.get()
                                    .load(uri.toString())
                                    .config(Bitmap.Config.RGB_565)
                                    .fit()
                                    .centerCrop()
                                    .placeholder(R.drawable.logo_cover)
                                    .error(R.drawable.logo_cover)
                                    .into(cover, object : Callback {
                                        override fun onSuccess() {
                                            Log.i(TAG, "cover upload successful!")
                                        }

                                        override fun onError(e: Exception?) {
                                            Log.e(TAG, "cover upload not successful!$e")
                                            Picasso.get()
                                                    .load(uri.toString())
                                                    .config(Bitmap.Config.RGB_565)
                                                    .fit()
                                                    .centerCrop()
                                                    .placeholder(R.drawable.logo_cover)
                                                    .error(R.drawable.logo_cover)
                                                    .into(cover);
                                        }
                                    })
                            cover.setOnClickListener(View.OnClickListener {
                                val intent = Intent(applicationContext, FullScreenActivity::class.java)
                                intent.putExtra("imageUrl", uri.toString())
                                startActivity(intent)
                            })
                        }
                    }).addOnFailureListener(object : OnFailureListener {
                        override fun onFailure(p0: java.lang.Exception) {
                            cover.setImageResource(R.drawable.logo_cover)
                        }
                    })
                }
                catch (e: Exception)  {
                    Log.d(TAG, "userDatabase listener exception: " + e.message)
                    e.printStackTrace()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "userDatabase listener failed: " + error.message)
            }
        }
        userDatabase?.addValueEventListener(userListener!!)
    }

    override fun onResume() {
        super.onResume()

        FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId).child("online").setValue("true")
    }

    override fun onPause() {
        super.onPause()

        FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserId).child("online").setValue(ServerValue.TIMESTAMP)
    }

    override fun onStop() {
        super.onStop()

        removeListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == android.R.id.home)  {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    fun removeListeners()  {
        if(userDatabase != null && userListener != null)
        {
            userDatabase?.removeEventListener(userListener!!);
        }
    }
}
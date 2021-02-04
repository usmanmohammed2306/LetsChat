package android.example.com.letschat.Holders

import android.app.Activity
import android.content.Context
import android.example.com.letschat.R
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.util.TypedValue
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ChatHolder : RecyclerView.ViewHolder {

    private val TAG : String = "CA/ChatHolder"

    private var userdatabase: DatabaseReference? = null
    private var userListener: ValueEventListener? = null

    private var activity: Activity
    private var view: View
    private var context: Context

    constructor(activity: Activity,view: View,context: Context) : super(view)  {
        this.activity = activity
        this.view = view
        this.context = context
    }

    fun getView(): View {
        return view
    }

    fun setHolder(userId: String,message: String,timestamp: Long,seen: Long) {
        val userName: TextView = view.findViewById(R.id.user_name)
        val userStatus: TextView = view.findViewById(R.id.user_status)
        val userTime: TextView = view.findViewById(R.id.user_timestamp)
        val userImage: CircleImageView = view.findViewById(R.id.user_image)
        val userOnline: ImageView = view.findViewById(R.id.user_online)

        userStatus.text = message

        userTime.visibility = View.VISIBLE
        userTime.text = SimpleDateFormat("MMM d,HH:mm", Locale.getDefault()).format(timestamp)

        if(seen == 0L) {
            userStatus.setTypeface(null,Typeface.BOLD)
            userTime.setTypeface(null,Typeface.BOLD)
        }
        else {
            userStatus.setTypeface(null,Typeface.NORMAL)
            userTime.setTypeface(null,Typeface.NORMAL)
        }

        if(userdatabase != null && userListener != null)  {
            userdatabase?.removeEventListener(userListener!!)
        }

        userdatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userId)
        userListener = object : ValueEventListener  {
            var timer: Timer? = null
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val name: String = snapshot.child("name").value.toString()
                    val reference = Firebase.storage.reference.child("profile_images").child("$userId.jpg")

                    if(snapshot.hasChild("online"))  {
                        val online: String = snapshot.child("online").value.toString()

                        if(online.equals("true"))  {
                            if(timer != null)  {
                                timer?.cancel()
                                timer = null
                            }
                            userOnline.visibility = View.VISIBLE
                        }
                        else {
                            if(userName.text.toString().equals(""))  {
                                userOnline.visibility = View.GONE
                            }
                            else {
                                timer = null
                                timer?.schedule(object : TimerTask()  {
                                    override fun run()  {
                                        activity.runOnUiThread(object : Runnable {
                                            override fun run() {
                                                userOnline.visibility = View.VISIBLE
                                            }
                                        })
                                    }
                                },2000)
                            }
                        }
                    }

                    userName.text = name

                    reference.downloadUrl.addOnSuccessListener(object : OnSuccessListener<Uri> {
                        override fun onSuccess(uri: Uri?) {
                            Picasso.get()
                                    .load(uri)
                                    .config(Bitmap.Config.RGB_565)
                                    .fit()
                                    .centerCrop()
                                    .placeholder(R.drawable.user)
                                    .into(userImage,object : Callback {
                                        override fun onSuccess() {

                                        }

                                        override fun onError(e: Exception?) {
                                            Picasso.get()
                                                    .load(uri)
                                                    .config(Bitmap.Config.RGB_565)
                                                    .fit()
                                                    .centerCrop()
                                                    .placeholder(R.drawable.user)
                                                    .into(userImage)
                                        }
                                    })
                        }
                    }).addOnFailureListener(object : OnFailureListener {
                        override fun onFailure(p0: Exception) {
                            Log.e(TAG,"Failed to upload: $p0")
                            userImage.setImageResource(R.drawable.user)
                        }
                    })

                }
                catch (e: Exception)  {
                    Log.d(TAG,"userListener exception: " + e.message)
                    e.printStackTrace()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG,"userListener failed: " + error.message)
            }
        }
        userdatabase?.addValueEventListener(userListener!!)
    }
}
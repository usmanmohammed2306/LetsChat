package android.example.com.letschat.Holders


import android.example.com.letschat.R
import android.content.Context
import android.content.Intent
import android.example.com.letschat.Activities.FullScreenActivity
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*


class MessageHolder(private var view: View, private var context: Context) : RecyclerView.ViewHolder(view) {

    private val TAG = "CA/MessageHolder"

    private var userDatabase: DatabaseReference? = null
    private var chatSeenDatabase: DatabaseReference? = null
    private var chatTypingDatabase: DatabaseReference? = null
    private var userListener: ValueEventListener? = null
    private var chatSeenListener: ValueEventListener? = null
    private var chatTypingListener: ValueEventListener? = null

    fun hideBottom() {

        val messageBottom = view.findViewById<RelativeLayout>(R.id.message_relative_bottom)
        messageBottom.visibility = View.GONE
    }

    fun setLastMessage(currentUserId: String, from: String, to: String?) {

        val messageSeen = view.findViewById<TextView>(R.id.message_seen)
        val messageTyping = view.findViewById<TextView>(R.id.message_typing)
        val messageBottom = view.findViewById<RelativeLayout>(R.id.message_relative_bottom)
        messageBottom.visibility = View.VISIBLE
        var otherUserId: String? = from
        if (from == currentUserId) {
            otherUserId = to
            if (chatSeenDatabase != null && chatSeenListener != null) {
                chatSeenDatabase!!.removeEventListener(chatSeenListener!!)
            }

            // Initialize/Update seen message on the bottom of the message
            chatSeenDatabase = FirebaseDatabase.getInstance().reference.child("Chat").child(to!!)
                .child(currentUserId)
            chatSeenListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    try {
                        if (from == currentUserId && dataSnapshot.hasChild("seen")) {
                            messageSeen.visibility = View.VISIBLE
                            val seen = dataSnapshot.child("seen").value as Long
                            if (seen == 0L) {
                                messageSeen.text = "Sent"
                            } else {
                                messageSeen.text = "Seen at " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(seen)
                            }
                        } else {
                            messageSeen.visibility = View.INVISIBLE
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "chatSeenListerner exception: " + e.message)
                        e.printStackTrace()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d(TAG, "chatSeenListerner failed: " + databaseError.message)
                }
            }
            chatSeenDatabase?.addValueEventListener(chatSeenListener!!)
        } else {
            messageSeen.visibility = View.INVISIBLE
        }
        if (chatTypingDatabase != null && chatTypingListener != null) {
            chatTypingDatabase?.removeEventListener(chatTypingListener!!)
        }

        // Initialize/Update typing status on the bottom
        chatTypingDatabase = FirebaseDatabase.getInstance().reference.child("Chat").child(
            otherUserId!!
        ).child(currentUserId)
        chatTypingListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    if (dataSnapshot.hasChild("typing")) {
                        val typing = dataSnapshot.child("typing").value.toString().toInt()
                        messageTyping.visibility = View.VISIBLE
                        if (typing == 1) {
                            messageTyping.text = "Typing..."
                        } else if (typing == 2) {
                            messageTyping.text = "Deleting..."
                        } else if (typing == 3) {
                            messageTyping.text = "Thinking..."
                        } else {
                            messageTyping.visibility = View.INVISIBLE
                        }
                    } else {
                        messageTyping.visibility = View.INVISIBLE
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "chatTypingListener exception: " + e.message)
                    e.printStackTrace()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "chatTypingListener failed: " + databaseError.message)
            }
        }
        chatTypingDatabase?.addValueEventListener(chatTypingListener!!)
    }

    fun setRightMessage(userid: String?, message: String?, time: Long, type: String) {
        // If this an upcoming message
        val messageLayoutLeft = view.findViewById<RelativeLayout>(R.id.message_relative_left)
        val messageLayoutRight = view.findViewById<RelativeLayout>(R.id.message_relative_right)
        val messageTextRight = view.findViewById<TextView>(R.id.message_text_right)
        val messageTimeRight = view.findViewById<TextView>(R.id.message_time_right)
        val messageImageRight: CircleImageView = view.findViewById(R.id.message_image_right)
        val messageTextPictureRight = view.findViewById<ImageView>(R.id.message_imagetext_right)
        val messageLoadingRight = view.findViewById<TextView>(R.id.message_loading_right)
        messageLayoutLeft.visibility = View.GONE
        messageLayoutRight.visibility = View.VISIBLE
        if (type == "text") {
            messageTextPictureRight.visibility = View.GONE
            messageLoadingRight.visibility = View.GONE
            messageTextRight.visibility = View.VISIBLE
            messageTextRight.text = message
        } else {
            messageTextRight.visibility = View.GONE
            messageTextPictureRight.visibility = View.VISIBLE
            messageLoadingRight.visibility = View.VISIBLE
            messageLoadingRight.text = "Loading picture..."
            Picasso.get()
                .load(message)
                .fit()
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(messageTextPictureRight, object : Callback {
                    override fun onSuccess() {
                        messageLoadingRight.visibility = View.GONE
                    }

                    override fun onError(e: java.lang.Exception?) {
                        Picasso.get()
                            .load(message)
                            .fit()
                            .into(messageTextPictureRight, object : Callback {
                                override fun onSuccess() {
                                    messageLoadingRight.visibility = View.GONE
                                }

                                override fun onError(e: java.lang.Exception?) {
                                    messageLoadingRight.text = "Error: could not load picture."
                                }
                            })
                    }
                })
            messageTextPictureRight.setOnClickListener {
                val intent = Intent(context, FullScreenActivity::class.java)
                intent.putExtra("imageUrl", message)
                context.startActivity(intent)
            }
        }
        messageTimeRight.text =
            if (DateUtils.isToday(time)) SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(time) else SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(time)
        if (userDatabase != null && userListener != null) {
            userDatabase!!.removeEventListener(userListener!!)
        }

        // Initialize/Update user image
        userDatabase = FirebaseDatabase.getInstance().reference.child("Users").child(userid!!)
        userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    val reference = Firebase.storage.reference.child("profile_images").child("$userid.jpg")
                    reference.downloadUrl.addOnSuccessListener(object : OnSuccessListener<Uri> {
                        override fun onSuccess(uri: Uri?) {
                            Picasso.get()
                                    .load(uri)
                                    .config(Bitmap.Config.RGB_565)
                                    .fit()
                                    .centerCrop()
                                    .placeholder(R.drawable.user)
                                    .into(messageImageRight,object : Callback {
                                        override fun onSuccess() {

                                        }

                                        override fun onError(e: java.lang.Exception?) {
                                            Picasso.get()
                                                    .load(uri)
                                                    .config(Bitmap.Config.RGB_565)
                                                    .fit()
                                                    .centerCrop()
                                                    .placeholder(R.drawable.user)
                                                    .into(messageImageRight)
                                        }
                                    })
                        }
                    }).addOnFailureListener(object : OnFailureListener {
                        override fun onFailure(p0: java.lang.Exception) {
                            Log.e(TAG,"Failed to upload: $p0")
                            messageImageRight.setImageResource(R.drawable.user)
                        }
                    })
                } catch (e: Exception) {
                    Log.d(TAG, "userDatabase exception: " + e.message)
                    e.printStackTrace()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "userDatabase failed: " + databaseError.message)
            }
        }
        userDatabase?.addValueEventListener(userListener!!)
    }

    fun setLeftMessage(userid: String?, message: String?, time: Long, type: String) {
        // If this is a sent message
        val messageLayoutRight = view.findViewById<RelativeLayout>(R.id.message_relative_right)
        val messageLayoutLeft = view.findViewById<RelativeLayout>(R.id.message_relative_left)
        val messageTextLeft = view.findViewById<TextView>(R.id.message_text_left)
        val messageTimeLeft = view.findViewById<TextView>(R.id.message_time_left)
        val messageImageLeft: CircleImageView = view.findViewById(R.id.message_image_left)
        val messageTextPictureLeft = view.findViewById<ImageView>(R.id.message_imagetext_left)
        val messageLoadingLeft = view.findViewById<TextView>(R.id.message_loading_left)
        messageLayoutRight.visibility = View.GONE
        messageLayoutLeft.visibility = View.VISIBLE
        if (type == "text") {
            messageTextPictureLeft.visibility = View.GONE
            messageLoadingLeft.visibility = View.GONE
            messageTextLeft.visibility = View.VISIBLE
            messageTextLeft.text = message
        } else {
            messageTextLeft.visibility = View.GONE
            messageTextPictureLeft.visibility = View.VISIBLE
            messageLoadingLeft.visibility = View.VISIBLE
            messageLoadingLeft.text = "Loading picture..."
            Picasso.get()
                .load(message)
                .fit()
                .networkPolicy(NetworkPolicy.OFFLINE)
                .into(messageTextPictureLeft, object : Callback {
                    override fun onSuccess() {
                        messageLoadingLeft.visibility = View.GONE
                    }

                    override fun onError(e: java.lang.Exception?) {
                        Picasso.get()
                            .load(message)
                            .fit()
                            .into(messageTextPictureLeft, object : Callback {
                                override fun onSuccess() {
                                    messageLoadingLeft.visibility = View.GONE
                                }

                                override fun onError(e: java.lang.Exception?) {
                                    messageLoadingLeft.text = "Error: could not load picture.$message"
                                }
                            })
                    }
                })
            messageTextPictureLeft.setOnClickListener {
                val intent = Intent(context, FullScreenActivity::class.java)
                intent.putExtra("imageUrl", message)
                context.startActivity(intent)
            }
        }
        messageTimeLeft.text =
            if (DateUtils.isToday(time)) SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(time) else SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(time)
        if (userDatabase != null && userListener != null) {
            userDatabase!!.removeEventListener(userListener!!)
        }

        // Initilize/Update user image
        userDatabase = FirebaseDatabase.getInstance().reference.child("Users").child(userid!!)
        userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    val reference = Firebase.storage.reference.child("profile_images").child("$userid.jpg")
                    reference.downloadUrl.addOnSuccessListener(object : OnSuccessListener<Uri> {
                        override fun onSuccess(uri: Uri?) {
                            Picasso.get()
                                    .load(uri)
                                    .config(Bitmap.Config.RGB_565)
                                    .fit()
                                    .centerCrop()
                                    .placeholder(R.drawable.user)
                                    .into(messageImageLeft,object : Callback {
                                        override fun onSuccess() {

                                        }

                                        override fun onError(e: java.lang.Exception?) {
                                            Picasso.get()
                                                    .load(uri)
                                                    .config(Bitmap.Config.RGB_565)
                                                    .fit()
                                                    .centerCrop()
                                                    .placeholder(R.drawable.user)
                                                    .into(messageImageLeft)
                                        }
                                    })
                        }
                    }).addOnFailureListener(object : OnFailureListener {
                        override fun onFailure(p0: java.lang.Exception) {
                            Log.e(TAG,"Failed to upload: $p0")
                            messageImageLeft.setImageResource(R.drawable.user)
                        }
                    })
                } catch (e: Exception) {
                    Log.d(TAG, "userDatabase exception: " + e.message)
                    e.printStackTrace()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "userDatabase failed: " + databaseError.message)
            }
        }
        userDatabase?.addValueEventListener(userListener!!)
    }
}
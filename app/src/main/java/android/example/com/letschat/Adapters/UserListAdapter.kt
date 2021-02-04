package android.example.com.letschat.Adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.example.com.letschat.Activities.ChatActivity
import android.example.com.letschat.Activities.FullScreenActivity
import android.example.com.letschat.Activities.ProfileActivity
import android.example.com.letschat.R
import android.example.com.letschat.Models.User
import android.graphics.Bitmap
import android.net.Uri
import android.nfc.Tag
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI.getApplicationContext
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.lang.Exception
import java.security.AccessController.getContext
import kotlin.collections.ArrayList

class UserListAdapter(val userList: MutableList<User>) : RecyclerView.Adapter<UserListAdapter.UserListViewHolder>() {

    //lateinit var userList: ArrayList<User>

    //constructor(userList: ArrayList<User>)  {
      //  this.userList = userList
    //}

    private val TAG = "CA/ContactActivityAdapter"

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): UserListViewHolder {

        val layoutView: View = LayoutInflater.from(parent.context).inflate(R.layout.userslist,null, false)
        val lp = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutView.layoutParams = lp
        return UserListViewHolder(layoutView)
    }

    override fun onBindViewHolder(holder: UserListViewHolder, position: Int) {
        holder.mName.text = userList[position].username
        holder.mPhone.text = userList[position].phone

        val reference = Firebase.storage.reference.child("profile_images").child(
            userList[position].id + ".jpg"
        )

        reference.downloadUrl.addOnSuccessListener(object : OnSuccessListener<Uri> {
            override fun onSuccess(uri: Uri?) {
                Picasso.get()
                    .load(uri.toString())
                    .config(Bitmap.Config.RGB_565)
                    .fit()
                    .centerCrop()
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(holder.mImage,object : Callback {
                        override fun onSuccess() {
                            Log.i(TAG,"Successful upload")
                        }

                        override fun onError(e: Exception?) {
                            Picasso.get()
                                .load(uri.toString())
                                .config(Bitmap.Config.RGB_565)
                                .fit()
                                .centerCrop()
                                .placeholder(R.drawable.user)
                                .error(R.drawable.user)
                                .into(holder.mImage)
                        }
                    })
                holder.mImage.setOnClickListener {view ->
                    val userProfileIntent = Intent(view.context, ProfileActivity::class.java)
                    userProfileIntent.putExtra("userid",userList[position].id.toString())
                    view.context.startActivity(userProfileIntent)
                }
            }
        }).addOnFailureListener(object : OnFailureListener {
            override fun onFailure(p0: Exception) {
                Log.e(TAG,"Failed to upload: $p0")
                holder.mImage.setImageResource(R.drawable.user)
            }
        })


        holder.mLayout.setOnClickListener {view ->
            System.out.println("layout touched")
            val sendMessageIntent  = Intent(view.context, ChatActivity::class.java)
            sendMessageIntent.putExtra("userid",userList[position].id.toString())
            view.context.startActivity(sendMessageIntent)
        }
    }


    override fun getItemCount(): Int {
        return userList.size
    }

    inner class UserListViewHolder : RecyclerView.ViewHolder {
        var mName: TextView
        var mPhone: TextView
        var mLayout: LinearLayout
        var mImage: CircleImageView
        constructor(view: View) : super(view)  {
            mName = view.findViewById(R.id.users_name)
            mPhone = view.findViewById(R.id.users_number)
            mLayout = view.findViewById(R.id.contact_layout)
            mImage = view.findViewById(R.id.users_picture)
        }

    }
}
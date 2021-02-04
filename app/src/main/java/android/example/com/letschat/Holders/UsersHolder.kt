package android.example.com.letschat.Holders

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class UsersHolder : RecyclerView.ViewHolder {

    private val TAG : String = "CA/UsersHolder"

    private var userdatabase: DatabaseReference? = null
    private var userListener: ValueEventListener? = null

    private var activity: Activity
    private var view: View
    private var context: Context

    constructor(activity: Activity, view: View, context: Context) : super(view)  {
        this.activity = activity
        this.view = view
        this.context = context
    }

    fun getView(): View {
        return view
    }

    fun setHolder(userId: String)  {
        //val username: TextView = view.findViewById(R.id.users_name)
        //val userImage: ImageView = view.findViewById(R.id.users_image)

        /*if(userdatabase != null && userListener != null)  {
            userdatabase?.removeEventListener(userListener!!)
        }

        //userdatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(userId)
        userListener = object : ValueEventListener  {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val name: String = snapshot.child("name").getValue().toString()
                    val image: String = snapshot.child("image").getValue().toString()

                    username.text = name

                    if(!image.equals("default"))  {
                        Picasso.get()
                            .load(image)
                            .resize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,50F, context.resources.displayMetrics).toInt(),
                                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,50F, context.resources.displayMetrics).toInt())
                            .centerCrop()
                            .placeholder(R.drawable.user)
                            .into(userImage,object : Callback {
                                override fun onSuccess() {

                                }

                                override fun onError(e: Exception?) {
                                    Picasso.get()
                                        .load(image)
                                        .resize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,50F, context.resources.displayMetrics).toInt(),
                                            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,50F, context.resources.displayMetrics).toInt())
                                        .centerCrop()
                                        .placeholder(R.drawable.user)
                                        .into(userImage)
                                }
                            })
                    }
                    else {
                        userImage.setImageResource(R.drawable.user)
                    }
                }
                catch (e : Exception)  {
                    //Log.d(TAG,"userlistener exception: " + e.message)
                    //e.printStackTrace()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG,"userlistener failed: " + error.message)
            }
        }
        userdatabase?.addValueEventListener(userListener!!) */
    }
}
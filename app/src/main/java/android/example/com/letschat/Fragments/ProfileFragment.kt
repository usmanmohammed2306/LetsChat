package android.example.com.letschat.Fragments

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.example.com.letschat.Activities.FullScreenActivity
import android.example.com.letschat.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.flaviofaria.kenburnsview.KenBurnsView
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import id.zelory.compressor.Compressor
import id.zelory.compressor.Compressor.compress
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.destination
import jahirfiquitiva.libs.fabsmenu.FABsMenu
import jahirfiquitiva.libs.fabsmenu.TitleFAB
import java.io.File
import java.io.FileOutputStream


class ProfileFragment : Fragment()  {

    //IOnBackPressed

    private val TAG = "CA/ProfileFragment"

    private var userDatabase: DatabaseReference? = null
    private var userListener: ValueEventListener? = null

    private var currentUserId: String = ""

    private lateinit var name: TextView
    private lateinit var status:TextView
    private lateinit var image: CircleImageView
    private lateinit var cover: KenBurnsView
    private lateinit var menu: FABsMenu
    private var button1: TitleFAB? = null
    private var button2: TitleFAB? = null
    private var button3: TitleFAB? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view: View = inflater.inflate(R.layout.fragment_profile, container, false)

        name = view.findViewById(R.id.profile_name)
        status = view.findViewById(R.id.profile_status)
        image = view.findViewById(R.id.profile_image)
        menu = view.findViewById(R.id.profile_fabs_menu)
        cover = view.findViewById(R.id.profile_cover)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()

        return view
    }

    override fun onStart() {
        super.onStart()

        Log.i(TAG, "onStart function overriden")

        if(userDatabase != null && userListener != null)  {
            userDatabase?.removeEventListener(userListener!!)
        }

        userDatabase = FirebaseDatabase.getInstance().reference.child("Users").child(currentUserId)
        userDatabase?.keepSynced(true)
        userListener = object : ValueEventListener  {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    val layoutName: String = dataSnapshot.child("name").value.toString()
                    val layoutStatus: String = dataSnapshot.child("status").value.toString()
                    val layoutImage: String = dataSnapshot.child("image").value.toString()
                    val layoutCover: String = dataSnapshot.child("cover").value.toString()

                    val profileReference = Firebase.storage.reference.child("profile_images").child(
                            currentUserId + ".jpg"
                    )
                    val coverReference = Firebase.storage.reference.child("profile_covers").child(
                            currentUserId + ".jpg"
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
                            image.setOnClickListener {
                                val intent = Intent(context, FullScreenActivity::class.java)
                                //System.out.println(uri.toString())
                                intent.putExtra("imageUrl", uri.toString())
                                startActivity(intent)
                            }
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
                                val intent = Intent(context, FullScreenActivity::class.java)
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

        initMyProfile()
    }

    override fun onResume() {
        super.onResume()

        FirebaseDatabase.getInstance().reference.child("Users").child(currentUserId).child("online").setValue(
                "true"
        )
    }

    override fun onPause() {
        super.onPause()

        FirebaseDatabase.getInstance().reference.child("Users").child(currentUserId).child("online").setValue(
                ServerValue.TIMESTAMP
        )
    }

    override fun onStop() {
        super.onStop()

        removeListeners()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 1 && resultCode == RESULT_OK)  {
            val url: Uri? = data?.data

            val file: StorageReference = FirebaseStorage.getInstance().reference.child("profile_images").child(
                    currentUserId + ".jpg"
            )

            file.putFile(url!!).addOnCompleteListener { task: Task<UploadTask.TaskSnapshot> ->
                if (task.isSuccessful) {
                    val imageUrl: String = url.toString()
                    // Updating image on user data
                    userDatabase!!.child("image").setValue(imageUrl)
                        .addOnCompleteListener { task: Task<Void> ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Picture updated", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.d(
                                        TAG,
                                        "updateImage listener failed: " + task.exception!!.message
                                )
                            }
                        }
                }
                else {
                    Log.d(TAG, "uploadImage listener failed: " + task.exception!!.message)
                }
            }
        }
        else if(requestCode == 2 && resultCode == RESULT_OK)  {
            val url: Uri? = data?.data

            val file: StorageReference = FirebaseStorage.getInstance().reference.child("profile_covers").child(
                    currentUserId + ".jpg"
            )

            file.putFile(url!!).addOnCompleteListener { task: Task<UploadTask.TaskSnapshot> ->
                if (task.isSuccessful) {
                    val imageUrl: String = url.toString()

                    // Updating image on user data
                    userDatabase!!.child("cover").setValue(imageUrl)
                        .addOnCompleteListener { task: Task<Void> ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Cover updated", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.d(
                                        TAG,
                                        "updateUserCover listener failed: " + task.exception!!.message
                                )
                            }
                        }
                }
                else {
                    Log.d(TAG, "uploadCover listener failed: " + task.exception!!.message)
                }
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId)  {
            R.id.home -> activity?.finish()
        }

        return super.onOptionsItemSelected(item)
    }

    fun initMyProfile()  {

        if(button1?.title != "Change Cover") {

            button1 = TitleFAB(context)
            button1?.title = "Change Cover"
            button1?.setBackgroundColor(resources.getColor(R.color.coolPurpleColor))
            button1?.setRippleColor(resources.getColor(R.color.pastelPurpleColor))
            button1?.setImageResource(R.drawable.ic_filter_hdr_white_24dp)
            button1?.setOnClickListener {
                val gallery = Intent()
                gallery.type = "image/*"
                gallery.action = Intent.ACTION_OPEN_DOCUMENT
                startActivityForResult(Intent.createChooser(gallery, "Select Cover"), 2)

                menu.collapse()
            }
            menu.addButton(button1)
        }

        if(button2?.title != "Change Image") {
            button2 = TitleFAB(context)
            button2?.title = "Change Image"
            button2?.setBackgroundColor(resources.getColor(R.color.moneyGreenColor))
            button2?.setRippleColor(resources.getColor(R.color.cactusGreenColor))
            button2?.setImageResource(R.drawable.ic_image_white_24dp)
            button2?.setOnClickListener {
                val gallery = Intent()
                gallery.type = "image/*"
                gallery.action = Intent.ACTION_OPEN_DOCUMENT
                startActivityForResult(Intent.createChooser(gallery, "Select Image"), 1)

                menu.collapse()
            }
            menu.addButton(button2)
        }
        if(button3?.title != "Change Status") {
            button3 = TitleFAB(context)
            button3?.title = "Change Status"
            button3?.setBackgroundColor(resources.getColor(R.color.skyBlueColor))
            button3?.setRippleColor(resources.getColor(R.color.midnightBlueColor))
            button3?.setImageResource(R.drawable.ic_edit_white_24dp)
            button3?.setOnClickListener {

                menu.collapse()

                val builder = AlertDialog.Builder(context);
                builder.setTitle("Enter your new Status:")

                val mView: View = layoutInflater.inflate(R.layout.status_dialog, null)

                val tmp: EditText = mView.findViewById(R.id.status_text)

                builder.setPositiveButton(
                        "Update"
                ) { dialogInterface, i ->
                    val newStatus: String = tmp.text.toString()

                    if (newStatus.length < 1 || newStatus.length > 24) {
                        Toast.makeText(
                                context,
                                "Status must be between 1-24 characters",
                                Toast.LENGTH_LONG
                        ).show()
                        dialogInterface.dismiss()
                    } else {
                        userDatabase?.child("status")?.setValue(newStatus)
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Toast.makeText(
                                            context,
                                            task.exception?.message,
                                            Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                    }
                }
                builder.setNegativeButton("Cancel") { dialogInterface, i ->
                    dialogInterface.dismiss()
                }
                builder.setView(mView)
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
            menu.addButton(button3)
        }
    }

    fun removeListeners() {
        if (userDatabase != null && userListener != null) {
            userDatabase!!.removeEventListener(userListener!!)
        }
    }

    private fun getRightAngleImage(photoPath: String): String? {
        try {
            val ei = ExifInterface(photoPath)
            val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            var degree = 0
            degree = when (orientation) {
                ExifInterface.ORIENTATION_NORMAL -> 0
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                ExifInterface.ORIENTATION_UNDEFINED -> 0
                else -> 90
            }
            return rotateImage(degree, photoPath)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return photoPath
    }

    private fun rotateImage(degree: Int, imagePath: String): String? {
        if (degree <= 0) {
            return imagePath
        }
        try {
            var b: Bitmap = BitmapFactory.decodeFile(imagePath)
            val matrix = Matrix()
            if (b.getWidth() > b.getHeight()) {
                matrix.setRotate(degree.toFloat())
                b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(),
                        matrix, true)
            }
            val fOut = FileOutputStream(imagePath)
            val imageName = imagePath.substring(imagePath.lastIndexOf("/") + 1)
            val imageType = imageName.substring(imageName.lastIndexOf(".") + 1)
            val out = FileOutputStream(imagePath)
            if (imageType.equals("png", ignoreCase = true)) {
                b.compress(Bitmap.CompressFormat.PNG, 100, out)
            } else if (imageType.equals("jpeg", ignoreCase = true) || imageType.equals("jpg", ignoreCase = true)) {
                b.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            fOut.flush()
            fOut.close()
            b.recycle()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return imagePath
    }
}

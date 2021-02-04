package android.example.com.letschat.Activities

import android.content.Context
import android.content.Intent
import android.example.com.letschat.Adapters.MessageAdapter
import android.example.com.letschat.Fragments.APIService
import android.example.com.letschat.Models.Message
import android.example.com.letschat.Notifications.*
import android.example.com.letschat.R
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.webkit.WebChromeClient
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.webkit.PermissionRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.HashMap


class ChatActivity : AppCompatActivity() {

    private val TAG = "CA/ChatActivity"

    private lateinit var messageEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var sendButton: Button
    private lateinit var sendPictureButton: ImageView

    private var userDatabase: DatabaseReference?= null
    private var chatDatabase:DatabaseReference? = null
    private var userListener: ValueEventListener? = null
    private var chatListener:ValueEventListener? = null

    private lateinit var messagesAdapter: MessageAdapter
    private var messagesList: MutableList<Message> = mutableListOf()

    private lateinit var appBarName: TextView
    private lateinit var appBarSeen:TextView

    var notify: Boolean = false

    private var messagesDatabase: Query? = null
    private var messagesListener: ChildEventListener? = null

    private lateinit var currentUserId: String

    lateinit var apiService: APIService

    companion object {
        lateinit var otherUserId: String
        var running: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        running = true

        messageEditText = findViewById(R.id.chat_message)
        recyclerView = findViewById(R.id.chat_recycler)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        otherUserId = intent.getStringExtra("userid").toString()

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        messagesAdapter = MessageAdapter(messagesList)

        recyclerView.adapter = messagesAdapter

        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.emeraldColor)))
        actionBar?.setDisplayShowCustomEnabled(true)
        actionBar?.setTitle("")

        val inflater: LayoutInflater = this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val actionBarView: View = inflater.inflate(R.layout.chat_bar, null)

        appBarName = actionBarView.findViewById(R.id.chat_bar_name)
        appBarSeen = actionBarView.findViewById(R.id.chat_bar_seen)

        actionBar?.customView = actionBarView

        apiService = Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)

        sendButton = findViewById(R.id.chat_send)
        sendButton.setOnClickListener{
            notify = true
            sendMessage()
        }

        sendPictureButton = findViewById(R.id.chat_send_picture)
        sendPictureButton.setOnClickListener {
            val galleryIntent = Intent()
            galleryIntent.type = "image/*"
            galleryIntent.action = Intent.ACTION_OPEN_DOCUMENT

            startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), 1)
        }

        messageEditText.addTextChangedListener(object : TextWatcher {
            private var timer: Timer? = null

            override fun beforeTextChanged(charsequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charsequence: CharSequence?, i: Int, i1: Int, i2: Int) {
                if (messagesList.size > 0) {
                    if (charsequence?.length == 0) {
                        FirebaseDatabase.getInstance().reference.child("Chat").child(currentUserId)
                            .child(
                                otherUserId
                            ).child("typing").setValue(0)
                        timer?.cancel()
                    } else if (i2 > 0) {
                        FirebaseDatabase.getInstance().reference.child("Chat").child(currentUserId)
                            .child(
                                otherUserId
                            ).child("typing").setValue(1)

                        timer?.cancel()
                        timer = null
                        timer?.schedule(object : TimerTask() {
                            override fun run() {
                                FirebaseDatabase.getInstance().reference.child("Chat").child(
                                    currentUserId
                                ).child(otherUserId).child("typing").setValue(3)
                            }
                        }, 5000)
                    } else if (i1 > 0) {
                        FirebaseDatabase.getInstance().reference.child("Chat").child(currentUserId)
                            .child(
                                otherUserId
                            ).child("typing").setValue(2)

                        timer?.cancel()
                        timer = null
                        timer?.schedule(object : TimerTask() {
                            override fun run() {
                                FirebaseDatabase.getInstance().reference.child("Chat").child(
                                    currentUserId
                                ).child(otherUserId).child("typing").setValue(3)
                            }
                        }, 5000)
                    }
                }
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        val root = findViewById<RelativeLayout>(R.id.chat_root)
        root.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            var previousHeight = root.rootView.height - root.height - recyclerView.height
            override fun onGlobalLayout() {
                val height = root.rootView.height - root.height - recyclerView.height
                if (previousHeight != height) {
                    if (previousHeight > height) {
                        previousHeight = height
                    } else if (previousHeight < height) {
                        recyclerView.scrollToPosition(messagesList.size - 1)
                        previousHeight = height
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        running = true
        FirebaseDatabase.getInstance().reference.child("Users").child(currentUserId).child("online")
            .setValue("true")
        loadMessages()
        initDatabases()
    }

    private fun loadMessages() {
        messagesList.clear()

        messagesDatabase = FirebaseDatabase.getInstance().reference.child("Messages").child(
            currentUserId
        ).child(otherUserId)
        messagesListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                try {
                    val message: Message? = dataSnapshot.getValue(Message::class.java)
                    messagesList.add(message!!)
                    messagesAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messagesList.size - 1)
                } catch (e: Exception) {
                    Log.d(
                        TAG,
                        "loadMessages(): onChildAdded(): messegesListener exception: " + e.message
                    )
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                messagesAdapter.notifyDataSetChanged()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                messagesAdapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                messagesAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(
                    TAG,
                    "loadMessages(): onCancelled(): messegesListener failed: " + databaseError.message
                )
            }
        }
        messagesDatabase?.addChildEventListener(messagesListener!!)
    }

    fun initDatabases() {
        userDatabase = FirebaseDatabase.getInstance().reference.child("Users").child(otherUserId)
        userListener = object : ValueEventListener {
            var timer: Timer? = null
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    val name: String = dataSnapshot.child("name").value.toString()
                    appBarName.text = name
                    val online: String = dataSnapshot.child("online").value.toString()
                    if (online == "true") {
                        if (timer != null) {
                            timer?.cancel()
                            timer = null
                        }
                        appBarSeen.text = "Online"
                    } else {
                        if (appBarSeen.text.isEmpty()) {
                            appBarSeen.text = "Last Seen: " + getTimeAgo(online.toLong())
                        } else {
                            timer = Timer()
                            timer!!.schedule(object : TimerTask() {
                                override fun run() {
                                    runOnUiThread {
                                        appBarSeen.text =
                                            "Last Seen: " + getTimeAgo(online.toLong())
                                    }
                                }
                            }, 2000)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "setDatabase(): usersOtherUserListener exception: " + e.message)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "setDatabase(): usersOtherUserListener failed: " + databaseError.message)
            }
        }
        userDatabase!!.addValueEventListener(userListener!!)

        chatDatabase = FirebaseDatabase.getInstance().reference.child("Chat").child(currentUserId)
            .child(otherUserId)
        chatListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    if (dataSnapshot.hasChild("seen")) {
                        val seen = dataSnapshot.child("seen").getValue() as Long
                        if (seen == 0L) {
                            chatDatabase!!.child("seen").setValue(ServerValue.TIMESTAMP)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "setDatabase(): chatCurrentUserListener exception: " + e.message)
                    e.printStackTrace()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(
                    TAG,
                    "setDatabase(): chatCurrentUserListener failed: " + databaseError.message
                )
            }
        }
        chatDatabase!!.addValueEventListener(chatListener!!)
    }

    override fun onPause() {
        super.onPause()
        running = false
        FirebaseDatabase.getInstance().reference.child("Users").child(currentUserId).child("online")
            .setValue(ServerValue.TIMESTAMP)
        if (messagesList.size > 0 && messageEditText.text.length > 0) {
            FirebaseDatabase.getInstance().reference.child("Chat").child(currentUserId)
                .child(otherUserId).child("typing").setValue(0)
        }
        removeListeners()
    }

    fun removeListeners()  {
        try {
            chatDatabase?.removeEventListener(chatListener!!)
            chatListener = null
            userDatabase?.removeEventListener(userListener!!)
            userListener = null
            messagesDatabase?.removeEventListener(messagesListener!!)
            messagesListener = null
        } catch (e: java.lang.Exception) {
            Log.d(TAG, "exception: " + e.message)
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> NavUtils.navigateUpFromSameTask(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode== 1 && resultCode == RESULT_OK)  {
            val url: Uri? = data?.data

            val messageRef = FirebaseDatabase.getInstance().reference.child("Messages").child(
                currentUserId
            ).child(otherUserId).push()
            val messageId = messageRef.key

            val notificationRef = FirebaseDatabase.getInstance().reference.child("Notifications").child(
                otherUserId
            ).push()
            val notificationId = notificationRef.key

            val file = FirebaseStorage.getInstance().reference.child("message_images").child("$messageId.jpg")

            file.putFile(url!!)
                .addOnCompleteListener(object : OnCompleteListener<UploadTask.TaskSnapshot?> {
                    override fun onComplete(task: Task<UploadTask.TaskSnapshot?>) {
                        if (task.isSuccessful()) {
                            val ref = Firebase.storage.reference.child("message_images").child("$messageId.jpg")
                            ref.downloadUrl.addOnSuccessListener(object : OnSuccessListener<Uri>  {
                                override fun onSuccess(uri: Uri?) {
                                    Log.i(TAG,"Successful to download")
                                    val imageUrl: String = uri.toString()
                                    val messageMap: MutableMap<String, Any?> = HashMap()
                                    messageMap["message"] = imageUrl
                                    messageMap["type"] = "image"
                                    messageMap["from"] = currentUserId
                                    messageMap["to"] = otherUserId
                                    messageMap["timestamp"] = ServerValue.TIMESTAMP
                                    val notificationData: HashMap<String, String> = HashMap()
                                    notificationData["from"] = currentUserId
                                    notificationData["type"] = "message"
                                    val userMap: MutableMap<String, Any?> = HashMap()
                                    userMap["Messages/$currentUserId/$otherUserId/$messageId"] = messageMap
                                    userMap["Messages/$otherUserId/$currentUserId/$messageId"] = messageMap
                                    userMap["Chat/$currentUserId/$otherUserId/message"] =
                                        "You have sent a picture."
                                    userMap["Chat/$currentUserId/$otherUserId/timestamp"] =
                                        ServerValue.TIMESTAMP
                                    userMap["Chat/$currentUserId/$otherUserId/seen"] = ServerValue.TIMESTAMP
                                    userMap["Chat/$otherUserId/$currentUserId/message"] =
                                        "Has send you a picture."
                                    userMap["Chat/$otherUserId/$currentUserId/timestamp"] =
                                        ServerValue.TIMESTAMP
                                    userMap["Chat/$otherUserId/$currentUserId/seen"] = 0
                                    userMap["Notifications/$otherUserId/$notificationId"] = notificationData
                                    FirebaseDatabase.getInstance().reference.updateChildren(userMap) { databaseError, databaseReference ->
                                        sendButton.isEnabled = true
                                        if (databaseError != null) {
                                            Log.d(
                                                TAG,
                                                "sendMessage(): updateChildren failed: " + databaseError.message
                                            )
                                        }
                                    }
                                }
                            }).addOnFailureListener(object : OnFailureListener {
                                override fun onFailure(p0: java.lang.Exception) {
                                    Log.e(TAG,"failed to download:$p0")
                                }
                            })
                        }
                    }
                })
        }
    }

    fun sendMessage()  {
        sendButton.isEnabled = false
        val message: String = messageEditText.text.toString()

        if(message.length == 0) {
            Toast.makeText(applicationContext, "Message cannot be empty", Toast.LENGTH_SHORT).show()
        }
        else {
            messageEditText.setText("")

            val userMessage: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Messages").child(
                currentUserId
            ).child(otherUserId).push()
            val pushId = userMessage.key

            val notificationRef: DatabaseReference = FirebaseDatabase.getInstance().reference.child(
                "Notifications"
            ).child(otherUserId).push()
            val notificationid: String = notificationRef.key.toString()

            val messageMap: MutableMap<String, Any?> = HashMap()
            messageMap["message"] = message
            messageMap["type"] = "text"
            messageMap["from"] = currentUserId
            messageMap["to"] = otherUserId
            messageMap["timestamp"] = ServerValue.TIMESTAMP

            val notificationData: HashMap<String, String> = HashMap()
            notificationData["from"] = currentUserId
            notificationData["type"] = "message"

            val userMap: MutableMap<String, Any?> = HashMap()
            userMap["Messages/$currentUserId/$otherUserId/$pushId"] = messageMap
            userMap["Messages/$otherUserId/$currentUserId/$pushId"] = messageMap

            userMap["Chat/$currentUserId/$otherUserId/message"] = message
            userMap["Chat/$currentUserId/$otherUserId/timestamp"] = ServerValue.TIMESTAMP
            userMap["Chat/$currentUserId/$otherUserId/seen"] = ServerValue.TIMESTAMP

            userMap["Chat/$otherUserId/$currentUserId/message"] = message
            userMap["Chat/$otherUserId/$currentUserId/timestamp"] = ServerValue.TIMESTAMP
            userMap["Chat/$otherUserId/$currentUserId/seen"] = 0

            userMap["Notifications/$otherUserId/$notificationid"] = notificationData

            FirebaseDatabase.getInstance().reference.updateChildren(userMap,
                object : DatabaseReference.CompletionListener {
                    override fun onComplete(error: DatabaseError?, ref: DatabaseReference) {
                        sendButton.isEnabled = true

                        if (error != null) {
                            Log.d(TAG, "sendMessage(): updateChildren failed: " + error.message)
                        }
                    }
                })
        }
        val msg : String = message
        val reference = FirebaseDatabase.getInstance().reference.child("Users").child(currentUserId).child("name")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user: String = snapshot.value.toString()
                if(notify)  {
                    sendNotification(otherUserId,user,msg)
                }
                notify = false
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendNotification(receiver: String, username: String, message: String) {
        val tokens = FirebaseDatabase.getInstance().getReference("Tokens")
        val query = tokens.orderByKey().equalTo(receiver)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val token: Token? = snapshot.getValue(Token::class.java)
                    val data = Data(currentUserId, R.drawable.mylogo, "$message", "$username", otherUserId)
                    val sender = Sender(data, token?.token!!)
                    apiService.sendNotification(sender)!!.enqueue(object : Callback<MyResponse?> {
                        override fun onResponse(call: Call<MyResponse?>, response: Response<MyResponse?>) {
                            if (response.code() == 200) {
                                if (response.body()!!.success != 1) {
                                    Toast.makeText(
                                        this@ChatActivity,
                                        "Failed!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        override fun onFailure(call: Call<MyResponse?>?, t: Throwable?) {}
                        })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }


    private fun getTimeAgo(time: Long): String? {
        val diff = System.currentTimeMillis() - time
        if (diff < 1) {
            return " just now"
        }
        return if (diff < 60 * 1000) {
            if (diff / 1000 < 2) {
                (diff / 1000).toString() + " second ago"
            } else {
                (diff / 1000).toString() + " seconds ago"
            }
        } else if (diff < 60 * (60 * 1000)) {
            if (diff / (60 * 1000) < 2) {
                (diff / (60 * 1000)).toString() + " minute ago"
            } else {
                (diff / (60 * 1000)).toString() + " minutes ago"
            }
        } else if (diff < 24 * (60 * (60 * 1000))) {
            if (diff / (60 * (60 * 1000)) < 2) {
                (diff / (60 * (60 * 1000))).toString() + " hour ago"
            } else {
                (diff / (60 * (60 * 1000))).toString() + " hours ago"
            }
        } else {
            if (diff / (24 * (60 * (60 * 1000))) < 2) {
                (diff / (24 * (60 * (60 * 1000)))).toString() + " day ago"
            } else {
                (diff / (24 * (60 * (60 * 1000)))).toString() + " days ago"
            }
        }
    }
}
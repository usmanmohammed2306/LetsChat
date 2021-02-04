package android.example.com.letschat.Fragments

import android.content.Intent
import android.example.com.letschat.Activities.ChatActivity
import android.example.com.letschat.Activities.ContactActivity
import android.example.com.letschat.Holders.ChatHolder
import android.example.com.letschat.Models.Chat
import android.example.com.letschat.Notifications.Token
import android.example.com.letschat.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId


class ChatFragment : Fragment() {

    private var adapter: FirebaseRecyclerAdapter<Chat, ChatHolder>? = null
    private lateinit var recyclerView: RecyclerView

    var currentUserId: String = ""

    fun ChatFragment() {

    }

    override fun onCreateView(
        @NonNull inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_chat, container, false)

        val chatFloat: ImageButton = view.findViewById(R.id.chat_float)

        chatFloat.setOnClickListener {
            startActivity(Intent(context, ContactActivity::class.java))
        }

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()
        val chatDatabase = FirebaseDatabase.getInstance().reference.child("Chat").child(
            currentUserId
        )
        chatDatabase.keepSynced(true)

        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.reverseLayout = true
        linearLayoutManager.stackFromEnd = true

        recyclerView = view.findViewById(R.id.chat_recycler)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = linearLayoutManager

        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            linearLayoutManager.orientation
        )
        recyclerView.addItemDecoration(dividerItemDecoration)

        val options = FirebaseRecyclerOptions.Builder<Chat>()
                .setQuery(chatDatabase.orderByChild("timestamp"), Chat::class.java)
                .build()
        adapter = object : FirebaseRecyclerAdapter<Chat, ChatHolder>(options) {

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatHolder  {
                val viewr: View = LayoutInflater.from(context).inflate(R.layout.user, parent, false)
                return ChatHolder(activity!!, viewr, context!!)
            }

            override fun onBindViewHolder(holder: ChatHolder, position: Int, model: Chat) {
                val userid: String = getRef(position).key.toString()

                holder.setHolder(userid, model.message, model.timestamp, model.seen)
                holder.getView().setOnClickListener {
                    val chatIntent = Intent(context, ChatActivity::class.java)
                    chatIntent.putExtra("userid", userid)
                    startActivity(chatIntent)
                }
            }

            override fun onDataChanged() {
                super.onDataChanged()

                val text: TextView = view.findViewById(R.id.chat_text)

                if(adapter?.itemCount == 0)  {
                    text.visibility = View.VISIBLE
                }
                else {
                    text.visibility = View.GONE
                }
            }
        }
        recyclerView.adapter = adapter
        updateToken(FirebaseInstanceId.getInstance().token.toString())
        return view
    }

    private fun updateToken(token: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("Tokens")
        val token1 = Token(token)
        reference.child(currentUserId).setValue(token1)
    }

    override fun onStart() {
        super.onStart()

        adapter?.startListening()
        adapter?.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()

        adapter?.stopListening()
    }
}


package android.example.com.letschat.Adapters

import android.example.com.letschat.Holders.MessageHolder
import android.example.com.letschat.Models.Message
import android.example.com.letschat.R
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth


class MessageAdapter(var messagesList: MutableList<Message>) : RecyclerView.Adapter<MessageHolder>() {

    private val TAG: String = "MessageAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.message, parent, false)
        return MessageHolder(view, view.context)
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val c: Message = messagesList[position]
        Log.i(TAG,"messagelist size: ${messagesList.size}")
        if (messagesList.size - 1 == position) {
            holder.setLastMessage(currentUserId, c.from, c.to)
        } else {
            holder.hideBottom()
        }
        if (c.from.equals(currentUserId)) {
            holder.setRightMessage(c.from, c.message, c.timestamp, c.type)
        } else {
            holder.setLeftMessage(c.from, c.message, c.timestamp, c.type)
        }
    }

    override fun getItemCount(): Int {
        return messagesList.size
    }

    init {
        this.messagesList = messagesList
    }
}
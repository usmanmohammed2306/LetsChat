package android.example.com.letschat.Models

class Message {
    lateinit var message: String
    lateinit var type: String
    lateinit var from: String
    lateinit var to: String
    var timestamp: Long = 0
}
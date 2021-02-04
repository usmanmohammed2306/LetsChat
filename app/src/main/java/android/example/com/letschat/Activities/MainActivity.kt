package android.example.com.letschat.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class MainActivity : AppCompatActivity() {

    val providers = arrayListOf(
            AuthUI.IdpConfig.PhoneBuilder().build()
    )
    private val RC_SIGN_IN = 1822
    private lateinit var mAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()
    }

    override fun onStart() {
        super.onStart()

        if(mAuth.currentUser == null) {
            val welcomeIntent: Intent = Intent(this, LoginActivity::class.java)
            startActivity(welcomeIntent)
            finish()
        }
        else {
            val startIntent:Intent = Intent(this, StartActivity::class.java)
            startActivity(startIntent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

        if(currentUser != null) {
            FirebaseDatabase.getInstance().getReference().child("Users").child(currentUser.uid).child("online").setValue(ServerValue.TIMESTAMP)
        }
    }
}
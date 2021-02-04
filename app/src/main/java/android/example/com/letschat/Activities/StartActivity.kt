package android.example.com.letschat.Activities

import android.content.Intent
import android.example.com.letschat.Fragments.ChatFragment
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.ogaclejapan.smarttablayout.SmartTabLayout
import android.example.com.letschat.R
import android.example.com.letschat.Fragments.ProfileFragment
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItemAdapter
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItems
import jahirfiquitiva.libs.fabsmenu.FABsMenu

class StartActivity : AppCompatActivity() {

    private val TAG: String = "StartActivity"

    private lateinit var adapter: FragmentPagerItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val toolbar: Toolbar = findViewById(R.id.main_app_bar)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "LetsChat"

        adapter = FragmentPagerItemAdapter(
            supportFragmentManager, FragmentPagerItems.with(this)
                .add("Profile", ProfileFragment::class.java)
                .add("Chat", ChatFragment::class.java)
                .create())

        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = adapter
        viewPager.currentItem = 1

        val viewPagerTab: SmartTabLayout = findViewById(R.id.viewpagertab)
        viewPagerTab.setViewPager(viewPager)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener {task->
            if(!task.isSuccessful)  {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val token = task.result

            System.out.println(TAG + ": " + token.toString())
        })

    }

    override fun onStart() {
        super.onStart()

        if(FirebaseAuth.getInstance().currentUser == null)  {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

        if(currentUser != null)  {
            FirebaseDatabase.getInstance().reference.child("Users").child(currentUser.uid).child("online").setValue("true")
        }
    }

    override fun onPause() {
        super.onPause()

        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

        if(currentUser != null)  {
            FirebaseDatabase.getInstance().reference.child("Users").child(currentUser.uid).child("online").setValue(ServerValue.TIMESTAMP)
        }
    }

    override fun onBackPressed() {
        val menu = findViewById<FABsMenu>(R.id.profile_fabs_menu)
        if(menu.isExpanded)  {
            menu.collapse()
        }
        else {
            super.onBackPressed()
        }
    }
}
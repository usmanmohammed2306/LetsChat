package android.example.com.letschat.Activities

import android.database.Cursor
import android.example.com.letschat.Models.User
import android.example.com.letschat.R
import android.example.com.letschat.Utils.CountryToPhonePrefix
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.database.*
import android.example.com.letschat.Adapters.UserListAdapter
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import kotlin.contracts.ContractBuilder

class ContactActivity : AppCompatActivity() {

    private lateinit var usersList: RecyclerView
    private lateinit var usersListAdapter: UserListAdapter
    private lateinit var usersListLayoutManager: RecyclerView.LayoutManager

    lateinit var userList: ArrayList<User>
    lateinit var contactList: MutableSet<User>
    lateinit var phoneList: ArrayList<String>

    var myNum : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)
        setSupportActionBar(findViewById(R.id.toolbar))

        userList = arrayListOf()
        contactList = mutableSetOf()
        phoneList = arrayListOf()

        myNum = FirebaseAuth.getInstance().currentUser?.phoneNumber.toString()

        if(supportActionBar != null)  {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }

        initializeRecyclerView()
        getContactList()
    }

    private fun getContactList()  {
        val ISOPrefix: String = getCountryISO()

        var name: String
        var phone: String

        val phones : Cursor? = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null)
        while(phones?.moveToNext()!!) {
            name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            phone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            phone = phone.replace(" ", "")
            phone = phone.replace("-", "")
            phone = phone.replace("(", "")
            phone = phone.replace(")", "")
            if(phone.get(0) == '0')  {
                phone = phone.substring(1)
            }

            if(!(phone.get(0)).toString().equals("+"))
                phone = ISOPrefix + phone;

            val contact = User("",name,phone)
            contactList.add(contact)
            getUserDetails(contact)
        }
    }

    private fun getUserDetails(contact: User)  {
        val userdb: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users")
        System.out.println(contact.phone)
        val query: Query = userdb.orderByChild("phone").equalTo(contact.phone)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists())  {
                    var phone = ""
                    var name = ""
                    for(childSnapshot : DataSnapshot in snapshot.children) {

                        if(childSnapshot.child("phone").value != null)  {
                            phone = childSnapshot.child("phone").value.toString()
                        }
                        if(childSnapshot.child("name").value != null)  {
                            name = childSnapshot.child("name").value.toString()
                        }


                        val mUser = User(childSnapshot.key.toString(),name,phone)
                        if(name.equals(phone)) {
                            for (mContactIterator: User in contactList) {
                                if (mContactIterator.phone.equals(mUser.phone)) {
                                    mUser.username = mContactIterator.username
                                }
                            }
                        }
                        System.out.println(contact.phone)
                        if(!phoneList.contains(phone) && !phone.equals(myNum)) {
                            userList.add(mUser)
                            phoneList.add(phone)
                            System.out.println(userList.size)
                            usersListAdapter.notifyDataSetChanged()
                            return
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
        //System.out.println(userList.size)
    }

    private fun getCountryISO(): String  {
        var iso = ""

        val telephonyManager =
            applicationContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        if(telephonyManager.getNetworkCountryIso() != null)  {
            if(!telephonyManager.getNetworkCountryIso().toString().equals(""))  {
                iso = telephonyManager.getNetworkCountryIso().toString()
            }
        }
        return CountryToPhonePrefix.getPhone(iso)!!
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == android.R.id.home)  {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initializeRecyclerView()  {
        usersList = findViewById(R.id.list_contacts)
        usersList.isNestedScrollingEnabled = false
        usersList.setHasFixedSize(false)
        usersListLayoutManager = LinearLayoutManager(applicationContext,RecyclerView.VERTICAL,false)
        usersList.layoutManager = usersListLayoutManager
        usersListAdapter = UserListAdapter(userList)
        usersList.adapter = usersListAdapter
    }

}
package android.example.com.letschat.Activities

import android.content.Intent
import android.example.com.letschat.R
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.rilixtech.widget.countrycodepicker.CountryCodePicker
import io.michaelrocks.libphonenumber.android.Phonenumber
import java.util.concurrent.TimeUnit


class LoginActivity : AppCompatActivity() {

    private val TAG = "CA/LoginActivity"

    private lateinit var ccp: CountryCodePicker
    private lateinit var mPhoneText: TextView
    private lateinit var mPhoneNumber: EditText
    private lateinit var mUserName: EditText
    private lateinit var errorMessage: TextView
    private lateinit var mCode:EditText
    private lateinit var mSend: Button
    private lateinit var auth: FirebaseAuth
    var valid : Boolean = false

    private lateinit var mCallbacks: OnVerificationStateChangedCallbacks

    var mVerificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        FirebaseApp.initializeApp(this)
        userIsLoggedIn()
        mPhoneText = findViewById(R.id.phone_text)
        ccp = findViewById(R.id.ccp)
        mPhoneNumber = findViewById(R.id.phone_number)
        mUserName = findViewById(R.id.user_name)
        mCode = findViewById(R.id.verify_code)
        errorMessage = findViewById(R.id.error_message)
        mSend = findViewById(R.id.phone_button)
        mSend.setOnClickListener {
            if(mUserName.text.toString().length > 0) {
                mUserName.visibility = View.INVISIBLE
                ccp.visibility = View.VISIBLE
                mPhoneNumber.visibility = View.VISIBLE
                mPhoneText.text = "Enter your phone number"
                if(mPhoneNumber.text.toString().length > 0) {
                    val phone: com.google.i18n.phonenumbers.Phonenumber.PhoneNumber? =
                        PhoneNumberUtil.getInstance().parse(
                            ccp.selectedCountryCodeWithPlus.toString() + mPhoneNumber.text.toString(),
                            com.google.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource.UNSPECIFIED.name
                        )
                    if (isValidPhoneNumber(mPhoneNumber.text.toString())) {
                        valid = PhoneNumberUtil.getInstance().isValidNumber(phone)
                    }
                    if (valid) {
                        ccp.visibility = View.INVISIBLE
                        mPhoneNumber.visibility = View.INVISIBLE
                        mCode.visibility = View.VISIBLE
                        mPhoneText.text = "Enter Verification Code"
                        mSend.text = "Verify Code"
                        if (mVerificationId != null)
                            verifyPhoneNumberWithCode()
                        else
                            startPhoneNumberVerification()
                    } else {
                        mPhoneNumber.error = "INVALID PHONE NUMBER"
                    }
                }
            }
            else {
                mUserName.error = "Enter valid user name"
            }
        }

        auth = Firebase.auth

        mCallbacks = object : OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (e is FirebaseAuthInvalidCredentialsException) {
                    mPhoneNumber.error = "Invalid phone number."
                } else if (e is FirebaseTooManyRequestsException) {
                    Snackbar.make(
                        findViewById(android.R.id.content), "Quota exceeded.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                errorMessage.text = "Verification failed!"
            }
            override fun onCodeSent(
                verificationId: String,
                forceResendingToken: ForceResendingToken
            ) {
                super.onCodeSent(verificationId, forceResendingToken)
                mVerificationId = verificationId
                errorMessage.text = "Code Sent!"
            }
        }
    }


    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return if (!TextUtils.isEmpty(phoneNumber)) {
            Patterns.PHONE.matcher(phoneNumber).matches()
        } else false
    }

    private fun verifyPhoneNumberWithCode() {
        val credential = PhoneAuthProvider.getCredential(mVerificationId!!, mCode.text.toString())
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(phoneAuthCredential: PhoneAuthCredential) {
        FirebaseAuth.getInstance()
                .signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = FirebaseAuth.getInstance().currentUser
                        val userid = FirebaseAuth.getInstance().currentUser?.uid.toString()
                        if (user != null) {
                            val mUserDB = FirebaseDatabase.getInstance().reference.child("Users").child(userid)
                            mUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    if (!dataSnapshot.exists()) {
                                        val userMap: MutableMap<String, Any> = HashMap()
                                        userMap["phone"] = user.phoneNumber.toString()
                                        userMap["name"] = mUserName.text.toString()
                                        mUserDB.updateChildren(userMap)
                                    }
                                    userIsLoggedIn()
                                }

                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                        }
                    }
                }
    }

    private fun userIsLoggedIn() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(Intent(applicationContext, StartActivity::class.java))
            finish()
            return
        }
    }

    private fun startPhoneNumberVerification() {
        val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(ccp.selectedCountryCodeWithPlus.toString() + mPhoneNumber.text.toString())
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}

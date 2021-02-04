package android.example.com.letschat.Fragments

import android.example.com.letschat.Notifications.MyResponse
import android.example.com.letschat.Notifications.Sender
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


interface APIService {
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAAEpl2KRM:APA91bHBxVGhkU2fp5IzqrVM0GAPfT3VySUs5gy1P14jtDyZNYOlYamsn8_gWEgLWs_KTVEAi3FPbm7iQKima706icoPYlRf5k7yV6tW9mVOSKbDUQ34OQOGEGTuOF_03dErXzCRmTh4"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: Sender?): Call<MyResponse?>?
}
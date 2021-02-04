package android.example.com.letschat.Notifications

class Data {
    private var user: String = ""
    private var icon : Int = 0
    private var body: String = ""
    private var title: String = ""
    private var sented: String = ""

    constructor(user: String, icon: Int, body: String, title: String, sented: String) {
        this.user = user
        this.icon = icon
        this.body = body
        this.title = title
        this.sented = sented
    }

    constructor() {}
}
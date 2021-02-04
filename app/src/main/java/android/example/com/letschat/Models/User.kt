package android.example.com.letschat.Models


class User {
    var id: String = ""
    var username: String = ""
    var phone: String = ""

    constructor(
        id: String,
        username: String,
        phone: String
    ) {
        this.id = id
        this.username = username
        this.phone = phone
    }

    constructor() {}
}
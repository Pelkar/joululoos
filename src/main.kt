import java.util.*
import javax.mail.*
import java.io.File
import java.io.InputStream
import java.lang.Exception
import com.google.gson.Gson
import java.io.FileInputStream
import java.time.LocalDateTime
import javax.mail.internet.MimeMessage
import javax.mail.internet.InternetAddress

fun main() {
    val peopleList : List<AssignedPerson> = assignRandom(readFromFile())
    writeResults(peopleList)
    prepareEmailContent(peopleList)
}

fun assignRandom(peopleList : List<Person>) : List<AssignedPerson> {
    val remaining : MutableList<Person> = peopleList.toMutableList().asReversed()
    val random = Random()
    var index : Int
    val newList = mutableListOf<AssignedPerson>()
    for (i in peopleList.indices) {
        var turn = 5
        while (true) {
            index = random.nextInt(remaining.size)
            val from = peopleList[i]
            val to = remaining[index]
            if (!from.elim.contains(to.name) && from.name != to.name) {
                newList.add(
                        AssignedPerson(
                                giverName = from.name,
                                giverEmail = from.email,
                                receiverName = to.name,
                                receiverPreferences = to.preferences,
                                receiverComment = to.comment
                        )
                )
                remaining.removeAt(index)
                break
            }
            if (turn == 0) {
                println("Not an even pair up")
                return assignRandom(peopleList)
            }
            turn -= 1
        }
    }
    return newList
}

fun readFromFile() : List<Person> {
    val peopleData = getJsonDataFromFile()
    val peopleList = Gson().fromJson(peopleData, PersonList::class.java)
    return peopleList.persons
}

fun writeResults(peopleList: List<AssignedPerson>) {
    val time = LocalDateTime.now()
    File("results$time.txt").bufferedWriter().use { out ->
        peopleList.forEach {
            out.write("${it.giverName} -> ${it.receiverName}\n")
        }
    }
}

fun prepareEmailContent(peopleList: List<AssignedPerson>) {
    var html : String
    for (person : AssignedPerson in peopleList) {
        html = readHtml()
        println(person.giverName + " -> " + person.receiverName)
        html = html.replace(oldValue = "-NAME-", newValue = person.giverName.split(" ".toRegex())[0], ignoreCase = false)
        html = html.replace(oldValue = "-RECIPIENT-", newValue = person.receiverName, ignoreCase = false)
        val prefs = person.receiverPreferences.split(",").joinToString(separator = "<br>")
        html = html.replace(oldValue = "-PREFERENCES-", newValue = prefs, ignoreCase = false)
        html = html.replace(oldValue = "-COMMENT-", newValue = person.receiverComment, ignoreCase = false)
        if (person.giverEmail == "karl.valliste@gmail.com") {
            sendEmail(html, person.giverEmail)
        }
    }
    println("Done")
}

fun sendEmail(html: String, recipient: String) {
    try {
        val conf = Properties()
        conf.load(FileInputStream("conf.properties"))
        val username = conf.getProperty("loos.email")
        val password = conf.getProperty("loos.pw")
        val prop = Properties()
        prop["mail.smtp.host"] = "smtp.gmail.com"
        prop["mail.smtp.port"] = "587"
        prop["mail.smtp.auth"] = "true"
        prop["mail.smtp.ssl.trust"] = "smtp.gmail.com"
        prop["mail.smtp.starttls.enable"] = "true"
        prop["mail.smtp.ssl.protocols"] = "TLSv1.2"
        prop["mail.smtp.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
        val session = Session.getInstance(prop,
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(username, password)
                    }
                })
        try {
            val message: Message = MimeMessage(session)
            message.setFrom(InternetAddress("olenpakapikk@gmail.com"))
            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(recipient)
            )
            message.subject = "Jõululoos"
            message.setContent(html, "text/html")
            Transport.send(message)
        } catch (e: MessagingException) {
            println("FAILED - to $recipient")
            e.printStackTrace()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun readHtml(): String {
    val inputStream: InputStream = File("bb.html").inputStream()
    return inputStream.bufferedReader().readText()
}

fun getJsonDataFromFile(): String? {
    val jsonString: String
    try {
        val inputStream = File("bb.json").inputStream()
        jsonString = inputStream.bufferedReader().readText()
    } catch (exception: Exception) {
        exception.printStackTrace()
        return null
    }
    return jsonString
}

data class PersonList(val persons: List<Person>)
data class Person(val email: String, val name: String, val elim: List<String>, var preferences: String = "", var comment: String = "")
data class AssignedPerson(val giverName: String, val receiverName: String, var giverEmail: String = "", var receiverPreferences: String = "", var receiverComment: String = "")

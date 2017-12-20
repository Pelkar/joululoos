import java.io.File
import java.io.InputStream
import java.util.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail
import java.time.LocalDateTime

val contactsMap : MutableMap<String, String> = mutableMapOf<String, String>()

fun main(args : Array<String>) {
    val peopleList : List<Person> = assignRandom(readFromFile())
    writeResults(peopleList)
    sendEmails(peopleList)
}

fun writeResults(peopleList: List<Person>) {
    val time = LocalDateTime.now()
    File("results$time.txt").bufferedWriter().use { out ->
        peopleList.forEach {
            out.write("${it.name} -> ${it.recipient}\n")
        }
    }
}

fun sendEmails(peopleList: List<Person>) {
    val senderEmail = "joululoos@online.ee"
    var toMail : String
    val password = "Eleri1992"
    var html : String

    for (person : Person in peopleList) {
        html = readHtml()
        toMail = contactsMap.get(person.name)!!
        println(contactsMap.get(person.name) + " -> " + person.recipient)
        val email = HtmlEmail()
        email.hostName = "mail.hot.ee"
        email.setSmtpPort(465)
        email.setAuthenticator(DefaultAuthenticator(senderEmail, password))
        email.isSSLOnConnect = true
        email.setFrom(senderEmail)
        email.addTo(toMail)
        email.subject = "Jõululoos"
        html = html.replace(oldValue = "-NAME-", newValue = person.name.split(" ".toRegex())[0], ignoreCase = false)
        html = html.replace(oldValue = "-RECIPIENT-", newValue = person.recipient!!, ignoreCase = false)
        email.setHtmlMsg(html)
        email.send()
    }
}

fun assignRandom(peopleList : List<Person>) : List<Person> {
    val remaining : MutableList<Person> = peopleList.toMutableList().asReversed()
    val random = Random()
    var index : Int
    for (i in 0 until peopleList.size) {
        var turn = 5
        while (true) {
            index = random.nextInt(remaining.size)
            if (!peopleList[i].elim.contains(remaining[index].name) && peopleList[i].name != remaining[index].name) {
                peopleList[i].recipient = remaining[index].name
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
    return peopleList
}

fun readFromFile() : List<Person> {
    val peopleList = mutableListOf<Person>()

    val inputstream : InputStream = File("people.txt").inputStream()
    inputstream.bufferedReader().useLines { lines -> lines.forEach {
        val arr = it.split(";")
        val eliminations : List<String>?
        if (arr.size > 2) {
            eliminations = arr.subList(2, arr.size)
        } else {
            eliminations = emptyList()
        }
        contactsMap.put(arr[0], arr[1])
        peopleList.add(Person(name = arr[0], elim = eliminations, recipient = null))
    } }
    return peopleList
}

fun readHtml() : String {
    val inputstream : InputStream = File("email.html").inputStream()
    val html = inputstream.bufferedReader().readText()
    return html
}

data class Person(val name: String, val elim : List<String>, var recipient : String?)
package org.kirgor.imapsync

import java.io.File
import java.lang.System.err
import java.util.*
import javax.mail.Flags.Flag.DELETED
import javax.mail.Folder.HOLDS_MESSAGES
import javax.mail.Folder.READ_WRITE
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.script.Invocable
import javax.script.ScriptEngineManager
import kotlin.math.min

fun main() {
    print("Reading config... ")
    val config = ScriptEngineManager().getEngineByExtension("kts").apply {
        eval(File("config.kt").readText())
    } as Invocable
    println("OK")

    val rules = { message: Message -> config.invokeFunction("rules", message) as String }
    val connectSourceStore = { store: Store -> config.invokeFunction("connectSourceStore", store) as Unit }
    val connectDestinationStore = { store: Store -> config.invokeFunction("connectDestinationStore", store) as Unit }

    val session = Session.getDefaultInstance(Properties())
    val sourceStore = session.getStore("imaps")
    val destinationStore = session.getStore("imaps")

    while (true) {
        try {
            moveMessages(rules, connectSourceStore, connectDestinationStore, sourceStore, destinationStore)
        } catch (e: Exception) {
            e.printStackTrace(err)
        } finally {
            Thread.sleep(30000)
        }
    }
}

fun moveMessages(
    rules: (Message) -> String,
    connectSourceStore: (Store) -> Unit,
    connectDestinationStore: (Store) -> Unit,
    sourceStore: Store,
    destinationStore: Store
) {
    if (!sourceStore.isConnected) {
        connectSourceStore(sourceStore)
        println("Connected to source store $sourceStore")
    }

    if (!destinationStore.isConnected) {
        connectDestinationStore(destinationStore)
        println("Connected to destination store $sourceStore")
    }

    val fromFolder = sourceStore.getFolder("INBOX").apply {
        open(READ_WRITE)
    }

    fromFolder.use {
        val count = fromFolder.messageCount
        if (count > 0) {
            println("Inbox has $count messages")
            val messages = fromFolder.getMessages(1, min(count, 100))

            println("Processing ${messages.size} messages")
            println("----------------------------------------------------------------")

            val groupedMessages = messages.groupBy {
                println("${it.receivedDate} - ${it.subject}")
                val to = rules(it)
                println("  will be moved to $to")
                println("----------------------------------------------------------------")
                to
            }

            for (entry in groupedMessages.entries) {
                print("Moving ${entry.value.size} messages to ${entry.key}... ")
                val toFolder = destinationStore.getFolder(entry.key).apply {
                    if (!exists()) {
                        create(HOLDS_MESSAGES)
                        print("creating folder... ")
                    }
                    open(READ_WRITE)
                }
                toFolder.use {
                    it.appendMessages(entry.value.toTypedArray())
                    println("OK")
                }
            }

            messages.forEach { it.setFlag(DELETED, true) }
            println("Moving completed, ${count - messages.size} messages should be left")
            println()
        }
    }
}

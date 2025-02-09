import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.util.*

class DeepSeekBot(
    private val telegramBotToken: String,
    private val deepSeekApiKey: String
) : TelegramLongPollingBot() {

    private val gson = Gson()
    private val httpClient = OkHttpClient()
    private val chatHistories = mutableMapOf<Long, MutableList<String>>()
    private val captchaAnswers = mutableMapOf<Long, String>()
    private val captchaQuestions = mapOf(
        "2+2" to "4",
        "3*3" to "9",
        "10-5" to "5"
    )

    override fun getBotUsername(): String {
        return "DeepSeek astr0Bot"
    }

    override fun getBotToken(): String {
        return telegramBotToken
    }

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val messageText = update.message.text
            val chatId = update.message.chatId
            val userId = update.message.from.id

            when {
                messageText == "/start" -> sendCaptcha(chatId)
                captchaAnswers[userId] != null -> checkCaptcha(chatId, userId, messageText)
                messageText == "Создать чат 1" -> createChat(chatId, 1)
                messageText == "Создать чат 2" -> createChat(chatId, 2)
                messageText == "Создать чат 3" -> createChat(chatId, 3)
                messageText == "Очистить историю" -> clearHistory(chatId)
                else -> handleMessage(chatId, userId, messageText)
            }
        }
    }

    private fun sendCaptcha(chatId: Long) {
        val question = captchaQuestions.keys.random()
        captchaAnswers[chatId] = captchaQuestions[question]!!
        sendMessage(chatId, "Решите капчу: $question")
    }

    private fun checkCaptcha(chatId: Long, userId: Long, answer: String) {
        if (answer == captchaAnswers[userId]) {
            captchaAnswers.remove(userId)
            sendMessage(chatId, "Капча решена! Теперь вы можете использовать бота.")
            showMenu(chatId)
        } else {
            sendMessage(chatId, "Неверный ответ. Попробуйте еще раз.")
        }
    }

    private fun showMenu(chatId: Long) {
        val keyboard = ReplyKeyboardMarkup().apply {
            keyboard = listOf(
                KeyboardRow().apply {
                    add("Создать чат 1")
                    add("Создать чат 2")
                    add("Создать чат 3")
                },
                KeyboardRow().apply {
                    add("Очистить историю")
                }
            )
            resizeKeyboard = true
        }
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = "Выберите действие:"
            this.replyMarkup = keyboard
        }
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }


    private fun createChat(chatId: Long, chatNumber: Int) {
        chatHistories[chatId] = mutableListOf()
        sendMessage(chatId, "Чат $chatNumber создан. Теперь вы можете общаться.")
    }

    private fun clearHistory(chatId: Long) {
        chatHistories[chatId]?.clear()
        sendMessage(chatId, "История чата очищена.")
    }

    private fun handleMessage(chatId: Long, userId: Long, messageText: String) {
        val history = chatHistories[chatId] ?: mutableListOf()
        history.add("User: $messageText")
        val response = sendToDeepSeek(messageText, history)
        history.add("Bot: $response")
        sendMessage(chatId, response)
    }

    private fun sendToDeepSeek(message: String, history: List<String>): String {
        val mediaType = "application/json".toMediaType()
        val requestBody = RequestBody.create(mediaType, gson.toJson(mapOf("prompt" to message, "history" to history)))

        val request = Request.Builder()
            .url("https://api.deepseek.com/v1/chat")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $deepSeekApiKey")
            .build()

        val response = httpClient.newCall(request).execute()
        return response.body?.string() ?: "Ошибка при получении ответа от DeepSeekAI"  // body как свойство
    }




    private fun sendMessage(chatId: Long, text: String) {
        val message = SendMessage().apply {
            this.chatId = chatId.toString()
            this.text = text
        }
        try {
            execute(message)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

}

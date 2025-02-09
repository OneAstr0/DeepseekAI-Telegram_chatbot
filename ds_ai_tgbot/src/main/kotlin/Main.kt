import com.sun.tools.javac.Main
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession
import java.util.Properties

fun loadConfig(): Pair<String, String> {
    val inputStream = Main::class.java.getResourceAsStream("/config.properties")
    if (inputStream != null) {
        val properties = Properties()
        properties.load(inputStream)

        val botToken = properties.getProperty("bot.token")
        val botUsername = properties.getProperty("bot.username")
        val deepSeekApiKey = properties.getProperty("deepseek.apiKey")

        println("Bot Token: $botToken")
        println("Bot Username: $botUsername")
        println("DeepSeek API Key: $deepSeekApiKey")

        return Pair(botToken, deepSeekApiKey)
    } else {
        println("Файл config.properties не найден в classpath")
        throw Exception("Config file not found")
    }
}

fun main() {
    val (botToken, deepSeekApiKey) = loadConfig()
    val bot = DeepSeekBot(botToken, deepSeekApiKey)

    // Запуск бота с использованием DefaultBotSession
    val botsApi = TelegramBotsApi(DefaultBotSession::class.java)  // Указываем тип сессии
    try {
        botsApi.registerBot(bot)
        println("Bot successfully registered.")
    } catch (e: TelegramApiException) {
        e.printStackTrace()
    }
}

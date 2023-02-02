package bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.*
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import data.remote.API_KEY
import data.remote.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private const val BOT_ANSWER_TIMEOUT = 30
private const val BOT_TOKEN = ""
class WeatherBot (private val weatherRepository: WeatherRepository) {
    private lateinit var country: String
    private var _chatId: ChatId? = null
    private val chatId by lazy { requireNotNull(_chatId) }

    fun createBot(): Bot{
        return bot {
            timeout = BOT_ANSWER_TIMEOUT
            token = BOT_TOKEN
            logLevel= LogLevel.Error

            dispatch {
                text {
                    setUpCommands()
                    setUpCallbacks()
                }
            }
        }
    }

    private fun Dispatcher.setUpCallbacks() {
       callbackQuery(callbackData = "getMyLocation") {
        bot.sendMessage(chatId = chatId, text = "Отправь мне свою локацию")
            location {
                CoroutineScope(Dispatchers.IO).launch {
                    weatherRepository.getReverseGeocodingCountryName(
                        location.latitude.toString(), location.longitude.toString(), "json"
                    ).address.country

                    country = weatherRepository.toString()

                    val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                        listOf(
                            InlineKeyboardButton.CallbackData(
                                text = "Да, верно",
                                callbackData = "yes_label"
                            )
                        )
                    )
                    bot.sendMessage(
                        chatId = chatId,
                        text = "Твой город - $country? Если нет - скинь ещё раз локацию",
                        replyMarkup = inlineKeyboardMarkup
                    )
                }
            }

       }
        callbackQuery (callbackData = "enterManually"){
            bot.sendMessage(
                chatId = chatId,
                text = "Введи свой город"
            )
            message(com.github.kotlintelegrambot.extensions.filters.Filter.Text) {
                country = message.text.toString()

                val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                    listOf(
                        InlineKeyboardButton.CallbackData(
                            text = "Да, верно",
                            callbackData = "yes_label"
                        )
                    )
                )
                bot.sendMessage(
                    chatId = chatId,
                    text = "Твой город - $country? Если нет - введи снова",
                    replyMarkup = inlineKeyboardMarkup
                )

            }
        }

        callbackQuery(callbackData = "yes_label"){
            bot.apply {
                sendMessage(chatId = chatId, text = "Подождите, пожалуйста")
                sendChatAction(chatId = chatId, action = ChatAction.TYPING)
            }
            CoroutineScope(Dispatchers.IO).launch{
                val currentWeather = weatherRepository.getCurrentWeather(
                API_KEY, country, airQualityData = "no"
                )
            bot.sendMessage(
                chatId = chatId,
                text = """
                    Облачность: ${currentWeather.clouds}
                    Температура: ${currentWeather.main.temp}
                    Ощущается как: ${currentWeather.main.feels_like}
                    Влажность: ${currentWeather.main.humidity}
                    Скорость ветра: ${currentWeather.wind.speed}
                    Давление: ${currentWeather.main.pressure}
                """.trimIndent()
            )
                bot.sendMessage(
                    chatId = chatId,
                    text = "если нужен еще один запрос - введи /weather"
                )
                country = ""
            }
        }
    }

    private fun Dispatcher.setUpCommands() {
        command("start"){
            _chatId = ChatId.fromId(message.chat.id)
            bot.sendMessage(
                chatId = chatId,
                text = "Привет"
            )
        }
        command("weather"){
            val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
                listOf(
                    InlineKeyboardButton.CallbackData(
                        text = "Определить мой город",
                        callbackData = "getMyLocation"
                    )
                ),
                listOf(
                    InlineKeyboardButton.CallbackData(
                        text = "Ввести город вручную",
                        callbackData = "enterManually"
                    )
                )
            )

            bot.sendMessage(
                chatId = chatId,
                text = "Введи свой город",
                replyMarkup = inlineKeyboardMarkup
            )
        }
    }
}

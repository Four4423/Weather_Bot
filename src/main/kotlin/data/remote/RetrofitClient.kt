package data.remote

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import data.remote.api.ReverseGeocodingApi
import data.remote.api.WeatherApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

private const val WEATHER_BASE_URL = "http://api.weatherapi.com/v1/"
private const val REVERSE_GEOCODER_BASE_URL = "https://nominatim.openstreetmap.org/"
const val API_KEY = "8f543192d252e0b0f431f694cc9c87f1"

enum class RetrofitType(val baseUrl: String){
    WEATHER(WEATHER_BASE_URL),
    REVERSE_GEOCODER(REVERSE_GEOCODER_BASE_URL)
}

class RetrofitClient{


    fun getRetrofit(retrofitType: RetrofitType): Retrofit{
        return Retrofit.Builder()
            .baseUrl(retrofitType.baseUrl)
            .addCallAdapterFactory(CoroutineCallAdapterFactory.invoke())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

fun getWeatherApi(retrofit: Retrofit): WeatherApi{
    return retrofit.create(WeatherApi::class.java)
}

    fun getReversedGeocodingApi(retrofit: Retrofit): ReverseGeocodingApi{
        return  retrofit.create(ReverseGeocodingApi::class.java)
    }
}
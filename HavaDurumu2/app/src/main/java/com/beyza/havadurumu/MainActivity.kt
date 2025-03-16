package com.beyza.havadurumu

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beyza.havadurumu.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val apiKey = "4f3d1b2655524060cc48e2badca81616"
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(WeatherApi::class.java)

        // Load last searched city data
        loadLastSearchedCityData()

        binding.btnGetWeather.setOnClickListener {
            val city = binding.etCityName.text.toString()
            if (city.isNotEmpty()) {
                fetchWeatherData(api, city)
            } else {
                Toast.makeText(this, "Lütfen şehir adı girin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchWeatherData(api: WeatherApi, city: String) {
        val call = api.getCurrentWeather(city, apiKey)
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weatherResponse = response.body()
                    weatherResponse?.let {
                        displayWeatherData(it, city)
                        saveLastSearchedCityData(it, city)
                    } ?: run {
                        Toast.makeText(this@MainActivity, "Veri alınamadı", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Hata: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Hata: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayWeatherData(weatherResponse: WeatherResponse, city: String) {
        val tempInK = weatherResponse.main.temp
        val tempInC = tempInK - 273.15
        val tempInF = tempInC * 9 / 5 + 32
        val displayTemp = if (binding.switchTempUnit.isChecked) {
            String.format("%.2f°F", tempInF)
        } else {
            String.format("%.2f°C", tempInC)
        }

        binding.tvTemperature.text = displayTemp
        binding.tvPressure.text = "Basınç: ${weatherResponse.main.pressure} hPa"
        binding.tvHumidity.text = "Nem: ${weatherResponse.main.humidity}%"
        binding.tvWeatherDescription.text = weatherResponse.weather[0].description
        binding.tvCityName.text = city

        val weatherId = weatherResponse.weather[0].id
        val weatherIcon = getWeatherIcon(weatherId)
        binding.ivWeatherIcon.setImageResource(weatherIcon)
    }

    private fun saveLastSearchedCityData(weatherResponse: WeatherResponse, city: String) {
        val editor = sharedPreferences.edit()
        editor.putString("city", city)
        editor.putFloat("temp", weatherResponse.main.temp.toFloat())
        editor.putInt("pressure", weatherResponse.main.pressure)
        editor.putInt("humidity", weatherResponse.main.humidity)
        editor.putInt("weatherId", weatherResponse.weather[0].id)
        editor.putString("description", weatherResponse.weather[0].description)
        editor.apply()
    }

    private fun loadLastSearchedCityData() {
        val city = sharedPreferences.getString("city", null)
        if (city != null) {
            val temp = sharedPreferences.getFloat("temp", 0f).toDouble()
            val pressure = sharedPreferences.getInt("pressure", 0)
            val humidity = sharedPreferences.getInt("humidity", 0)
            val weatherId = sharedPreferences.getInt("weatherId", 0)
            val description = sharedPreferences.getString("description", "")

            val weatherResponse = WeatherResponse(
                Main(temp, pressure, humidity),
                listOf(Weather(weatherId, description!!))
            )
            displayWeatherData(weatherResponse, city)
        }
    }

    private fun getWeatherIcon(weatherId: Int): Int {
        return when (weatherId) {
            800 -> R.drawable.gunesli
            in 801..803 -> R.drawable.bulutlu
            804 -> R.drawable.bulutlu
            in 500..531 -> R.drawable.yagmurlu
            in 300..321 -> R.drawable.sagnak
            in 600..622 -> R.drawable.karli
            in 701..781 -> R.drawable.sisli
            in 900..902, in 905..906 -> R.drawable.ruzgarli
            else -> R.drawable.havalogo
        }
    }
}

interface WeatherApi {
    @GET("data/2.5/weather")
    fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String
    ): Call<WeatherResponse>
}

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>
)

data class Main(
    val temp: Double,
    val pressure: Int,
    val humidity: Int
)

data class Weather(
    val id: Int,
    val description: String
)

package com.example.weatherapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    public static final String API_KEY = "YOUR_API_KEY";
    EditText etCity;
    Button btnGetWeather, btnAddFavorite, btnFavorite;
    Database mDatabase;
    TextView tvCity, tvCoords, tvTime, fragment1, tvTemp;
    DecimalFormat df = new DecimalFormat("#.#");
    DecimalFormat df2 = new DecimalFormat("#");
    ImageView weatherIconImageView;
    private RequestQueue requestQueue;
    GridLayout gridForecast;
    double lat, lon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        weatherIconImageView = findViewById(R.id.weather_icon_image_view);
        etCity = findViewById(R.id.etCity);
        btnGetWeather = findViewById(R.id.btnGetWeather);
        tvCity = findViewById(R.id.tvCity);
        tvCoords = findViewById(R.id.tvCoords);
        tvTime = findViewById(R.id.tvTime);
        fragment1 = findViewById(R.id.fragment1);
        tvTemp = findViewById(R.id.tvTemp);
        gridForecast = findViewById(R.id.gridForecast);
        btnAddFavorite = findViewById(R.id.btnAddFavorite);
        btnFavorite = findViewById(R.id.btnFavorite);
        requestQueue = Volley.newRequestQueue(this);
        mDatabase = new Database(this);
        btnAddFavorite.setOnClickListener(view -> addData());

        btnFavorite.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, FavoritesActivity.class);
            startActivity(intent);
        });


        if (savedInstanceState != null) {
            tvCity.setText(savedInstanceState.getString("tvCity"));
            tvCoords.setText(savedInstanceState.getString("tvCoords"));
            tvTime.setText(savedInstanceState.getString("tvTime"));
            fragment1.setText(savedInstanceState.getString("fragment1"));
            tvTemp.setText(savedInstanceState.getString("tvTemp"));
            lat = savedInstanceState.getDouble("lat");
            lon = savedInstanceState.getDouble("lon");
            Bitmap bitmap = savedInstanceState.getParcelable("ivWeather");
            weatherIconImageView.setImageBitmap(bitmap);
            if (lat != 0 || lon != 0) {
                String hourlyUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY;
                getPredictedWeather(hourlyUrl);
            }
        }
    }

    public void getWeatherDetails(View view) {
        String city = etCity.getText().toString().trim();
        if (city.equals("")) {
            makeToast("City field can not be empty!");
        } else {
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
                String output = "";
                try {
                    JSONObject jsonResponse = new JSONObject(response);

                    JSONArray jsonArray = jsonResponse.getJSONArray("weather");

                    // Weather Object
                    JSONObject jsonObjectWeather = jsonArray.getJSONObject(0);
                    String iconCode = jsonObjectWeather.getString("icon");
                    String iconUrl = "https://openweathermap.org/img/w/" + iconCode + ".png";
                    Glide.with(this)
                            .load(iconUrl)
                            .into(weatherIconImageView);
                    String description = jsonObjectWeather.getString("description");

                    // Main Object
                    JSONObject jsonObjectMain = jsonResponse.getJSONObject("main");
                    double temp = jsonObjectMain.getDouble("temp") - 273.15;
                    float pressure = jsonObjectMain.getInt("pressure");
                    double feelsLike = jsonObjectMain.getDouble("feels_like") - 273.15;
                    int humidity = jsonObjectMain.getInt("humidity");

                    // Response Object
                    JSONObject coords = jsonResponse.getJSONObject("coord");
                    lat = coords.getDouble("lat");
                    lon = coords.getDouble("lon");

                    long time = jsonResponse.getLong("dt");
                    java.util.Date time1 = new java.util.Date(time * 1000);

                    int visibility = jsonResponse.getInt("visibility");

                    JSONObject jsonObjectWind = jsonResponse.getJSONObject("wind");
                    String windSpeed = jsonObjectWind.getString("speed");
                    double windDir = jsonObjectWind.getDouble("deg");

                    JSONObject jsonObjectClouds = jsonResponse.getJSONObject("clouds");
                    String clouds = jsonObjectClouds.getString("all");

                    JSONObject jsonObjectSys = jsonResponse.getJSONObject("sys");
                    String countryName = jsonObjectSys.getString("country");
                    String cityName = jsonResponse.getString("name");

                    tvCity.setText(cityName + " " + countryName);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d HH:mm");
                    String strDate = dateFormat.format(time1);
                    tvTime.setText(strDate);


                    tvCoords.setText("(" + lat + ", " + lon + ")");

                    tvTemp.setText(df.format(temp) + " °C");

                    output += " Description: " + description
                            + "\n Feels Like: " + df.format(feelsLike) + " °C"
                            + "\n Humidity: " + humidity + "%"
                            + "\n Wind: " + windSpeed + " m/s " + degreesToDirection(windDir)
                            + "\n Visibility: " + visibility / 1000 + " km"
                            + "\n Cloudiness: " + clouds + "%"
                            + "\n Pressure: " + pressure + " hPa";

                    fragment1.setText(output);


                    String hourlyUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY;
                    getPredictedWeather(hourlyUrl);

                } catch (JSONException e) {
                    makeToast("Error while parsing JSON response");
                }
            }, error -> makeToast("Error occurred while making API request"));

            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(stringRequest);
        }
    }

    private void getPredictedWeather(String predictedUrl) {
        // 5 days weather forecast
        StringRequest predictedRequest = new StringRequest(Request.Method.GET, predictedUrl, predictedResponse -> {
            try {
                JSONObject predictedJsonResponse = new JSONObject(predictedResponse);
                JSONArray predictedData = predictedJsonResponse.getJSONArray("list");

                SimpleDateFormat dateFormat1 = new SimpleDateFormat("HH:mm:ss");
                int gridPlace = 0;

                for (int i = 0; i < predictedData.length(); i++) {
                    JSONObject forecastItem = predictedData.getJSONObject(i);
                    long dt = forecastItem.getLong("dt");
                    Date date = new Date(dt * 1000);
                    String hourDate = dateFormat1.format(date);
                    if (hourDate.equals("14:00:00")) {

                        // Set date
                        SimpleDateFormat dateFormat2 = new SimpleDateFormat("d MMM");
                        String dayDate = dateFormat2.format(date);

                        TextView textView = (TextView) gridForecast.getChildAt(gridPlace);
                        textView.setText(dayDate);

                        //Set icon
                        JSONArray jsonArray = forecastItem.getJSONArray("weather");
                        JSONObject jsonObjectWeather = jsonArray.getJSONObject(0);
                        String iconCode = jsonObjectWeather.getString("icon");
                        String iconUrl = "https://openweathermap.org/img/w/" + iconCode + ".png";

                        ImageView imageView = (ImageView) gridForecast.getChildAt(gridPlace + 1);
                        Glide.with(this)
                                .load(iconUrl)
                                .override(150, 150)
                                .into(imageView);

                        //Set temperature
                        JSONObject jsonObjectMain = forecastItem.getJSONObject("main");
                        double temp = jsonObjectMain.getDouble("temp") - 273.15;
                        textView = (TextView) gridForecast.getChildAt(gridPlace + 2);
                        textView.setText(df2.format(temp) + " °C");

                        gridPlace += 3;
                    }
                }

            } catch (JSONException e) {
                makeToast("Error while parsing JSON response");
            }
        }, error -> makeToast("Error occurred while making API request"));

        RequestQueue predictedRequestQueue = Volley.newRequestQueue(getApplicationContext());
        predictedRequestQueue.add(predictedRequest);
        predictedRequestQueue.cancelAll(this);
    }

    public static String degreesToDirection(double degrees) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round(degrees / 45.0) % 8;
        return directions[index];
    }

    public void addData() {
        String city = etCity.getText().toString().trim();
        if (city.equals("")) {
            makeToast("City field can not be empty!");
        } else {
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY;
            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    response -> {
                        int woeid = Integer.parseInt(response.split("\"id\":")[1].split(",")[0]);
                        boolean add = mDatabase.addData(city, woeid);
                        if (add) {
                            makeToast("Location " + city + " saved");
                        } else {
                            makeToast("Something went wrong");
                        }
                    }, error -> makeToast("Incorrect city name!"));
            queue.add(stringRequest);

        }
    }

    public void makeToast(String text) {
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Bitmap bitmap = ((BitmapDrawable) weatherIconImageView.getDrawable()).getBitmap();
        outState.putParcelable("ivWeather",bitmap);
        outState.putString("tvCity", tvCity.getText().toString());
        outState.putString("tvCoords", tvCoords.getText().toString());
        outState.putString("tvTime", tvTime.getText().toString());
        outState.putString("fragment1", fragment1.getText().toString());
        outState.putString("tvTemp", tvTemp.getText().toString());
        outState.putDouble("lat", lat);
        outState.putDouble("lon", lon);

        super.onSaveInstanceState(outState);
    }
}


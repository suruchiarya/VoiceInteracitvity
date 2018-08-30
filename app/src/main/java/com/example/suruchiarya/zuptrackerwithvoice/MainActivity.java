package com.example.suruchiarya.zuptrackerwithvoice;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Response;
import com.google.android.gms.common.util.IOUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.*;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener,OnClickListener {
    private final int REQ_CODE_SPEECH_INPUT = 100;
    ImageButton btnSpeak;
    TextView txtSpeechInput;
    private TextToSpeech tts;
    final Context context = this;
    private static final int REQUEST_LOCATION = 1;
    String distanceFromLocaction,duration1,route;
    Button button;
    TextView textView;
    TextView tv;
    TextView tv1;
    TextView tv2;
    TextView tv3;
    TextView tv4;
    LocationManager locationManager;
    String lattitude, longitude;
    double x, y, lat, lon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);
        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        tts = new TextToSpeech(this, this);

        tv = (TextView) findViewById(R.id.locality);
        tv1 = (TextView) findViewById(R.id.city);
        tv2 = (TextView) findViewById(R.id.country);
        tv3 = (TextView) findViewById(R.id.postal);
        tv4 = (TextView) findViewById(R.id.zip);

        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);

        textView = (TextView) findViewById(R.id.text_location);
        /**
        button = (Button) findViewById(R.id.button_location);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
               if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps();

                } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    try {
                        getLocation(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
               }
           }
        });
*/
        btnSpeak.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });

        MyLocation(false);

    }


    /**
     * Showing google speech input dialog
     */
    private void promptSpeechInput() {
        Log.d("promptSpeechInput", "start");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Say Something");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "sorry! Your device doesn\\'t support speech input",
                    Toast.LENGTH_SHORT).show();
        }
        Log.d("promptSpeechInput", "end");
    }

    /**
     * Receiving speech input
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    String string;
                    string = result.get(0);
                    txtSpeechInput.setText(string);
                    String match="sync";
                    String text=txtSpeechInput.getText().toString();
                    Boolean found = Arrays.asList(text.split(" ")).contains(match);
                    if(found){
                        Sync_Method();
                    }


                    String health_keyword="health";
                    Boolean found1 = Arrays.asList(text.split(" ")).contains(health_keyword);
                    if(found1){
                        Dialog_Display();
                    }

                   String location_keyword="location";
                    Boolean found2 = Arrays.asList(text.split(" ")).contains(location_keyword);
                    if(found2) {

                        MyLocation(true);
                        LocationSpeech();
                    }






                    List<String> distancecommands = new ArrayList<>();
                    distancecommands.add("distance to");

                    for (String dis : distancecommands) {
                        if (string.contains(dis)) {
                            DistanceAsk();
                        }
                    }
                }
            }
        }


    }


    /**
     * Method for location
     */
    private void MyLocation(boolean showlocation) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();

        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                getLocation(showlocation);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Method for Location
     */

    protected float getLocation(boolean showLocation) throws IOException {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);

        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            Location location1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            Location location2 = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

            if (location != null) {
                x = location.getLatitude();
                y = location.getLongitude();
                lattitude = valueOf(x);
                longitude = valueOf(y);

                if(showLocation)
                textView.setText("Your current location is" + "\n" + "Lattitude = " + lattitude
                        + "\n" + "Longitude = " + longitude);

            } else if (location1 != null) {
                double latti = location1.getLatitude();
                double longi = location1.getLongitude();
                lattitude = valueOf(latti);
                longitude = valueOf(longi);

                if(showLocation)
                textView.setText("Your current location is" + "\n" + "Lattitude = " + lattitude
                        + "\n" + "Longitude = " + longitude);


            } else if (location2 != null) {
                double latti = location2.getLatitude();
                double longi = location2.getLongitude();
                lattitude = valueOf(latti);
                longitude = valueOf(longi);

                textView.setText("Your current location is" + "\n" + "Lattitude = " + lattitude
                        + "\n" + "Longitude = " + longitude);

            } else {

                Toast.makeText(this, "Unable to Trace your location", Toast.LENGTH_SHORT).show();

            }


        }
        Geocoder geocoder;
        List<Address> addresses;


        geocoder = new Geocoder(MainActivity.this, Locale.ENGLISH);
        addresses = geocoder.getFromLocation(x, y, 1);
        StringBuilder str = new StringBuilder();
        if (geocoder.isPresent()) {

            Address returnAddress = addresses.get(0);
            String state = returnAddress.getAdminArea();
            String locality = returnAddress.getFeatureName();
            String localityString = returnAddress.getLocality();
            String city = returnAddress.getCountryName();
            String region_code = returnAddress.getCountryCode();
            String zipcode = returnAddress.getPostalCode();
            //String sub_address=returnAddress.getSubAdminArea();
            String sub_local=returnAddress.getSubLocality();
           String sub_area=returnAddress.getPremises();
                //Bundle b=returnAddress.getExtras();
            if(showLocation) {

                tv.setText(locality + "      " +sub_local+"   " +"   "+localityString );
                tv1.setText(state);
                tv2.setText(city);
                tv3.setText(region_code);
                tv4.setText(zipcode);
            }

        }

        return 0;

    }

    /**
     * Method for location
     */
    protected void buildAlertMessageNoGps() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please Turn ON your GPS Connection")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    /**
     * Method for Location text to speech
     */
    private void LocationSpeech() {
        String t2 = "Your current location is";
        String t3 = tv.getText().toString();
        String t = tv1.getText().toString();
        String t1 = tv2.getText().toString();
        tts.speak(t2 + t3+" " + t + t1, TextToSpeech.QUEUE_FLUSH, null);
    }


    /**
     * Method to display dialog box
     */
    public void Dialog_Display() {
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.dialog);
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
isNetworkConnected();
        // set the custom dialog components - text, image and button
        TextView text1 = (TextView) dialog.findViewById(R.id.text1);
        TextView text2 = (TextView) dialog.findViewById(R.id.text2);
        TextView text3 = (TextView) dialog.findViewById(R.id.text3);
        TextView text4 = (TextView) dialog.findViewById(R.id.text4);
        String a;
        if(isNetworkConnected()==true)
        {
           a="Internet Connected";
        }
        else {
            a="Internet DisConnected";
        }

        String b = "Device Connected";
        String c = "All Files Synced";


        text1.setText(a);
        text2.setText(b);
        text3.setText(c);
        text4.setText("Battery Status" + "  " + batLevel);


        ImageView image = (ImageView) dialog.findViewById(R.id.image);
        image.setImageResource(R.drawable.ic_launcher);


        dialog.show();
        int i = 0;


        if (a.contains("Internet Connected") && b.contains("Device Connected") && c.contains("All Files Synced") && (batLevel > 25))
        {
            i = Constants.ALL_WORKING;
            textToSpeech(i);

        }
        else if (a.contains("Internet DisConnected") && b.contains("Device Connected") && c.contains("All Files Synced") && (batLevel > 25)) {
            i = Constants.INTERNET;
            textToSpeech(i);
        } else if (a.contains("Internet Connected") && b.contains("Device DisConnected") && c.contains("All Files Synced") && (batLevel > 25)) {
            i = Constants.DEVICE;
            textToSpeech(i);

        } else if (a.contains("Internet Connected") && b.contains("Device Connected") && c.contains("Files  pending") && (batLevel > 25)) {
            i = Constants.FILES;
            textToSpeech(i);
        } else if (a.contains("Internet Connected") && b.contains("Device Connected") && c.contains("All Files Synced") && (batLevel <= 25)) {
            i = Constants.BATTERY;
            textToSpeech(i);
        } else if (a.contains("Internet DisConnected") && b.contains("Device DisConnected") && c.contains("All Files Synced") && (batLevel > 25)) {
            i = Constants.INTERNET_AND_DEVICE;
            textToSpeech(i);
        } else if (a.contains("Internet DisConnected") && b.contains("Device Connected") && c.contains("All Files Synced") && (batLevel <= 25)) {
            i = Constants.INTERNET_AND_BATTERY;
            textToSpeech(i);
        } else if (a.contains("Internet DisConnected") && b.contains("Device DisConnected") && c.contains("Files Pending") && (batLevel > 25)) {
            i = Constants.INTERNET_DEVICE_FILES;
            textToSpeech(i);

        } else if (a.contains("Internet DisConnected") && b.contains("Device Connected") && c.contains("Files Pending") && (batLevel > 25)) {
            i = Constants.INTERNET_AND_FILES;
            textToSpeech(i);
        } else if (a.contains("Internet Connected") && b.contains("Device DisConnected") && c.contains("Files Pending") && (batLevel > 25)) {
            i = Constants.DEVICE_AND_FILES;
            textToSpeech(i);
        } else if (a.contains("Internet Connected") && b.contains("Device DisConnected") && c.contains("All Files Synced") && (batLevel <= 25)) {
            i = Constants.DEVICE_AND_BATTERY;
            textToSpeech(i);
        } else if (a.contains("Internet Connected") && b.contains("Device Connected") && c.contains("Files Pending") && (batLevel <= 25)) {
            i = Constants.FILES_AND_BATTERY;
            textToSpeech(i);
        } else if (a.contains("Internet DisConnected") && b.contains("Device DisConnected") && c.contains("All Files Synced") && (batLevel <= 25)) {
            i = Constants.INTERNET_DEVICE_BATTERY;
            textToSpeech(i);
        } else if (a.contains("Internet Connected") && b.contains("Device DisConnected") && c.contains("Files Pending") && (batLevel <= 25)) {
            i = Constants.DEVICE_FILES_BATTERY;
            textToSpeech(i);
        }

    }

    /**
     * Text to speech for dialog box
     */
    private void textToSpeech(int i) {

        switch (i) {
            case Constants.ALL_WORKING: {

                List<String> internet1 = new ArrayList<String>();
                internet1.add("Everything is working fine ");
                internet1.add("We are good ");
                internet1.add("It seems all the components are working fine   ");

                for (String inte : internet1) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (internet1.get(random.nextInt(internet1.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                }
                break;
            }
            case Constants.INTERNET: {
                List<String> internet1 = new ArrayList<String>();
                internet1.add("everything is okay No internet Connection ");
                internet1.add("all components are working fine but internet connectivity lost ");
                internet1.add("everything is fine but There is no internet connection ");

                for (String inte : internet1) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (internet1.get(random.nextInt(internet1.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                }
                break;
            }
            case Constants.DEVICE: {
                List<String> device = new ArrayList<String>();
                device.add("everything is working fine but Device is not connected ");
                device.add("everything is okay except for Device Connectivity");
                device.add("all other components are okay but No device Connectivity  ");

                for (String inte : device) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (device.get(random.nextInt(device.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                }
                break;
            }
            case Constants.FILES: {
                List<String> device = new ArrayList<String>();
                device.add("everything is okay but Files are Pending ");
                device.add("all other components are okay but  files are not synced");

                for (String inte : device) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (device.get(random.nextInt(device.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                }
                break;
            }
            case Constants.BATTERY: {
                List<String> device = new ArrayList<String>();
                device.add("everything else is working fine but u need to charge your battery");
                device.add("all other components are working fine but Battery is low");

                for (String inte : device) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (device.get(random.nextInt(device.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                }
                break;
            }
            case Constants.INTERNET_AND_DEVICE: {

                List<String> device = new ArrayList<String>();
                device.add("Internet disconnected and device disconnected");
                device.add("no internet connection and device is not connected");

                for (String inte : device) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (device.get(random.nextInt(device.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                }
                break;

            }
            case Constants.INTERNET_DEVICE_FILES: {
                List<String> device = new ArrayList<String>();
                device.add("Internet disconnected  device disconnected and Files are pending");


                for (String inte : device) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (device.get(random.nextInt(device.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                }
                break;

            }
            case Constants.INTERNET_AND_FILES: {
                List<String> device = new ArrayList<String>();
                device.add("Internet disconnected  and Files are pending");
                device.add("No internet connection and files are noy synced");

                for (String inte : device) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (device.get(random.nextInt(device.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                }
                break;
            }
            case Constants.INTERNET_AND_BATTERY: {
                List<String> device = new ArrayList<String>();
                device.add("Internet disconnected  and Battery is low");
                device.add("No internet connection and low battery");

                for (String inte : device) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (device.get(random.nextInt(device.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                }
                break;

            }
            case Constants.DEVICE_AND_FILES: {
                List<String> device = new ArrayList<String>();
                device.add("Device disconnected  and Files are pending");
                device.add("device is not connected and files not synced");

                for (String inte : device) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (device.get(random.nextInt(device.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                    break;
                }

            }
            case Constants.DEVICE_AND_BATTERY: {
                List<String> device = new ArrayList<String>();
                device.add("Device disconnected  and Battery is low");
                device.add("Device is not connected and low battery");

                for (String inte : device) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (device.get(random.nextInt(device.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                    break;
                }

            }
            case Constants.FILES_AND_BATTERY: {
                List<String> device = new ArrayList<String>();
                device.add("Files are pending and Battery is low");
                device.add("Files are noy synced and battery is low");

                for (String inte : device) {
                    Random random = new Random();

                    for (int x = 0; x < 1; x++) {
                        String t = (device.get(random.nextInt(device.size())));

                        tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                    }
                    break;
                }

            }
            case Constants.INTERNET_DEVICE_BATTERY: {

                String t = "No internet connection Device is not connected and battery is low";

                tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);


                break;


            }
            case Constants.DEVICE_FILES_BATTERY: {
                String t = "Device is not connected Files are pending and battery is low";
                tts.speak(t, TextToSpeech.QUEUE_FLUSH, null);
            }
        }

    }

   /**
    to check internet connection
    */
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }


    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();

    }

    /**
     * Method to sync Files
     */
    public void Sync_Method() {


        Toast.makeText(getApplicationContext(), "Hey there! Your files synced ", Toast.LENGTH_SHORT).show();


    }


    /**
     * Method of OnInIt listener
     */
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                btnSpeak.setEnabled(true);


                //  Log.d("ONINIT", "speak");

            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }


    }




    private void DistanceAsk() {
        String address = txtSpeechInput.getText().toString();

        GeocodingLocation locationAddress = new GeocodingLocation();
        locationAddress.getAddressFromLocation(address,
                getApplicationContext(), new GeocoderHandler());
    }

    @Override
    public void onClick(View view) {

    }

    class GeocoderHandler extends Handler {

        @Override
        public void handleMessage(Message message) {
            String locationAddress = "";


            Bundle bundle1 = message.getData();
            locationAddress = bundle1.getString("address");

            if (locationAddress.contains("Unable to get Latitude and Longitude")) {
                textView.setText("Address not found");
                return;
            }

            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    lat = bundle.getDouble("latitude");
                    lon = bundle.getDouble("longitude");

                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = null;
            }
           // textView.setText(locationAddress);

            Log.e("x",x+"");
            Log.e("y",y+"");

            getDistance(x,y,lat,lon);
        }
    }

    public String getDistance(final double x, final double y, final double lat, final double lon){
        final String[] parsedDistance = new String[1];
        final String[] response = new String[1];
        Thread thread=new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    URL url = new URL("http://maps.googleapis.com/maps/api/directions/json?origin=" + x + "," + y + "&destination=" + lat + "," + lon + "&sensor=false&units=metric&mode=driving");                    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    Log.e("url","http://maps.googleapis.com/maps/api/directions/json?origin=" + x + "," + y + "&destination=" + lat + "," + lon + "&sensor=false&units=metric&mode=driving");
                    conn.setRequestMethod("POST");
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    response[0] = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

                    JSONObject jsonObject = new JSONObject(response[0]);
                    JSONArray array = jsonObject.getJSONArray("routes");
                    JSONObject routes = array.getJSONObject(0);
                    JSONArray legs = routes.getJSONArray("legs");
                    JSONObject steps = legs.getJSONObject(0);
                    JSONObject distance = steps.getJSONObject("distance");
                    JSONObject duration=steps.optJSONObject("duration");

                   distanceFromLocaction =distance.getString("text");
                    duration1=duration.getString("text");

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                           textView.setText(distanceFromLocaction);
                            tv.setText(duration1);
                          // tv2.setText(route);
                            String t=textView.getText().toString();
                            String s=txtSpeechInput.getText().toString();
String a="and duration is";
String b=tv.getText().toString();
                            tts.speak(s + "is" + t + "  " + a + b,TextToSpeech.QUEUE_FLUSH,null);
                        }
                    });
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();


        return distanceFromLocaction;

    }

    }





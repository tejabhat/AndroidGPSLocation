package com.vandadike.mylocation;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


import java.io.BufferedReader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

import static android.Manifest.permission.SEND_SMS;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    private GoogleApiClient mGoogleApiClient = null;


    LocationManager locationManager;

    private static final String TAG = "MainActivity";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    String network_provider = LocationManager.NETWORK_PROVIDER;


    String gps_provider = LocationManager.GPS_PROVIDER;

    // The minimum distance to change Updates in meters
    private static final long MIN_UPDATE_DISTANCE = 10;

    // The minimum time between updates in milliseconds
    private static final long MIN_UPDATE_TIME = 1000 * 60;

    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList();
    private ArrayList<String> permissions = new ArrayList();

    private final static int ALL_PERMISSIONS_RESULT = 101;

    private Button btnLocation;
    private TextView txtGoogleLocation;
    private TextView txtGpsLocation;
    private TextView txtNPLocation;//network provider

    private Button btnShowNum;
    private Button btnUpdateNum;
    private EditText txtPhoneNum;

    private String filename = "vandadike_mylocation";
    private String phoneNumbers = "";


    ////////////////////// LIFE CYCLE METHODS //////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setView();

        doPermissionsPart();

        setLocationServers();

    }

    @Override
    protected void onStart()
    {
        super.onStart();

        if (mGoogleApiClient != null)
        {
            mGoogleApiClient.connect();
        }

        Log.d(TAG, "onStart: ");
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        stopLocationUpdates();
    }


    ///////////////////////// MAIN METHODS FOR LOCATION SERVICE ////////////////////////////////////


    private void setLocationServers()
    {
        //USE GOOGLE BASED APIs
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //USE ANDROID APIs
        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

    }



    ////////USE GOOGLE API TO GET LAST LOCATION ///////////////////////////
    private void getGoogleLocatoin()
    {
        String url = "";
        Log.d(TAG, "get_google_location: ");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding

            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(TAG, "get_google_location:  no permission");
            txtGoogleLocation.setText("No Permissions!");
            return;
        }
        Task<Location> task = LocationServices.getFusedLocationProviderClient(this).getLastLocation();

        task.addOnCompleteListener(new OnCompleteListener<Location>()
        {
            @Override
            public void onComplete(@NonNull Task<Location> task)
            {
                try
                {
                    Location location = task.getResult();
                    showLocation(location, "Google Location");
                } catch (Exception e)
                {
                    Log.e(TAG, "onComplete: ", e);
                    txtGoogleLocation.setText("Error in getting google location: " + e.getMessage());
                }

            }
        });


    }

    //USER Android APIs to get the GPS and NetworkProvider location.
    private void getAndroidLocation()
    {
        Log.d(TAG, "getAndroidLocation: ");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding

            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(TAG, "getAndroidLocation: no permission");
            return;
        }
        try
        {
            locationManager.requestSingleUpdate(gps_provider, this, null);
            Log.d(TAG, "getAndroidLocation: gps provider location requested");

        } catch (Exception e)
        {
            Log.e(TAG, "getAndroidLocation: error", e);
            txtGpsLocation.setText("Error in requesting GPS location " + e.getMessage());
        }

        try
        {
            locationManager.requestSingleUpdate(network_provider, this, null);
            Log.d(TAG, "getAndroidLocation: network provider location requested");
        } catch (Exception e)
        {
            Log.e(TAG, "getAndroidLocation: error", e);
            txtNPLocation.setText("Error in requesting network provider location " + e.getMessage());
        }

    }

    //This is the LocationListener method that gets called after android API 'requestSingleUpdate' completes.
    @Override
    public void onLocationChanged(Location location)
    {
        Log.d(TAG, "onLocationChanged: provider=" + location.getProvider());

        if (location.getProvider().equals(gps_provider))
        {
            showLocation(location, "GPS Location");
        } else
        {
            showLocation(location, "Network Provider Location");
        }
    }


    private void showLocation(Location location, String type)
    {
        String str = location.getLatitude() + "," + location.getLongitude();

        double speed = location.getSpeed(); //spedd in meter/minute
        speed = (speed * 3600) / 1000;

        String str_spd = "Speed(mtrs/min):" + new DecimalFormat("#.##").format(speed);

        Log.d(TAG, "showLocation: speed=" + speed);

        String link = "https://www.google.com/maps/place/" + str;
        //link="geo:"+str;


        String url = str_spd
                + ", <a href='" + link + "'>" + str + "</a>";
        if (type.startsWith("Google"))
        {
            txtGoogleLocation.setText(Html.fromHtml(url));
        } else if (type.startsWith("GPS"))
        {
            txtGpsLocation.setText(Html.fromHtml(url));
        } else
        {
            txtNPLocation.setText(Html.fromHtml(url));
        }


        sendSMS(str_spd + " " + type + ": " + link);
    }

    public void sendSMS(String msg)
    {
        try
        {
            SmsManager smsManager = SmsManager.getDefault();
            Log.d(TAG, "sendSMS: phonenumbers=" + phoneNumbers + " msgs=" + msg);
            if (phoneNumbers.isEmpty())
            {
                Log.d(TAG, "sendSMS: phonenumbers are empty");
                Toast.makeText(getApplicationContext(), "No Phone numbers to send SMS",
                        Toast.LENGTH_LONG).show();
            } else
            {
                String[] nums = phoneNumbers.split(",");
                for (String num : nums)
                {
                    smsManager.sendTextMessage(num, null, msg, null, null);
                }
                Toast.makeText(getApplicationContext(), "Message Sent",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex)
        {
            Toast.makeText(getApplicationContext(), ex.getMessage().toString(),
                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }



   /////////////////// OTHER METHODS ///////////////////////////
    private void setView()
    {
        btnLocation = (Button) findViewById(R.id.btnLocation);
        txtGoogleLocation = (TextView) findViewById(R.id.txtGoogleLocation);
        txtGpsLocation = (TextView) findViewById(R.id.txtGPSLocation);
        txtNPLocation = (TextView) findViewById(R.id.txtNPLocation);

        btnShowNum = (Button) findViewById(R.id.btnShowNum);
        btnUpdateNum = (Button) findViewById(R.id.btnUpdateNum);
        txtPhoneNum = (EditText) findViewById(R.id.txtPhoneNum);


        txtGoogleLocation.setClickable(true);
        txtGoogleLocation.setMovementMethod(LinkMovementMethod.getInstance());
        txtGpsLocation.setClickable(true);
        txtGpsLocation.setMovementMethod(LinkMovementMethod.getInstance());

        setBtnLocation();

        setBtnShowNum();

        setBtnUpdateNum();

        setPhoneNumVisibility(false);//by default invisible.

        readFromFile();//set the phoneNumbers variable.

    }

    private void setLocationToEmpty()
    {
        String EMPTY = "Empty!";
        txtGoogleLocation.setText(EMPTY);
        txtGpsLocation.setText(EMPTY);
        txtNPLocation.setText(EMPTY);
    }

    private void setBtnLocation()
    {

        btnLocation.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Log.d(TAG, "onClick: btnLocation");
                        setLocationToEmpty();
                        getGoogleLocatoin();
                        getAndroidLocation();
                    }
                }
        );
    }


    private void setBtnShowNum()
    {
        btnShowNum.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Log.d(TAG, "onClick: btnShowButton");
                        setPhoneNumVisibility(true);
                        readFromFile();
                    }
                }
        );
    }

    private void setBtnUpdateNum()
    {
        btnUpdateNum.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Log.d(TAG, "onClick: btnUpdateNum");
                        saveToFile();
                        setPhoneNumVisibility(false);
                    }
                }
        );
    }

    private void saveToFile()
    {

        String fileContents = txtPhoneNum.getText().toString();
        if (fileContents == null)
        {
            fileContents = "";
        }
        fileContents = fileContents.toString().trim();
        Log.d(TAG, "saveToFile: content=" + fileContents);

        FileOutputStream outputStream;

        try
        {

            phoneNumbers = fileContents;//set the phoneNumbers

            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(fileContents.getBytes());
            outputStream.close();
        } catch (Exception e)
        {
            Log.e(TAG, "saveToFile: Error", e);
            e.printStackTrace();
            txtGpsLocation.setText("Error in saving phone numbers " + e);
        }
    }



    private void readFromFile()
    {
        try
        {
            FileInputStream inputStream = openFileInput(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String str_ph = "";

            String str = reader.readLine();
            while (str != null)
            {
                str_ph += str;
                str = reader.readLine();
            }

            reader.close();
            inputStream.close();


            Log.d(TAG, "readFromFile: str_ph=" + str_ph);
            txtPhoneNum.setText(str_ph);
            phoneNumbers = str_ph;

        } catch (Exception e)
        {
            Log.e(TAG, "readFromFile: Error ", e);
        }

    }

    private void setPhoneNumVisibility(boolean visibility)
    {
        if (visibility == false)
        {
            btnUpdateNum.setVisibility(View.INVISIBLE);
            txtPhoneNum.setVisibility(View.INVISIBLE);
        } else
        {
            btnUpdateNum.setVisibility(View.VISIBLE);
            txtPhoneNum.setVisibility(View.VISIBLE);
        }
    }

    private void doPermissionsPart()
    {
        permissions.add(ACCESS_FINE_LOCATION);
        permissions.add(ACCESS_COARSE_LOCATION);
        permissions.add(SEND_SMS);

        permissionsToRequest = findUnAskedPermissions(permissions);
        Log.d(TAG, "onCreate: permissionstorequest=" + permissionsToRequest.size());
        for (String str : permissionsToRequest)
        {
            Log.d(TAG, "onCreate: perm to request=" + str);
        }
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (permissionsToRequest.size() > 0)
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                        ALL_PERMISSIONS_RESULT);
        }
    }


    private boolean checkPlayServices()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (apiAvailability.isUserResolvableError(resultCode))
            {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else
                finish();

            return false;
        }
        return true;
    }


    @Override
    protected void onResume()
    {
        super.onResume();

        if (!checkPlayServices())
        {
            Log.d(TAG, "onResume: Please install Google Play services.");
        }
    }


    public void stopLocationUpdates()
    {
        if (mGoogleApiClient.isConnected())
        {
            /*LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(new LocationCallback()
            {

            });*/

            mGoogleApiClient.disconnect();
        }
        locationManager.removeUpdates(this);
    }


    /*protected void startLocationUpdates()
    {
        LocationRequest req = new LocationRequest();
        req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        req.setInterval(MIN_UPDATE_DISTANCE);
        req.setFastestInterval(MIN_UPDATE_TIME);
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(getApplicationContext(), "Enable Permissions", Toast.LENGTH_LONG).show();
        }

        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(req, this, null);
    }*/




   /* public void showMap(Uri geoLocation) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }*/


    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        Log.d(TAG, "onConnected: ");
        Toast.makeText(getApplicationContext(), "Google API Connected", Toast.LENGTH_LONG).show();
        //startLocationUpdates();


    }


    @Override
    public void onConnectionSuspended(int i)
    {
        Log.d(TAG, "onConnectionSuspended: ");
        txtGoogleLocation.setText("Google Location: Connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        Log.d(TAG, "onConnectionFailed: ");
        txtGoogleLocation.setText("Google Location: Connection failed");
    }


    private boolean canMakeSmores()
    {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    private boolean hasPermission(String permission)
    {
        if (canMakeSmores())
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }


    private ArrayList findUnAskedPermissions(ArrayList<String> wanted)
    {
        ArrayList<String> result = new ArrayList();

        for (String perm : wanted)
        {
            if (!hasPermission(perm))
            {
                result.add(perm);
            }
        }

        return result;
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        Log.d(TAG, "onRequestPermissionsResult: request code=" + requestCode);
        switch (requestCode)
        {

            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest)
                {
                    if (!hasPermission(perms))
                    {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0)
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0)))
                        {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which)
                                        {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                            {
                                                requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }

                }

                break;
        }

    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener)
    {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

/*
this only pops up the whatsapp and prints the message - but doesn't send the message automatically.
    private void send_whatsapp()
    {
             String toNumber = "917875556672";

            Intent sendIntent = new Intent("android.intent.action.MAIN");
            sendIntent.setComponent(new ComponentName("com.whatsapp", "com.whatsapp.Conversation"));
            sendIntent.putExtra("jid", toNumber + "@s.whatsapp.net");
            sendIntent.putExtra(Intent.EXTRA_TEXT, "This is automatic message by MyLocation App: "+link);
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setPackage("com.whatsapp");
            sendIntent.setType("text/plain");
            this.startActivity(sendIntent);


    }
*/

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        Log.d(TAG, "onStatusChanged: ");
    }

    @Override
    public void onProviderEnabled(String provider)
    {
        Log.d(TAG, "onProviderEnabled: ");
    }

    @Override
    public void onProviderDisabled(String provider)
    {
        Log.d(TAG, "onProviderDisabled: ");
    }

}


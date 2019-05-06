# AndroidGPSLocation
This is a simple android app that shows the current phone location info and also sends this info as a SMS to a given mobile numbers.

1. Gets location by three methods (Refer to MainActivity.java for the code) - 
   1). Google API -- this is fast and accurate when compared to other two methods - 
           Task<Location> task = LocationServices.getFusedLocationProviderClient(this).getLastLocation();
   2) Use Android API to get GPS location --  this is bit slow as pre my experience.
         locationManager.requestSingleUpdate(gps_provider, this, null);
   3) Use Android API to get Network Provider location.
         locationManager.requestSingleUpdate(network_provider, this, null);

2). Screen looks like this. 
On Clicking the 'Get Location' button, it gets the current location(last updated location) and displays in the respective boxes.

3).You can add the phone number (just numbers without + sign). I added a local mobile number without any country code. 
If you have added any numbers here, App sends the SMS with google map link along with the location co-ordinate and speed info,
every time you click on the 'Get Locaiton' button.





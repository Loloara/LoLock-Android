package com.gunghi.tgwing.lolock.ui;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.common.eventbus.Subscribe;
import com.gunghi.tgwing.lolock.R;
import com.gunghi.tgwing.lolock.Response.ResponseWeather;
import com.gunghi.tgwing.lolock.model.UserInfo;
import com.gunghi.tgwing.lolock.network.LoLockService;
import com.gunghi.tgwing.lolock.network.LoLockServiceGenarator;
import com.gunghi.tgwing.lolock.util.ActivityResultEvent;
import com.gunghi.tgwing.lolock.util.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;

/**
 * Created by joyeongje on 2017. 7. 20..
 */

public class FragmentInfo extends Fragment {

    LoLockService loLockService;

    ImageView weatherIconImageView;
    TextView currentTempatureTextView;
    TextView maxMinTempatureTextView;
    TextView currentLocaleTextView;
    TextView rainPercentTextView;
    TextView cloudAmounTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_info, container, false);

        initFragmentInfo(rootView);
        getWeartherInfo();

        EventBus.getInstance().register(this);
        getSchedule();

        return rootView;
    }

    @Override
    public void onDestroy() {
        //  EventBus.getDefault().unregister(this);
        EventBus.getInstance().unregister(this);
        super.onDestroy();
    }

    private void initFragmentInfo(ViewGroup viewGroup) {
        loLockService = LoLockServiceGenarator.createService(LoLockService.class);

        weatherIconImageView = (ImageView)viewGroup.findViewById(R.id.fragmentInfoWeatherIcon);
        currentTempatureTextView = (TextView)viewGroup.findViewById(R.id.fragmentInfoCurrentTemparature);
        maxMinTempatureTextView = (TextView)viewGroup.findViewById(R.id.fragmentInfoCurrentMaxMinTemparature);
        currentLocaleTextView = (TextView)viewGroup.findViewById(R.id.fragmentInfoWeatherLocale);
        rainPercentTextView = (TextView)viewGroup.findViewById(R.id.fragmentInfoRainPercent);
        cloudAmounTextView = (TextView)viewGroup.findViewById(R.id.fragmentInfoCloudAmount);
    }

    private void getWeartherInfo() {
        String loLockKey = UserInfo.getInstance().getLolockLTID();
        Log.d("lolockey",loLockKey);
        Call<ResponseWeather> responseWeatherCall = loLockService.getWeatherData(loLockKey);
        responseWeatherCall.enqueue(new Callback<ResponseWeather>() {
            @Override
            public void onResponse(Call<ResponseWeather> call, Response<ResponseWeather> response) {
                Log.d("weather",response.toString());
                weatherDataMappingUI(response.body());
            }

            @Override
            public void onFailure(Call<ResponseWeather> call, Throwable t) {
                Log.d("Weather info get fail",call.toString());

            }
        });
    }

    private void weatherDataMappingUI(ResponseWeather response) {

        int minTemperature = response.getMinTemperature();
        int maxTemperature = response.getMaxTemperature();
        maxMinTempatureTextView.setText(minTemperature + "℃ /" + maxTemperature);

        Double curTemperature = response.getTemperature();
        currentTempatureTextView.setText(String.valueOf(curTemperature));

        int probabilityRain = response.getProbabilityRain();
        rainPercentTextView.setText(probabilityRain + "%");

        String sky = response.getSky();
        cloudAmounTextView.setText(sky);


        // TODO: 2017. 7. 23. 주소줘야됨.
        switch (sky) {
            case "맑음":
                break;
            case "구름 조금":
            case "구름 많음":
            case "흐림":
                break;
            case "비":
                break;
            case "눈":
                break;
            default:
                break;
            // TODO: 2017. 7. 23. 이미지정보맵핑
        }


    }

    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };

    private void getSchedule() {

       // getResultsFromApi();

        mProgress = new ProgressDialog(getContext());
        mProgress.setMessage("Calling Google Calendar API ...");

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        // TODO: 2017. 7. 20. 스케줄 정보
    }


     private void getResultsFromApi() {
         if (!isGooglePlayServicesAvailable()) {
             acquireGooglePlayServices();
         } else if (mCredential.getSelectedAccountName() == null) {
             chooseAccount();
         } else if (!isDeviceOnline()) {
             // 인터넷 이용 불가 상태
         } else {
             new MakeRequestTask(mCredential).execute();
         }
     }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(getContext(), new String[]{Manifest.permission.GET_ACCOUNTS})) {
            String accountName = getActivity().getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
  */
 private boolean isGooglePlayServicesAvailable() {
     GoogleApiAvailability apiAvailability =
             GoogleApiAvailability.getInstance();
     final int connectionStatusCode =
             apiAvailability.isGooglePlayServicesAvailable(getContext());
     return connectionStatusCode == ConnectionResult.SUCCESS;
 }

 /**
  * Attempt to resolve a missing, out-of-date, invalid or disabled Google
  * Play Services installation via a user dialog, if possible.
  */
 private void acquireGooglePlayServices() {
     GoogleApiAvailability apiAvailability =
             GoogleApiAvailability.getInstance();
     final int connectionStatusCode =
             apiAvailability.isGooglePlayServicesAvailable(getContext());
     if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
         showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
     }
 }


 /**
  * Display an error dialog showing that Google Play Services is missing
  * or out of date.
  * @param connectionStatusCode code describing the presence (or lack of)
  *     Google Play Services on this device.
  */
 void showGooglePlayServicesAvailabilityErrorDialog(
         final int connectionStatusCode) {
     GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
     Dialog dialog = apiAvailability.getErrorDialog(
             getActivity(),
             connectionStatusCode,
             REQUEST_GOOGLE_PLAY_SERVICES);
     dialog.show();
 }

 /**
  * An asynchronous task that handles the Google Calendar API call.
  * Placing the API calls in their own task ensures the UI stays responsive.
  */
 private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
     private com.google.api.services.calendar.Calendar mService = null;
     private Exception mLastError = null;

     MakeRequestTask(GoogleAccountCredential credential) {
         HttpTransport transport = AndroidHttp.newCompatibleTransport();
         JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
         mService = new com.google.api.services.calendar.Calendar.Builder(
                 transport, jsonFactory, credential)
                 .setApplicationName("Google Calendar API Android Quickstart")
                 .build();
     }

     /**
      * Background task to call Google Calendar API.
      * @param params no parameters needed for this task.
      */
     @Override
     protected List<String> doInBackground(Void... params) {
         try {
             return getDataFromApi();
         } catch (Exception e) {
             mLastError = e;
             cancel(true);
             return null;
         }
     }

     /**
      * Fetch a list of the next 10 events from the primary calendar.
      * @return List of Strings describing returned events.
      * @throws IOException
      */
     private List<String> getDataFromApi() throws IOException {

         Log.d("getDataFromApi","getDataFromApi");
         // 일정 10개가져옴
         // List the next 10 events from the primary calendar.
         DateTime now = new DateTime(System.currentTimeMillis());
         List<String> eventStrings = new ArrayList<String>();
         Events events = mService.events().list("primary")
                 .setMaxResults(10)
                 .setTimeMin(now)
                 .setOrderBy("startTime")
                 .setSingleEvents(true)
                 .execute();
         List<Event> items = events.getItems();
         Log.d("getDataFromApi Sizez",String.valueOf(items.size()));

         for (Event event : items) {
             DateTime start = event.getStart().getDateTime();

             if (start == null) {
                 // All-day events don't have start times, so just use
                 // the start date.
                 start = event.getStart().getDate();
             }
             eventStrings.add(
                     String.format("%s (%s)", event.getSummary(), start));
         }
         return eventStrings;
     }


     @Override
     protected void onPreExecute() {
         mProgress.show();
     }

     @Override
     protected void onPostExecute(List<String> output) {
         mProgress.hide();
         if (output == null || output.size() == 0) {
             // 아무런 결과가없때
         } else {
             output.add(0, "Data retrieved using the Google Calendar API:");
            // 데이터 가져온건가..?
         }
     }

     @Override
     protected void onCancelled() {
         mProgress.hide();
         if (mLastError != null) {
             if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                 showGooglePlayServicesAvailabilityErrorDialog(
                         ((GooglePlayServicesAvailabilityIOException) mLastError)
                                 .getConnectionStatusCode());
             } else if (mLastError instanceof UserRecoverableAuthIOException) {
                 startActivityForResult(
                         ((UserRecoverableAuthIOException) mLastError).getIntent(),
                         REQUEST_AUTHORIZATION);
             } else {
                 // 에러 발생.
             }
         } else {
             // 요청 취소.
         }
        }
    }

    @Subscribe
    public void onActivityResultEvent(ActivityResultEvent activityResultEvent) {
        onActivityResult(activityResultEvent.getRequestCode(),activityResultEvent.getResultCode(),activityResultEvent.getData());
    }

     @Override
     public void onActivityResult(
             int requestCode, int resultCode, Intent data) {

         Log.d("프래그먼트 리절트", "들어옴");
         switch(requestCode) {
             case REQUEST_GOOGLE_PLAY_SERVICES:
                 if (resultCode != RESULT_OK) {
                     // 구글 플레이 서비스를 이용할수 없음..
                 } else {
                     getResultsFromApi();
                 }
                 break;
             case REQUEST_ACCOUNT_PICKER:
                 if (resultCode == RESULT_OK && data != null &&
                         data.getExtras() != null) {
                     String accountName =
                             data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                     if (accountName != null) {
                         SharedPreferences settings =
                                 getActivity().getPreferences(Context.MODE_PRIVATE);
                         SharedPreferences.Editor editor = settings.edit();
                         editor.putString(PREF_ACCOUNT_NAME, accountName);
                         editor.apply();
                         mCredential.setSelectedAccountName(accountName);
                         getResultsFromApi();
                     }
                 }
                 break;
             case REQUEST_AUTHORIZATION:
                 if (resultCode == RESULT_OK) {
                     getResultsFromApi();
                 }
                 break;
         }
     }


}

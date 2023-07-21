package com.ztcsoftware.agrohelp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.SphericalUtil;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.onesignal.OneSignal;
import com.ztcsoftware.agrohelp.model.Crop;
import com.ztcsoftware.agrohelp.model.Cropname;
import com.ztcsoftware.agrohelp.utils.Constants;
import com.ztcsoftware.agrohelp.utils.CryptLib;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;


public class MyCrops extends Fragment {

    public final int MAX_CROP_AREA_LIMIT = 65;
    public final int MIN_CROP_AREA_LIMIT = 10;
    public double cropArea=0;
    public int numberOfCrops=0;
    private SwipeRefreshLayout swipeToReload;
    private EditText searchText;
    TextView noCropText;
   // private RadioGroup radioGroup;
    private Spinner spinner;
    private NumberPicker mNumberPicker;
    private GoogleMap googleMap;
    private ListView listView;
    List<Crop> crops = new ArrayList<>();
    View myView;
    public String email,spinnerSelection;
    Boolean flag;
    Crop cropSelected = null;
    public SharedPreferences prefs;
    public ImageView noContent;
    private String locality;
    private Marker marker,tapMarker;
    private Double latBySearch =0.0, lngBySearch =0.0;
    MapView mMapView;
    ArrayList<Marker> markers = new ArrayList<>();
    static final int POLYGON_POINTS = 5;
    Polygon shape;
    private Double num0,num1;
    Button okBtn;
    JSONArray jsonArray2  = new JSONArray();
    JSONArray coordinates = new JSONArray();
    JSONObject geometry  = new JSONObject();
    JSONObject property  = new JSONObject();
    JSONObject geo_json  = new JSONObject();
    JSONObject cropField = new JSONObject();
    List<LatLng> latLngs = new ArrayList<>();
    List<String> cropnames = new ArrayList<>();
    long spinerIdSelection;

   // protected static final String TAG = "LocationOnOff";

    private GoogleApiClient googleApiClient;
    final static int REQUEST_LOCATION = 199;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);//In this fragment menu item not shown if false
        prefs = getActivity().getSharedPreferences(SignInActivity.PREFS, Context.MODE_PRIVATE);
        //Read email of logged user
        email = prefs.getString("email",null);
        flag = prefs.getBoolean("logged",false);

        // Todo Location Already on  ... start
        final LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && hasGPSDevice(getActivity())) {
            Toast.makeText(getActivity(),R.string.gps_enabled,Toast.LENGTH_SHORT).show();
            // finish();
        }
        // Todo Location Already on  ... end

        if(!hasGPSDevice(getActivity())){
            Toast.makeText(getActivity(),R.string.gps_not_supported,Toast.LENGTH_SHORT).show();
        }

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && hasGPSDevice(getActivity())) {
            Log.e("TAG","Gps already enabled");
            Toast.makeText(getActivity(),R.string.gps_not_enabled,Toast.LENGTH_SHORT).show();
            enableLoc();
        }else{
            Log.e("TAG","Gps already enabled");
            Toast.makeText(getActivity(),R.string.gps_enabled,Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ImageView noContent = myView.findViewById(R.id.noContent);
        noCropText = myView.findViewById(R.id.noCropText);
        swipeToReload = myView.findViewById(R.id.swipe);
        listView = myView.findViewById(R.id.listView);

        if(!crops.isEmpty()) crops.clear();

        if(flag && email!=null){
            ManagerNetwork.getMyCrops(email,null,new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    super.onSuccess(statusCode, headers, response);
                    listView.setAdapter(new CropsAdapter(response,getActivity()));
                    listView.setEmptyView(noContent);
                    if (response.length()!=0)  noCropText.setVisibility(View.INVISIBLE);
                        else
                    noCropText.setVisibility(View.VISIBLE);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    Toast.makeText(getActivity(), R.string.server_error ,Toast.LENGTH_LONG).show();
                }
            });
        }
        else
            Toast.makeText(getActivity(),getResources().getString(R.string.login),Toast.LENGTH_SHORT).show();

        swipeToReload.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                crops.clear();
                loadCrops();
                swipeToReload.setRefreshing(false);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                cropSelected = crops.get(position);

           //     Toast.makeText(getActivity(), cropSelected.get_id(), Toast.LENGTH_SHORT).show();

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(getActivity());
                View mView = getLayoutInflater().inflate(R.layout.edit_dialog_crop,null);

                Button updateBtn = mView.findViewById(R.id.updateBtn);
                Button deleteBtn = mView.findViewById(R.id.deleteBtn);
                Button cancelBtn = mView.findViewById(R.id.cancel);

                final EditText newTopothesia = mView.findViewById(R.id.edit_topothesia);
                final EditText newAreaNum = mView.findViewById(R.id.edit_arithm_strem);
                newTopothesia.setText(cropSelected.getTopothesia());
                newAreaNum.setText(Integer.toString(cropSelected.getStremmata()));

                mBuilder.setView(mView);
                final AlertDialog dialog = mBuilder.create();
                updateBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //update crop data by user
                        updateCrop(cropSelected.get_id(),newTopothesia.getText().toString(),Integer.parseInt( newAreaNum.getText().toString() ));
                        crops.clear();
                        loadCrops();
//                        mainActivity.loadSpinner(email);//update the spinner list
                        dialog.cancel();
                    }
                });
                deleteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //delete crop
                        deleteCrop(cropSelected.get_id());
                        crops.clear();
                        loadCrops();
 //                       mainActivity.loadSpinner(email);//update the spinner list
                        dialog.cancel();
                    }
                });
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                    }
                });
                dialog.show();

            }

        });

        //read from server the totalNumCrop
        ManagerNetwork.readNumCrop(email,null,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    CryptLib _crypt = new CryptLib();
                    String key = CryptLib.SHA256(Constants.KEY_NODE_TO_ANDROID, 32); //32 bytes = 256 bit
                    String iv = Constants.IV_NODE_TO_ANDROID; //16 bytes = 128 bit
                    String message = _crypt.decrypt(response.getString("result"),key,iv);
                    numberOfCrops = Integer.valueOf(message);

                }catch (JSONException ex){
                    ex.printStackTrace();
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
        FloatingActionButton floatingActionButton = myView.findViewById(R.id.floating_action_button);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Check if user is logged
                if (!flag || email ==null) {
                    Toast.makeText(getActivity(),R.string.account_login,Toast.LENGTH_LONG).show();

                }else
                    if(numberOfCrops == 2){
                        Toast.makeText(getActivity(),R.string.no_more_crop_permited,Toast.LENGTH_LONG).show();
                    }else
                if (ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getActivity(), R.string.enable_location, Toast.LENGTH_LONG).show();
                }else{

                    AlertDialog.Builder myBuilder = new AlertDialog.Builder(getActivity());
                    final View myView = getLayoutInflater().inflate(R.layout.custom_dialog ,null);

                    searchText = myView.findViewById(R.id.searchTextCd);
                    Button searchBtn = myView.findViewById(R.id.searchBtnCd);
                    Button cnlBtn = myView.findViewById(R.id.cancelBtn);
                    okBtn = myView.findViewById(R.id.okBtn);
                    //radioGroup = myView.findViewById(R.id.crops);
                    spinner = myView.findViewById(R.id.crops);
                    mNumberPicker = myView.findViewById(R.id.arithmosStr);
                    mNumberPicker.setMinValue(1);
                    mNumberPicker.setMaxValue(100);
                    mMapView = myView.findViewById(R.id.mapViewCd);
                    mMapView.onCreate(savedInstanceState);
                    mMapView.onResume();

                    myBuilder.setView(myView);
                    //Map init and set MapView
                    map();

                    //Read the name of supported crops from agrohelp.pro and AgroHelp
                    // And populate the ArrayString cropnames
                    cropnames.add(0, getResources().getString(R.string.spinnerDefaultText));
                    ManagerNetwork.supportedCropListBySystem(null,new JsonHttpResponseHandler(){
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                            super.onSuccess(statusCode, headers, response);
                            JSONObject jsonData;

                            for(int i=0;i<response.length();i++){
                                try{

                                    jsonData = response.getJSONObject(i);

                                    cropnames.add(jsonData.getString("cropName"));

                                }catch (JSONException e){
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            super.onFailure(statusCode, headers, responseString, throwable);
                        }
                    });

                    // Creating adapter for spinner
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, cropnames);

                    // Drop down layout style - list view with radio button
                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    // attaching data adapter to spinner
                    spinner.setAdapter(dataAdapter);

                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            spinnerSelection  = parent.getItemAtPosition(position).toString();
                            spinerIdSelection = parent.getItemIdAtPosition(position); // 0 is the text selected.. 1,2,3,...
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });

                    final AlertDialog dialog = myBuilder.create();



                    okBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int num = mNumberPicker.getValue();
                            // int rg = radioGroup.getCheckedRadioButtonId();
                            cropnames.clear();


                            if(cropArea == 0)
                                Toast.makeText(getActivity(),R.string.save_crop_error,Toast.LENGTH_LONG).show();
                            else
                                if(cropArea > MAX_CROP_AREA_LIMIT || cropArea<MIN_CROP_AREA_LIMIT)
                                    Toast.makeText(getActivity(),R.string.crop_area_limit,Toast.LENGTH_LONG).show();
                            else
                                {           //rg == myView.findViewById(R.id.radioBtnKiwi).getId()
                                    if (spinnerSelection.equals("Kiwi")){
                                        //save crop by point
                                        // saveNewCrop(email,"Ακτινίδιο",num,locality,latBySearch.floatValue(),lngBySearch.floatValue());
                                        //save crop by polygon
                                        saveToAgroMonitoring(convertStringToUTF8("Kiwi"),num);

                                        //Send Tag to OneSignal server Kiwi
                                        OneSignal.sendTag("kiwi","1");
                                        crops.clear();
                                        dialog.cancel();
                                        loadCrops();
                                        //getFragmentManager().beginTransaction().detach(MyCrops.this).attach(MyCrops.this).commit();
                                    }
                                    else  //rg == myView.findViewById(R.id.radioBtnOlive).getId()
                                    if (spinnerSelection.equals("Olive")){
                                        //save crop by point
                                        // saveNewCrop(email,"Ελιά",num,locality,latBySearch.floatValue(),lngBySearch.floatValue());
                                        saveToAgroMonitoring(convertStringToUTF8("Olive"),num);
                                        //Send Tag to OneSignal server Olive
                                        OneSignal.sendTag("olive","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    }else if(spinnerSelection.equals("Tobacco")){
                                        saveToAgroMonitoring(convertStringToUTF8("Tobacco"),num);
                                        OneSignal.sendTag("tobacco","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    }else if (spinnerSelection.equals("Rice")){
                                        saveToAgroMonitoring(convertStringToUTF8("Rice"),num);
                                        OneSignal.sendTag("rice","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    }else if (spinnerSelection.equals("Wheat")){
                                        saveToAgroMonitoring(convertStringToUTF8("Wheat"),num);
                                        OneSignal.sendTag("wheat","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    }else if (spinnerSelection.equals("Apple")){
                                        saveToAgroMonitoring(convertStringToUTF8("Apple"),num);
                                        OneSignal.sendTag("apple","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    } else if (spinnerSelection.equals("Cotton")){
                                        saveToAgroMonitoring(convertStringToUTF8("Cotton"),num);
                                        OneSignal.sendTag("cotton","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    } else if( spinnerSelection.equals("Strawberry")){
                                        saveToAgroMonitoring(convertStringToUTF8("Strawberry"),num);
                                        OneSignal.sendTag("strawberry","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    }else if (spinnerSelection.equals("Vineyard")){
                                        saveToAgroMonitoring(convertStringToUTF8("Vineyard"),num);
                                        OneSignal.sendTag("vineyard","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    }else if (spinnerSelection.equals("Tomato")){
                                        saveToAgroMonitoring(convertStringToUTF8("Tomato"),num);
                                        OneSignal.sendTag("tomato","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    }else if (spinnerSelection.equals("Potato")){
                                        saveToAgroMonitoring(convertStringToUTF8("Potato"),num);
                                        OneSignal.sendTag("potato","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    } else if (spinnerSelection.equals("Watermelon")){
                                        saveToAgroMonitoring(convertStringToUTF8("Watermelon"),num);
                                        OneSignal.sendTag("watermelon","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    } else if (spinnerSelection.equals("Cherry")){
                                        saveToAgroMonitoring(convertStringToUTF8("Cherry"),num);
                                        OneSignal.sendTag("cherry","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    }else if (spinnerSelection.equals("Corn")){
                                        saveToAgroMonitoring(convertStringToUTF8("Corn"),num);
                                        OneSignal.sendTag("corn","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    } else if (spinnerSelection.equals("Sugarbeet")) {
                                        saveToAgroMonitoring(convertStringToUTF8("Sugarbeet"),num);
                                        OneSignal.sendTag("sugarbeet","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                        } else if (spinnerSelection.equals("Orange")){
                                        saveToAgroMonitoring(convertStringToUTF8("Orange"),num);
                                        OneSignal.sendTag("orange","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    } else if (spinnerSelection.equals("Lemon")){
                                        saveToAgroMonitoring(convertStringToUTF8("Lemon"),num);
                                        OneSignal.sendTag("lemon","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    } else if (spinnerSelection.equals("Bean")){
                                        saveToAgroMonitoring(convertStringToUTF8("Bean"),num);
                                        OneSignal.sendTag("bean","1");
                                        dialog.cancel();
                                        crops.clear();
                                        loadCrops();
                                    }



                                }
                        }

                    });

                    cnlBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            cropnames.clear();
                            dialog.cancel();
                        }
                    });
                    searchBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            geoLocate();
                        }
                    });

                    //********* full screen custom dialog
                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                    lp.copyFrom(dialog.getWindow().getAttributes());
                    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                    lp.height = WindowManager.LayoutParams.MATCH_PARENT;
                    dialog.show();
                    dialog.getWindow().setAttributes(lp);
                    //**********
                }

            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        myView = inflater.inflate(R.layout.activity_mycrops,container,false);
        TextView textView = myView.findViewById(R.id.titleAction);
        textView.setText(R.string.cropListTitle);// Οι Καλλιέργειές μου
        return myView;
        }


    private void goToLocation(double lat,double lng){
        LatLng katerini = new LatLng(lat,lng);
        googleMap.addMarker(new MarkerOptions().position(katerini).title("Town Center"));

        CameraPosition cameraPosition = new CameraPosition.Builder().target(katerini).zoom(12).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public void geoLocate()  {
        googleMap.clear(); // to remove last added marker

        String location = searchText.getText().toString();
        Geocoder geocoder = new Geocoder(getActivity());
        try {
            List<Address> list = geocoder.getFromLocationName(location,1);

            if(list.isEmpty() ) {
               // goToLocation(40.271313,22.508842);
                Toast.makeText(getActivity(),R.string.location_not_found,Toast.LENGTH_LONG).show();
            }else{
                Address address = list.get(0);
                locality = address.getLocality();

                goToLocation(address.getLatitude(),address.getLongitude());
                final MarkerOptions options = new MarkerOptions()
                        .title(locality)
                        .position(new LatLng(address.getLatitude(),address.getLongitude()))
                        .draggable(true);


                latBySearch =address.getLatitude();
                lngBySearch =address.getLongitude();
                marker = googleMap.addMarker(options);
            }


//        editor.putString("topothesiaName",locality);
//        editor.putFloat("topothesiaLat",latBySearch.floatValue());
//        editor.putFloat("topothesiaLng",lngBySearch.floatValue());
//        editor.commit();


        }catch (IOException e){
            e.printStackTrace();
        }

    }
//    private void saveNewCrop(String email,String crop,int stremmata,String locality,float lat,float lng) {
//        //Hash input values on a token
//        String token = CreateToken.saveUserCrop(crop,email,crop,stremmata,locality,lat,lng);
//        ManagerNetwork.post("/android/crop/agrohelp/user/"+token,null,new JsonHttpResponseHandler(){
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                try{
//                    int error = response.getInt("error");
//                    if(error == 1){
//                        String message = response.getString("message");
//                        if(getActivity() != null)
//                            Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
//                    }else {
//                        String message = response.getString("message");
//                        if(getActivity() != null)
//                            Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
//                    }
//                }catch (JSONException ex){
//                    ex.printStackTrace();
//                    Log.d("Problem",ex.getMessage());
//                }
//            }
//        });
//    }

    private void saveNewCropWithPolygonId(String email,String crop,int stremmata,String locality,float lat,float lng,String polygonId){
        //Hash input values on a token
        String token = CreateToken.saveUserCropWithPolygonId(crop,email,crop,stremmata,locality,lat,lng,polygonId);
        ManagerNetwork.post("/android/crop/agrohelp/user/"+token,null,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try{
                    int error = response.getInt("error");
                    if(error == 1){
                        String message = response.getString("message");
                        if(getActivity() != null)
                            Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
                    }else {
                        String message = response.getString("message");
//                        if(getActivity() != null)
//                            Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
                        addCrop();
                    }
                }catch (JSONException ex){
                    ex.printStackTrace();
                    Log.d("Problem",ex.getMessage());
                }
            }
        });

    }
    //add one crop to user account
    private void addCrop(){
        int numTemp = ++numberOfCrops;
        ManagerNetwork.updateNumCrop(email,numTemp,null,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

            }
        });
    }
    //remove one crop to user account
    private void removeCrop(){
        int numTemp = --numberOfCrops;
        ManagerNetwork.updateNumCrop(email,numTemp,null,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);

            }
        });
    }

    //save polygon to AgroMonitoring and read response
    private void map(){
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());

        }catch (Exception e){
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                googleMap.setMyLocationEnabled(true);
                UiSettings uiSettings = googleMap.getUiSettings();
                uiSettings.setZoomControlsEnabled(true);
                uiSettings.setCompassEnabled(true);
                uiSettings.setZoomGesturesEnabled(true);

                //set Map Style
                try {
                    boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(),R.raw.style_json));

                    if(!success) {
                        Log.e("MapsActivityRaw","Style parsing failed.");
                    }
                }catch (Resources.NotFoundException e){
                    Log.e("MapsActivityRaw","Can't find style.",e);
                }

                //save o point from map to database
//                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
//                    @Override
//                    public void onMapClick(LatLng latLng) {
//                        if(tapMarker!=null) tapMarker.remove();
//                        Geocoder geocoder = new Geocoder(getActivity());
//                        try {
//                            List<Address> list = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
//                            Address address = list.get(0);
//                            locality = address.getLocality();
//                        }catch (IOException e){
//                            e.printStackTrace();
//                        }
//
//                        tapMarker = googleMap.addMarker(new MarkerOptions()
//                                .title(locality)
//                                .snippet(latLng.latitude+" "+latLng.longitude)
//                                .position(latLng).draggable(true));
//
//                        latBySearch = latLng.latitude;
//                        lngBySearch = latLng.longitude;
//                    }
//                });

                // save an area (polygon) from map to Agromonitoring
//                final JSONArray jsonArray2  = new JSONArray();
//                final JSONArray coordinates = new JSONArray();
//                final JSONObject geometry  = new JSONObject();
//                final JSONObject property  = new JSONObject();
//                final JSONObject geo_json  = new JSONObject();
//                final JSONObject cropField = new JSONObject();

                googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(LatLng latLng) {
                        if(markers.size() == POLYGON_POINTS){
                            removeEverything();
                            //clear JSONArrays and JSONObjects if user want to re-enter his/her choice
                            jsonArray2  = new JSONArray();
                            coordinates = new JSONArray();
                            geometry  = new JSONObject();
                            property  = new JSONObject();
                            geo_json  = new JSONObject();
                            cropField = new JSONObject();
                            latLngs = new ArrayList<>();
                            cropArea =0;
                        }
                        JSONArray coords = new JSONArray();
                        //read lat lng from tap on map
                        double lat = latLng.latitude;
                        double lng = latLng.longitude;
                        MarkerOptions options = new MarkerOptions()
                                .title("Area")
                                .draggable(true)
                                .position(new LatLng(lat,lng))
                                .snippet("Point here");
                        markers.add(googleMap.addMarker(options));
                        if(markers.size()< 2 || markers.size()>4){//The first and last marker with 2 decimal digits
                            String rLat = String.format(Locale.US,"%.1f",lat);
                            String rLng = String.format(Locale.US,"%.1f",lng);
                            try {
                                coords.put(Double.parseDouble(rLng));
                                coords.put(Double.parseDouble(rLat));
                                jsonArray2.put(coords);
                                latLngs.add(new LatLng(lat,lng));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (NumberFormatException e){
                                e.printStackTrace();
                            }
                        }else{//the other markers with 4 decimal digits
                            String rLat = String.format(Locale.US,"%.4f",lat);
                            String rLng = String.format(Locale.US,"%.4f",lng);
                            try {
                                coords.put(Double.parseDouble(rLng));
                                coords.put(Double.parseDouble(rLat));
                                jsonArray2.put(coords);
                                latLngs.add(new LatLng(lat,lng));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            } catch (NumberFormatException e){
                                e.printStackTrace();
                            }
                        }

                        if(markers.size() == POLYGON_POINTS){
                            drawPolygon();
                                //Calculate polygon area σε στρέμματα
                                cropArea = SphericalUtil.computeArea(latLngs)/1000;

                            Toast.makeText(getActivity(),getResources().getString(R.string.polygon_area)+" : "+String.format(Locale.US,"%.1f",cropArea)+" "+getResources().getString(R.string.stremmata_lbl),Toast.LENGTH_LONG).show();

                            coordinates.put(jsonArray2);
                                        //Read from the last tap on the map from user the coords and read the locality
                                        Geocoder geocoder = new Geocoder(getActivity());
                                        List<Address> list = null;
                                        try {
                                            list = geocoder.getFromLocation(lat,lng,1);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        Address address = list.get(0);
                                        locality = address.getLocality();
                                        //The locality saved in agromonitoring server
                            try {
                                geometry.put("type","Polygon");
                                geometry.put("coordinates",coordinates);
                                property.put("property","name of owner");
                                geo_json.put("type","Feature");
                                geo_json.put("properties",property);
                                geo_json.put("geometry",geometry);
                                cropField.put("name", convertStringToUTF8(locality));
                                cropField.put("geo_json",geo_json);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                           // Toast.makeText(getActivity(),String.valueOf(geo_json),Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }
    // @crop name of crop Kiwi or Olive
    // @num area in stremmata
    private void saveToAgroMonitoring(String crop,int num){
        StringEntity entity = null;
        try {
            //Sent GeoJSON object to Agromonitoring server to create polygon(crop) @cropField
            entity = new StringEntity(cropField.toString());
            ManagerNetwork.postGJSON(getActivity(), Constants.AGROMONITORING, entity,
                    "application/json",
                    new JsonHttpResponseHandler(){
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            super.onSuccess(statusCode, headers, response);
                            String polygonId = null;
                            String polygonName = null;
                            JSONArray polygonCenter;

                            try {
                                polygonId     = response.getString("id");
                                polygonName   = response.getString("name");
                                polygonCenter = response.getJSONArray("center");
                                //Polygon center coords, used to read locality
                                num0 = polygonCenter.getDouble(0);//longtitude
                                num1 = polygonCenter.getDouble(1);//latitude
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            //Read locality name from polygon center coords
                            Geocoder geocoder = new Geocoder(getActivity());
                            try {
                                List<Address> list = geocoder.getFromLocation(num1,num0,1);
                                Address address = list.get(0);
                                locality = address.getLocality();
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                            // saveNewCrop(email,"Ακτινίδιο",num,locality,latBySearch.floatValue(),lngBySearch.floatValue());
                            //save to ATLAS db
                            saveNewCropWithPolygonId(email,crop,num,convertStringToUTF8(locality),num1.floatValue() ,num0.floatValue(),polygonId);

                            Toast.makeText(getActivity(),R.string.save_crop,Toast.LENGTH_LONG).show();


                            okBtn.setEnabled(true);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            super.onFailure(statusCode, headers, throwable, errorResponse);
                            //String message = errorResponse.toString();
                            if(statusCode==422)//error code from agromonitoring server
                                Toast.makeText(getActivity(),R.string.save_error_422,Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(getActivity(),R.string.save_error,Toast.LENGTH_LONG).show();
                            okBtn.setEnabled(false);
                        }
                    });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    // convert internal Java String format to UTF-8 solve greek char appearance in agromonitoring
    //polygon list title
    private static String convertStringToUTF8(String s) {
        String out = null;
        try {
            out = new String(s.getBytes("UTF-8"), "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
        return out;
    }

    public  void drawPolygon(){
        PolygonOptions polygonOptions = new PolygonOptions()
                .fillColor(Color.GREEN)
                .strokeWidth(3)
                .strokeColor(Color.RED);
        for (int i =0;i<POLYGON_POINTS;i++)
            polygonOptions.add(markers.get(i).getPosition());

        shape = googleMap.addPolygon(polygonOptions);

    }
    public void removeEverything(){
        for(Marker marker : markers){
            marker.remove();
        }
        markers.clear();
        shape.remove();
        shape= null;
    }

    public  void loadCrops(){
        ManagerNetwork.getMyCrops(email,null,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);
                listView.setAdapter(new CropsAdapter(response,getActivity()));
                listView.setEmptyView(noContent);
                if(response.length()!=0)
                    noCropText.setVisibility(View.INVISIBLE);
                    else
                        noCropText.setVisibility(View.VISIBLE);

            }
        });
    }

    private void deleteCrop(String cropId) {
        //send cropId to Atlas db and return the polygonId
        ManagerNetwork.sendPolygonId(cropId,null,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {

                //send a DELETE post to Agromonitoring server from crop erase
                JSONObject jsonData;
                for(int i=0;i<response.length();i++){
                    try{
                        CryptLib _crypt = new CryptLib();
                        String key = CryptLib.SHA256(Constants.KEY_NODE_TO_ANDROID, 32); //32 bytes = 256 bit
                        String iv = Constants.IV_NODE_TO_ANDROID; //16 bytes = 128 bit
                        jsonData = response.getJSONObject(i);
                        String polygonId = _crypt.decrypt(jsonData.getString("polygonId"),key,iv);
                        RequestQueue queue = Volley.newRequestQueue(getActivity());
                        String url = "http://api.agromonitoring.com/agro/1.0/polygons/"+polygonId+"?appid="+Constants.AGRO_API;
                        StringRequest dr = new StringRequest(Request.Method.DELETE, url,
                                new Response.Listener<String>()
                                {
                                    @Override
                                    public void onResponse(String response) {
                                        // response
                                       // Toast.makeText(getActivity(), response, Toast.LENGTH_LONG).show();
                                    }
                                },
                                new Response.ErrorListener()
                                {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        // error.
                                       Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_LONG).show();
                                    }
                                }
                        );
                        queue.add(dr);

                    }catch (JSONException e){
                        e.printStackTrace();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        });

        ManagerNetwork.delCrop(cropId,null,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    String message = response.getString("message");
                    //update user account
                    removeCrop();
                    Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
                }catch (JSONException ex){

                }
            }
        });
    }
    private void updateCrop(String cropId,String areaName,int areaCrop){
        ManagerNetwork.updateCrop(cropId,convertStringToUTF8(areaName),areaCrop,null,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    int error = response.getInt("error");
                    if(error == 1){
                        String message = response.getString("message");
                        Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
                    }else  if (error == 2){
                        String message = response.getString("message");
                        Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
                    }

                }catch (JSONException ex){

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                String message= errorResponse.toString();
                Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();

            }
        });
    }

    public class CropsAdapter extends BaseAdapter {
        private final JSONArray jsonArray;
        private final Activity activity;

        public CropsAdapter(JSONArray jsonArray, Activity activity) {
            this.jsonArray = jsonArray;
            this.activity = activity;
        }

        @Override
        public int getCount() {
            if (null == jsonArray) return 0;
            return jsonArray.length();
        }

        @Override
        public JSONObject getItem(int position) {
            if (null == jsonArray) return null;
            JSONObject x = null;
            try {
                x = jsonArray.getJSONObject(position);
            }catch (JSONException e){
                e.printStackTrace();
            }
            return x;
        }

        @Override
        public long getItemId(int position) {
            JSONObject jsonObject = getItem(position);
            return jsonObject.optLong("id");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null)
                convertView = activity.getLayoutInflater().inflate(R.layout.crop_row,null);
            ImageView image = convertView.findViewById(R.id.pic_crop);
            TextView topothesia = convertView.findViewById(R.id.topothesia);
            TextView ar_str = convertView.findViewById(R.id.ar_str);

            JSONObject jsondata = getItem(position);
            Crop crop = new Crop();
            try {
                CryptLib _crypt = new CryptLib();
                String key = CryptLib.SHA256(Constants.KEY_NODE_TO_ANDROID, 32); //32 bytes = 256 bit
                String iv = Constants.IV_NODE_TO_ANDROID; //16 bytes = 128 bit
                String result= _crypt.decrypt(jsondata.getString("cropName"),key,iv);

               if (result.equals("Olive"))
                    Glide.with(getActivity()).asBitmap().load(R.drawable.olive).into(image);
                else if (result.equals("Kiwi"))
                    Glide.with(getActivity()).asBitmap().load(R.drawable.kiwi512).into(image);
                else if (result.equals("Tobacco"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.tobacco).into(image);
               else if (result.equals("Rice"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.rice).into(image);
               else if (result.equals("Wheat"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.wheat).into(image);
               else if (result.equals("Apple"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.apple).into(image);
               else if (result.equals("Cotton"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.cotton).into(image);
               else if (result.equals("Strawberry"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.strawberry).into(image);
               else if (result.equals("Vineyard"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.grape).into(image);
               else if (result.equals("Tomato"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.tomato).into(image);
               else if (result.equals("Potato"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.potato).into(image);
               else if (result.equals("Watermelon"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.watermelon).into(image);
               else if (result.equals("Cherry"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.cherry).into(image);
               else if (result.equals("Corn"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.corn).into(image);
               else if (result.equals("Sugarbeet"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.sugabeet).into(image);
               else if (result.equals("Orange"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.orange).into(image);
               else if (result.equals("Lemon"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.lemon).into(image);
               else if (result.equals("Bean"))
                   Glide.with(getActivity()).asBitmap().load(R.drawable.beans).into(image);

                crop.set_id( _crypt.decrypt(jsondata.getString("_id"),key,iv));

//                crop.setTopothesia(jsondata.getJSONObject("topothesia").getString("name"));
//                topothesia.setText(jsondata.getJSONObject("topothesia").getString("name"));

                crop.setTopothesia(_crypt.decrypt(jsondata.getString("topothesia"),key,iv));
                topothesia.setText(_crypt.decrypt(jsondata.getString("topothesia"),key,iv));

                crop.setStremmata(Integer.parseInt(_crypt.decrypt(jsondata.getString("stremmata"),key,iv)));
                ar_str.setText(_crypt.decrypt(jsondata.getString("stremmata"),key,iv));

                crops.add(crop);
            }catch (JSONException e){
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return convertView;
        }
    }

    private boolean hasGPSDevice(Context context) {
        final LocationManager mgr = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        if (mgr == null)
            return false;
        final List<String> providers = mgr.getAllProviders();
        if (providers == null)
            return false;
        return providers.contains(LocationManager.GPS_PROVIDER);
    }
    private void enableLoc() {

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(getActivity())
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {

                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            googleApiClient.connect();
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {

                            Log.d("Location error","Location error " + connectionResult.getErrorCode());
                        }
                    }).build();
            googleApiClient.connect();
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(getActivity(), REQUEST_LOCATION);

                          //  finish();
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                }
            }
        });
    }

}


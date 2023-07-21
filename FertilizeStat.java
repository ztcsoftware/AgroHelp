package com.ztcsoftware.agrohelp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.ztcsoftware.agrohelp.utils.Constants;
import com.ztcsoftware.agrohelp.utils.CryptLib;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

public class FertilizeStat extends Fragment {
    protected  View mView;
    public SharedPreferences prefs;
    public String email,cropId;
    public int yearNow;
    public ArrayList<String> years;
    private CombinedChart chart;
    public List<String> barChartLbl;
    public BarChart barChart;
    public Boolean flag;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Calendar calendar = Calendar.getInstance();
        yearNow = calendar.get(Calendar.YEAR);
        mView = inflater.inflate(R.layout.activity_fertilize_stat,container,false);
        prefs = getActivity().getSharedPreferences(SignInActivity.PREFS, Context.MODE_PRIVATE);
        //Read email of logged user
        email = prefs.getString("email",null);
        //Read cropId of selected crop from Spinner
        cropId = prefs.getString("_id",null);
        flag = prefs.getBoolean("logged",false);

        return mView;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView noDataImg = mView.findViewById(R.id.noDataImg);
        noDataImg.setVisibility(View.GONE);
        ImageView noDataImgBarChart = mView.findViewById(R.id.noDataImgBarChart);
        noDataImgBarChart.setVisibility(View.GONE);

        TextView noDataTxt = mView.findViewById(R.id.noDataText);
        noDataTxt.setVisibility(View.GONE);
        TextView noDataTxtBarChart = mView.findViewById(R.id.noDataTextBarChart);
        noDataTxtBarChart.setVisibility(View.GONE);

        barChartLbl = new ArrayList<>();
        //Combined chart definition
        chart = mView.findViewById(R.id.chartFert);
        barChart = mView.findViewById(R.id.chartPerYear);
        // draw bars behind lines
        chart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.LINE, CombinedChart.DrawOrder.BAR
        });
        //Spinner definition
        Spinner spinnerYear = mView.findViewById(R.id.spinnerYears);
        if(flag && email!=null)
            //Populate spinner with Fertilize/treatment years
            ManagerNetwork.treatmentNPKYears(email,cropId,null, new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                    super.onSuccess(statusCode, headers, response);
                    if(response.length() == 0){
                        chart.setVisibility(View.GONE);
                        noDataImg.setVisibility(View.VISIBLE);
                        noDataTxt.setVisibility(View.VISIBLE);
                        barChart.setVisibility(View.GONE);
                        noDataImgBarChart.setVisibility(View.VISIBLE);
                        noDataTxtBarChart.setVisibility(View.VISIBLE);
                    }else {
                        years = new ArrayList<>();
                        for(int i=0; i <response.length();i++){
                            JSONObject jsonData = null;
                            try{
                                jsonData = response.getJSONObject(i);
                                CryptLib _crypt = new CryptLib();
                                String key = CryptLib.SHA256(Constants.KEY_NODE_TO_ANDROID, 32); //32 bytes = 256 bit
                                String iv = Constants.IV_NODE_TO_ANDROID;  //16 bytes = 128 bit
                                String year = _crypt.decrypt(jsonData.getString("year"),key,iv);
                                years.add(year);
                                barChartLbl.add(year);
                            }catch (JSONException e){
                                e.printStackTrace();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        if(isVisible() && getActivity()!=null){
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(Objects.requireNonNull(getActivity()),
                                    R.layout.spinner_item,years);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerYear.setAdapter(adapter);

                            spinnerYear.setSelection(getIndex(spinnerYear,String.valueOf(yearNow)));
                            //Read the year labels and call the method to draw barchart
                            drawBarChart(barChartLbl,yearNow);
                        }
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    Toast.makeText(getActivity(), R.string.data_fail, Toast.LENGTH_SHORT).show();

                }
            });
        else
            Toast.makeText(getActivity(),getResources().getString(R.string.login),Toast.LENGTH_SHORT).show();

      //  spinnerYear.setSelection(getIndex(spinnerYear,String.valueOf(yearNow)));
        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String spinnerValue = parent.getItemAtPosition(position).toString();
            Legend legend = chart.getLegend();

            chart.setPinchZoom(true);
            chart.setScaleEnabled(true);
            ArrayList<BarEntry>   dataValuesWeightNitrogen = new ArrayList<>();
            ArrayList<BarEntry> dataValuesWeightPhosphorus = new ArrayList<>();
            ArrayList<BarEntry>  dataValuesWeightPotassium = new ArrayList<>();
            ArrayList<Entry>                dataValuesCost = new ArrayList<>();

            //Read from database weight of Nitrogen, Phosphorus, Potassium and price (post to sever)
            ManagerNetwork.treatmentNPKWeightCost(email,cropId,Integer.parseInt(spinnerValue),null,new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    super.onSuccess(statusCode, headers, response);
                    if(response.length()==0){
                        //chart.setVisibility(View.GONE);
                        chart.setVisibility(View.INVISIBLE);
                        noDataImg.setVisibility(View.VISIBLE);
                        noDataTxt.setVisibility(View.VISIBLE);
                    }else {
                        chart.setVisibility(View.VISIBLE);
                        noDataImg.setVisibility(View.GONE);
                        noDataTxt.setVisibility(View.GONE);
                        dataValuesCost.clear();
                        dataValuesWeightNitrogen.clear();
                        dataValuesWeightPhosphorus.clear();
                        dataValuesWeightPotassium.clear();

                        try {
                            CryptLib _crypt = new CryptLib();
                            String key = CryptLib.SHA256(Constants.KEY_NODE_TO_ANDROID, 32); //32 bytes = 256 bit
                            String iv = Constants.IV_NODE_TO_ANDROID; //16 bytes = 128 bit

                            //Weight Nitrogen
                            String nitroJan = _crypt.decrypt(response.getString("nitrogenJan"), key, iv);
                            dataValuesWeightNitrogen.add(new BarEntry(0f, Integer.parseInt(nitroJan)));
                            String nitroFeb = _crypt.decrypt(response.getString("nitrogenFeb"), key, iv);
                            dataValuesWeightNitrogen.add(new BarEntry(1f, Integer.parseInt(nitroFeb)));
                            String nitroMar = _crypt.decrypt(response.getString("nitrogenMar"), key, iv);
                            dataValuesWeightNitrogen.add(new BarEntry(2f, Integer.parseInt(nitroMar)));
                            String nitroApr = _crypt.decrypt(response.getString("nitrogenApr"), key, iv);
                            dataValuesWeightNitrogen.add(new BarEntry(3f, Integer.parseInt(nitroApr)));
                            String nitroMay = _crypt.decrypt(response.getString("nitrogenMay"), key, iv);
                            dataValuesWeightNitrogen.add(new BarEntry(4f, Integer.parseInt(nitroMay)));
                            String nitroJun = _crypt.decrypt(response.getString("nitrogenJun"), key, iv);
                            dataValuesWeightNitrogen.add(new BarEntry(5f, Integer.parseInt(nitroJun)));
                            String nitroJul = _crypt.decrypt(response.getString("nitrogenJul"), key, iv);
                            dataValuesWeightNitrogen.add(new BarEntry(6f, Integer.parseInt(nitroJul)));
                            String nitroAug = _crypt.decrypt(response.getString("nitrogenAug"), key, iv);
                            dataValuesWeightNitrogen.add(new BarEntry(7f, Integer.parseInt(nitroAug)));
                            String nitroSep = _crypt.decrypt(response.getString("nitrogenSep"), key, iv);
                            dataValuesWeightNitrogen.add(new BarEntry(8f, Integer.parseInt(nitroSep)));
//                            String nitroOct = _crypt.decrypt(response.getString("nitrogenOct"), key, iv);
//                            dataValuesWeightNitrogen.add(new BarEntry(9f, Integer.parseInt(nitroOct)));
//                            String nitroNov = _crypt.decrypt(response.getString("nitrogenNov"), key, iv);
//                            dataValuesWeightNitrogen.add(new BarEntry(10f, Integer.parseInt(nitroNov)));
//                            String nitroDec = _crypt.decrypt(response.getString("nitrogenDec"), key, iv);
//                            dataValuesWeightNitrogen.add(new BarEntry(11f, Integer.parseInt(nitroDec)));

                            //Weight Phoshorus
                            String  phosJan = _crypt.decrypt(response.getString("phosphorusJan"), key, iv);
                            dataValuesWeightPhosphorus.add(new BarEntry(0f,Integer.parseInt(phosJan)));
                            String  phosFeb = _crypt.decrypt(response.getString("phosphorusFeb"), key, iv);
                            dataValuesWeightPhosphorus.add(new BarEntry(1f,Integer.parseInt(phosFeb)));
                            String  phosMar = _crypt.decrypt(response.getString("phosphorusMar"), key, iv);
                            dataValuesWeightPhosphorus.add(new BarEntry(2f,Integer.parseInt(phosMar)));
                            String  phosApr = _crypt.decrypt(response.getString("phosphorusApr"), key, iv);
                            dataValuesWeightPhosphorus.add(new BarEntry(3f,Integer.parseInt(phosApr)));
                            String  phosMay = _crypt.decrypt(response.getString("phosphorusMay"), key, iv);
                            dataValuesWeightPhosphorus.add(new BarEntry(4f,Integer.parseInt(phosMay)));
                            String  phosJun = _crypt.decrypt(response.getString("phosphorusJun"), key, iv);
                            dataValuesWeightPhosphorus.add(new BarEntry(5f,Integer.parseInt(phosJun)));
                            String  phosJul = _crypt.decrypt(response.getString("phosphorusJul"), key, iv);
                            dataValuesWeightPhosphorus.add(new BarEntry(6f,Integer.parseInt(phosJul)));
                            String  phosAug = _crypt.decrypt(response.getString("phosphorusAug"), key, iv);
                            dataValuesWeightPhosphorus.add(new BarEntry(7f,Integer.parseInt(phosAug)));
                            String  phosSep = _crypt.decrypt(response.getString("phosphorusSep"), key, iv);
                            dataValuesWeightPhosphorus.add(new BarEntry(8f,Integer.parseInt(phosSep)));
//                            String  phosOct = _crypt.decrypt(response.getString("phosphorusOct"), key, iv);
//                            dataValuesWeightPhosphorus.add(new BarEntry(9f,Integer.parseInt(phosOct)));
//                            String  phosNov = _crypt.decrypt(response.getString("phosphorusNov"), key, iv);
//                            dataValuesWeightPhosphorus.add(new BarEntry(10f,Integer.parseInt(phosNov)));
//                            String  phosDec = _crypt.decrypt(response.getString("phosphorusDec"), key, iv);
//                            dataValuesWeightPhosphorus.add(new BarEntry(11f,Integer.parseInt(phosDec)));

                            //Weight Potassium
                            String  potasJan = _crypt.decrypt(response.getString("potassiumJan"), key, iv);
                            dataValuesWeightPotassium.add(new BarEntry(0f,Integer.parseInt(potasJan)));
                            String  potasFeb = _crypt.decrypt(response.getString("potassiumFeb"), key, iv);
                            dataValuesWeightPotassium.add(new BarEntry(1f,Integer.parseInt(potasFeb)));
                            String  potasMar = _crypt.decrypt(response.getString("potassiumMar"), key, iv);
                            dataValuesWeightPotassium.add(new BarEntry(2f,Integer.parseInt(potasMar)));
                            String  potasApr = _crypt.decrypt(response.getString("potassiumApr"), key, iv);
                            dataValuesWeightPotassium.add(new BarEntry(3f,Integer.parseInt(potasApr)));
                            String  potasMay = _crypt.decrypt(response.getString("potassiumMay"), key, iv);
                            dataValuesWeightPotassium.add(new BarEntry(4f,Integer.parseInt(potasMay)));
                            String  potasJun = _crypt.decrypt(response.getString("potassiumJun"), key, iv);
                            dataValuesWeightPotassium.add(new BarEntry(5f,Integer.parseInt(potasJun)));
                            String  potasJul = _crypt.decrypt(response.getString("potassiumJul"), key, iv);
                            dataValuesWeightPotassium.add(new BarEntry(6f,Integer.parseInt(potasJul)));
                            String  potasAug = _crypt.decrypt(response.getString("potassiumAug"), key, iv);
                            dataValuesWeightPotassium.add(new BarEntry(7f,Integer.parseInt(potasAug)));
                            String  potasSep = _crypt.decrypt(response.getString("potassiumSep"), key, iv);
                            dataValuesWeightPotassium.add(new BarEntry(8f,Integer.parseInt(potasSep)));
//                            String  potasOct = _crypt.decrypt(response.getString("potassiumOct"), key, iv);
//                            dataValuesWeightPotassium.add(new BarEntry(9f,Integer.parseInt(potasOct)));
//                            String  potasNov = _crypt.decrypt(response.getString("potassiumNov"), key, iv);
//                            dataValuesWeightPotassium.add(new BarEntry(10f,Integer.parseInt(potasNov)));
//                            String  potasDec = _crypt.decrypt(response.getString("potassiumDec"), key, iv);
//                            dataValuesWeightPotassium.add(new BarEntry(11f,Integer.parseInt(potasDec)));

                            //Fert Cost
                            String fertJan = _crypt.decrypt(response.getString("totalFertJan"), key, iv);
                            dataValuesCost.add(new Entry(0f,Integer.parseInt(fertJan)));
                            String fertFeb = _crypt.decrypt(response.getString("totalFertFeb"), key, iv);
                            dataValuesCost.add(new Entry(1f,Integer.parseInt(fertFeb)));
                            String fertMar = _crypt.decrypt(response.getString("totalFertMar"), key, iv);
                            dataValuesCost.add(new Entry(2f,Integer.parseInt(fertMar)));
                            String fertApr = _crypt.decrypt(response.getString("totalFertApr"), key, iv);
                            dataValuesCost.add(new Entry(3f,Integer.parseInt(fertApr)));
                            String fertMay = _crypt.decrypt(response.getString("totalFertMay"), key, iv);
                            dataValuesCost.add(new Entry(4f,Integer.parseInt(fertMay)));
                            String fertJun = _crypt.decrypt(response.getString("totalFertJun"), key, iv);
                            dataValuesCost.add(new Entry(5f,Integer.parseInt(fertJun)));
                            String fertJul = _crypt.decrypt(response.getString("totalFertJul"), key, iv);
                            dataValuesCost.add(new Entry(6f,Integer.parseInt(fertJul)));
                            String fertAug = _crypt.decrypt(response.getString("totalFertAug"), key, iv);
                            dataValuesCost.add(new Entry(7f,Integer.parseInt(fertAug)));
                            String fertSep = _crypt.decrypt(response.getString("totalFertSep"), key, iv);
                            dataValuesCost.add(new Entry(8f,Integer.parseInt(fertSep)));
//                            String fertOct = _crypt.decrypt(response.getString("totalFertOct"), key, iv);
//                            dataValuesCost.add(new Entry(9f,Integer.parseInt(fertOct)));
//                            String fertNov = _crypt.decrypt(response.getString("totalFertNov"), key, iv);
//                            dataValuesCost.add(new Entry(10f,Integer.parseInt(fertNov)));
//                            String fertDec = _crypt.decrypt(response.getString("totalFertDec"), key, iv);
//                            dataValuesCost.add(new Entry(11f,Integer.parseInt(fertDec)));

                        }catch (JSONException e){
                            e.printStackTrace();
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                        if(isVisible() && getActivity() !=null){
                            LineDataSet fertCost = new LineDataSet(dataValuesCost,getResources().getString(R.string.fertilize_cost));
                            LineData fC = new LineData();
                            fertCost.setColor(Color.rgb(14,73,65));
                            fertCost.setValueTextColor(Color.rgb(14,73,65));
                            fertCost.setValueTextSize(10f);
                            fertCost.setLineWidth(4f);
                            fC.addDataSet(fertCost);

                            BarDataSet nitrogen = new BarDataSet(dataValuesWeightNitrogen,getResources().getString(R.string.nitrogen));
                            nitrogen.setColor(Color.RED);
                            nitrogen.setValueTextColor(Color.BLACK);
                            nitrogen.setValueTextSize(10f);

                            BarDataSet phosphorus = new BarDataSet(dataValuesWeightPhosphorus,getResources().getString(R.string.phosphorus));
                            phosphorus.setColor(Color.YELLOW);
                            phosphorus.setValueTextColor(Color.BLACK);
                            phosphorus.setValueTextSize(10f);

                            BarDataSet potassium = new BarDataSet(dataValuesWeightPotassium,getResources().getString(R.string.potassium));
                            potassium.setColor(Color.BLUE);
                            potassium.setValueTextColor(Color.BLACK);
                            potassium.setValueTextSize(10f);

                            BarData barData = new BarData(nitrogen,phosphorus,potassium);
                            barData.setBarWidth(0.29f);
                            float groupSpace = 0.02f;
                            float barSpace = 0f;
                            //make this Bardata object grouped
                            barData.groupBars(0f,groupSpace,barSpace);

                            //The labels that should be drawn on the XAxis
                            final String[] treatmentMonths = new String[]{
                                    getResources().getString(R.string.jan),getResources().getString(R.string.feb),getResources().getString(R.string.mar),
                                    getResources().getString(R.string.apr), getResources().getString(R.string.may),
                                    getResources().getString(R.string.jun), getResources().getString(R.string.jul), getResources().getString(R.string.aug),
                                    getResources().getString(R.string.sep)
                            };
                            ValueFormatter formatter = new ValueFormatter() {
                                @Override
                                public String getAxisLabel(float value, AxisBase axis) {
                                    return treatmentMonths[(int) value];
                                }
                            };

                            XAxis xAxis = chart.getXAxis();
                            //xAxis configuration
                            xAxis.setGranularity(0.5f); // minimum axis-step (interval) is 1
                            xAxis.setValueFormatter(formatter);
                            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                            xAxis.setLabelRotationAngle(-49);
                            xAxis.setTextSize(12f);

                            //Description config
                            Description description = new Description();
                            description.setText(getResources().getString(R.string.year) + " " + spinnerValue);
                            description.setTextColor(Color.BLUE);
                            description.setTextSize(13f);
                            if (getScreenDimension() <5)
                                legend.setTextSize(7.9f);

                            if (getScreenDimension() >=5 && getScreenDimension() <7) {
                                description.setPosition(240, 35);
                                legend.setTextSize(9.7f);
                            }
                            if (getScreenDimension()>=7) {
                                description.setPosition(140, 20);
                                legend.setTextSize(14f);
                            }
                            chart.setDescription(description);

                            CombinedData data = new CombinedData();
                            data.setData(fC);//draw line for fertilize cost
                            data.setData(barData);//draw bars for NPK
                            chart.setData(data);
                            chart.invalidate();
                        }
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    Toast.makeText(getActivity(), R.string.data_fail, Toast.LENGTH_SHORT).show();

                }
            });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    private int getIndex(Spinner spinner, String myString){

        int index = 0;

        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).equals(myString)){
                index = i;
            }
        }
        return index;
    }
    private double getScreenDimension(){
        DisplayMetrics dm = new DisplayMetrics();
        if (isVisible() && getActivity() != null)
            Objects.requireNonNull(getActivity()).getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int dens = dm.densityDpi;
        double wi = (double) width / (double) dens;
        double hi = (double) height / (double) dens;
        double x = Math.pow(wi, 2);
        double y = Math.pow(hi, 2);
        //String screenInformation = String.format("%.2f", screenInches);

        return Math.sqrt(x + y);

    }
    private void drawBarChart(List<String> dataChart, int yearNow){
        ManagerNetwork.treatmentNPKTotal(email,cropId,yearNow,null,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                super.onSuccess(statusCode, headers, response);

                List<BarEntry> entriesNitro = new ArrayList<>();
                List<BarEntry> entriesPhos  = new ArrayList<>();
                List<BarEntry> entriesPotas = new ArrayList<>();
                List<BarEntry> entriesCost  = new ArrayList<>();

                if (response.length() == 0) {
                    Toast.makeText(getActivity(),"Null",Toast.LENGTH_LONG).show();
                } else {
                    for(int i=0;i<response.length();i++){
                        JSONObject jsonData = null;
                        try{
                            jsonData = response.getJSONObject(i);
                            CryptLib _crypt = new CryptLib();
                            String key = CryptLib.SHA256(Constants.KEY_NODE_TO_ANDROID, 32); //32 bytes = 256 bit
                            String iv = Constants.IV_NODE_TO_ANDROID; //16 bytes = 128 bit

                            String totalCost = _crypt.decrypt(jsonData.getString("totalCostYear"), key, iv);
                            String totalNitroWeight = _crypt.decrypt(jsonData.getString("totalNitroYear"), key, iv);
                            String totalPhosWeight = _crypt.decrypt(jsonData.getString("totalPhosYear"), key, iv);
                            String totalPotasWeight = _crypt.decrypt(jsonData.getString("totalPotasYear"), key, iv);

                             entriesCost.add(new BarEntry(i,Integer.parseInt(totalCost)));
                            entriesNitro.add(new BarEntry(i,Integer.parseInt(totalNitroWeight)));
                             entriesPhos.add(new BarEntry(i,Integer.parseInt(totalPhosWeight)));
                            entriesPotas.add(new BarEntry(i,Integer.parseInt(totalPotasWeight)));

                        }catch (JSONException e){
                            e.printStackTrace();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                   //barChart.setAutoScaleMinMaxEnabled(true);
                    Legend legendBarChart = barChart.getLegend();

                    //legendBarChart configuration

                    if(getScreenDimension()>=7)
                        legendBarChart.setTextSize(14f);
                    else
                    if(getScreenDimension()<5)
                        legendBarChart.setTextSize(7.9f);
                    else
                        legendBarChart.setTextSize(9.4f);

                    XAxis xAxis = barChart.getXAxis();
                    //xAxis configuration
                    xAxis.setCenterAxisLabels(true);
                    xAxis.setAxisMinimum(0f);
                    xAxis.setAxisMaximum(dataChart.size());
                    xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setLabelRotationAngle(0);
                    xAxis.setTextSize(13f);
                    xAxis.setDrawLabels(true);
                    xAxis.setValueFormatter(new IndexAxisValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            if (value >= 0) {
                                if (value <= dataChart.size() - 1)
                                    return dataChart.get((int) value);
                                return "";
                            }
                            return "";
                        }
                    });
                    if (isVisible() && getActivity() != null) {
                        Description descriptionBarChart = new Description();
                        //Description configuration
                        descriptionBarChart.setText(getResources().getString(R.string.per_year));
                        descriptionBarChart.setTextColor(Color.BLUE);
                        descriptionBarChart.setTextSize(13f);
                        if (getScreenDimension()<7 ) descriptionBarChart.setPosition(240, 35);
                        if (getScreenDimension()>=7) descriptionBarChart.setPosition(140, 20);
                        barChart.setDescription(descriptionBarChart);

                        BarDataSet cost = new BarDataSet(entriesCost, getResources().getString(R.string.fertilize_cost));
                        BarDataSet nitro = new BarDataSet(entriesNitro, getResources().getString(R.string.nitrogen));
                        BarDataSet phos = new BarDataSet(entriesPhos, getResources().getString(R.string.phosphorus));
                        BarDataSet potas = new BarDataSet(entriesPotas, getResources().getString(R.string.potassium));

                        cost.setColor(Color.rgb(14,73,65));
                        nitro.setColor(Color.RED);
                        phos.setColor(Color.YELLOW);
                        potas.setColor(Color.BLUE);

                        cost.setValueTextSize(10f);
                        nitro.setValueTextSize(10f);
                        phos.setValueTextSize(10f);
                        potas.setValueTextSize(10f);

                        float groupSpace = 0.1f;
                        float barSpace = 0f; // x2 dataset
                        float barWidth = 0.21f; // x2 dataset
                      //  barChart.setFitBars(true);

                        BarData data = new BarData(cost,nitro,phos,potas);
                        data.setBarWidth(barWidth); // set the width of each bar
                        barChart.setData(data);
                        barChart.groupBars(0.05f, groupSpace, barSpace); // perform the "explicit" grouping
                        barChart.invalidate(); // refresh
                    }
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(getActivity(), R.string.data_fail, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
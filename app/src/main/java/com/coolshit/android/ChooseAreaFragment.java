package com.coolshit.android;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolshit.android.db.City;
import com.coolshit.android.db.County;
import com.coolshit.android.db.Province;
import com.coolshit.android.util.HttpUtil;
import com.coolshit.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        //初始化ArrayAdapter
        listView.setAdapter(adapter);
        //并將他設置為ListViewd的適配器
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    //定義/修改選中的省份
                    queryCities();
                    //調用queryCity方法加載City數據
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    //定義/修改選中的城市
                    queryCounties();
                    //調用queryCounties方法加載Counties數據
                } else if (currentLevel == LEVEL_COUNTY) {
                    String weatherId = countyList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity) {
                        //定義天氣id
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        //把天氣id傳到intent
                        startActivity(intent);
                        //調到WeatherActivity頁面
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }

                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                    //如果在County點擊返回按鈕， 就重新加載City的數據
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                    //如果在City點擊返回按鈕， 就重新加載Province的數據
                }
            }
        });
        queryProvinces();
        //開始加載省級數據
    }

    //查詢全國所有的省， 優先從數據庫查詢， 如沒有再去服務器查詢
    private void queryProvinces() {
        titleText.setText("中国");
        //設置標題為中國， 因爲現在還沒有選中城市
        backButton.setVisibility(View.GONE);
        //將返回按鈕隱藏， 因爲這是最上級目錄
        provinceList = DataSupport.findAll(Province.class);
        //查找所有的數據， 並傳入provinceList省級列表
        if (provinceList.size() > 0) {
            //如果列表内有數據
            dataList.clear();
            //清理目前的數據列表
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
                //把省級列表内所有的Name鍵對應的值傳入數據列表dataList
            }
            adapter.notifyDataSetChanged();
            //如果數據改變， 刷新適配器的内容
            listView.setSelection(0);
            //顯示所有的數據（從第0個開始）
            currentLevel = LEVEL_PROVINCE;
            //把等級設置為 省級
        } else {
            //如果數據庫中沒有數據的情況
            String address = "http://guolin.tech/api/china";
            //將字符型地址設值
            queryFromServer(address, "province");
            //調用quetyFromServer方法， 以獲取服務器上的數據
        }
    }

    //查詢選中省内所有的市， 有限數據庫查詢
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        //設置標題為目前選擇省份的名字
        backButton.setVisibility(View.VISIBLE);
        //返回鍵顯示
        cityList = DataSupport.where("provinceid = ?",
                String.valueOf(selectedProvince.getId())).find(City.class);
        //查找City數據表裏provinceId等於當前選擇省份内的數據
        if (cityList.size() > 0) {
            //如果有數據
            dataList.clear();
            //清除dataList
            for (City city : cityList) {
                dataList.add(city.getCityName());
                //把所有城市名寫入到dataList
            }
            adapter.notifyDataSetChanged();
            //刷新適配器
            listView.setSelection(0);
            //顯示dataList内所有内容， （從0開始）
            currentLevel = LEVEL_CITY;
            //將等級設爲City
        } else {
            //如果數據庫中沒有數據（查找服務器）
            int provinceCode = selectedProvince.getProvinceCode();
            //設置省級代碼為（當前選擇的省+調用方法）
            String address = "http://guolin.tech/api/china/" + provinceCode;
            //設置地址為（地址+省級代碼）
            queryFromServer(address, "city");
            //調用queryFromServer方法， 並傳入參數（地址和type）
        }
    }

    //查詢選中市内所有的縣， 優先數據庫
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        //設置標題為當前選擇城市的名字
        backButton.setVisibility(View.VISIBLE);
        //顯示返回按鍵
        countyList = DataSupport.where("cityid = ?",
                String.valueOf(selectedCity.getId())).find(County.class);
        //查找County數據表内所有當前選擇城市的縣， 並傳入countyList
        if (countyList.size() > 0) {
            //如果數據庫中有數據
            dataList.clear();
            //清空dataList
            for (County county : countyList) {
                dataList.add(county.getCountyName());
                //將所有的縣名傳入dataList
            }
            adapter.notifyDataSetChanged();
            //刷新適配器
            listView.setSelection(0);
            //顯示listView的所有内容（從0開始）
            currentLevel = LEVEL_COUNTY;
            //設置等級為County
        } else {
            //如果本地沒有數據的話
            int provinceCode = selectedProvince.getProvinceCode();
            //設置省份代碼為當前選擇的省份
            int cityCode = selectedCity.getCityCode();
            //設置城市代碼
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            //設置地址
            queryFromServer(address, "county");
            //調用queryFromServer方法查找服務器數據
        }
    }

    //根據傳入的地址和類型從服務器上查詢省市縣數據
    private void queryFromServer(String address, final String type) {
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            //發出新的服務器請求
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //響應的數據會返回到onResponse中
                String responseText = response.body().string();
                boolean result = false;

                //調用handleXXXXResponse方法獲取數據並得到成功失敗
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    //如果獲取數據成功
                    // 通过runOnUiThread()方法回到主线程处理逻辑
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            //關閉加載時的提示框， 並重新調用獲取數據方法
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // 通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        //關閉對話框
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                        //創建Toast通知用戶加載失敗
                    }
                });
            }
        });
    }

    //關閉進度對話框
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}

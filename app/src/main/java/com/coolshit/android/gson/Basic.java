package com.coolshit.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by HP on 2018/2/11.
 */

public class Basic {

    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update {

        @SerializedName("loc")
        public String updateTime;
    }
}

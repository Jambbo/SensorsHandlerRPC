package com.example.datastoremicroservice.config;

import com.example.datastoremicroservice.model.MeasurementType;

public class RedisSchema {

    //set
    public static String sensorKeys(){
        return KeyHelper.getKey("sensors");
    }

    //hash with summary types
    //app:sensors:1:voltage
    public static String summaryKey(
            long sensorId,
            MeasurementType measurementType
    ){
        return KeyHelper.getKey("sensors:"+sensorId+":"+measurementType.name().toLowerCase());
    }

}

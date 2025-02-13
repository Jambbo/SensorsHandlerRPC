package com.example.datageneratormicroservice.service;

import com.example.datageneratormicroservice.model.Data;
import com.example.datageneratormicroservice.model.test.DataTestOptions;
import com.example.grpccommon.DataServerGrpc;
import com.example.grpccommon.GRPCData;
import com.example.grpccommon.MeasurementType;
import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TestDataServiceImpl implements TestDataService{
    private final GRPCDataService grpcDataService;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Value("${push.batch-size}")
    private int batchSize;

    @Override
    public void sendMessages(DataTestOptions dataTestOptions) {
        if(dataTestOptions.getMeasurementTypes().length>0){
        List<Data> dataList = new ArrayList<>();
            executorService.scheduleAtFixedRate(
                    ()->{
                        Data data = new Data();
                        data.setSensorId(
                                (long)getRandomNumber(1,15)
                        );
                        data.setMeasurement(
                                getRandomNumber(1,15)
                        );
                        data.setMeasurementType(
                                getRandomMeasurementType(
                                        dataTestOptions.getMeasurementTypes()
                                )
                        );
                        data.setTimestamp(
                                LocalDateTime.now()
                        );
                        dataList.add(data);
                        if(dataList.size()==batchSize){
                            grpcDataService.send(dataList);
                            dataList.clear();
                        }
                    },
                    0,
                    dataTestOptions.getDelayInSeconds(),
                    TimeUnit.SECONDS
            );

        }
    }

    private double getRandomNumber(int min, int max){
        return (Math.random()*(max-min))+min;
    }

    private Data.MeasurementType getRandomMeasurementType(Data.MeasurementType[] measurementTypes){
        int index = (int)(Math.random()* measurementTypes.length);
        return measurementTypes[index];
    }

}

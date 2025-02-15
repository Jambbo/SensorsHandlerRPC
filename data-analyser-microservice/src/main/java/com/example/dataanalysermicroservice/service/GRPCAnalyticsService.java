package com.example.dataanalysermicroservice.service;

import com.example.dataanalysermicroservice.model.Data;
import com.example.grpccommon.AnalyticsServerGrpc;
import com.example.grpccommon.GRPCAnalyticsRequest;
import com.example.grpccommon.GRPCData;
import com.example.grpccommon.MeasurementType;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class GRPCAnalyticsService extends AnalyticsServerGrpc.AnalyticsServerImplBase {

    private final DataService dataService;

    @Override
    public void askForData(GRPCAnalyticsRequest request, StreamObserver<GRPCData> responseObserver) {
        List<Data> data = dataService.getWithBatch(request.getBatchSize());

        if (data.isEmpty()) {
            log.warn("No data to send for batch size: {}", request.getBatchSize());
        }

        for (Data d : data) {
            GRPCData dataRequest = buildGRPCData(d);
            responseObserver.onNext(dataRequest);
        }

        log.info("Batch was sent.");
        responseObserver.onCompleted();
    }

    private GRPCData buildGRPCData(Data data) {
        return GRPCData.newBuilder()
                .setSensorId(data.getSensorId())
                .setTimestamp(convertToTimestamp(data.getTimestamp()))
                .setMeasurement(data.getMeasurement())
                .setMeasurementType(MeasurementType.valueOf(data.getMeasurementType().name()))
                .build();
    }

    private Timestamp convertToTimestamp(LocalDateTime dateTime) {
        return Timestamp.newBuilder()
                .setSeconds(dateTime.toEpochSecond(ZoneOffset.UTC))
                .build();
    }
}

package com.example.dataanalysermicroservice.service;

import com.example.dataanalysermicroservice.model.Data;
import com.example.grpccommon.DataServerGrpc;
import com.example.grpccommon.GRPCData;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class GRPCDataService extends DataServerGrpc.DataServerImplBase {

    private final DataService dataService;

    @Override
    public void addData(GRPCData request, StreamObserver<Empty> responseObserver) {
        Data data = new Data(request);
        dataService.handle(data);
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<GRPCData> addStreamOfData(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<GRPCData>() {
            @Override
            public void onNext(GRPCData grpcData) {
                    Data data = new Data(grpcData);
                    dataService.handle(data);
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("gRPC streaming error: {}", throwable.getMessage(), throwable);
                responseObserver.onError(Status.INTERNAL.withDescription("Streaming error occurred").withCause(throwable).asRuntimeException());
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }
}

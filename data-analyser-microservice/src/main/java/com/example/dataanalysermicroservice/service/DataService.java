package com.example.dataanalysermicroservice.service;

import com.example.dataanalysermicroservice.model.Data;

import java.util.List;

public interface DataService {

    void handle(Data data);

    List<Data> getWithBatch(long batchSize);



}

package com.example.datageneratormicroservice.service;


import com.example.datageneratormicroservice.model.Data;

import java.util.List;

public interface GRPCDataService {

    void send(Data data);

    void send(List<Data> data);

}

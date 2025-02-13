package com.example.datageneratormicroservice.web.controller;

import com.example.datageneratormicroservice.model.Data;
import com.example.datageneratormicroservice.model.test.DataTestOptions;
import com.example.datageneratormicroservice.service.GRPCDataService;
import com.example.datageneratormicroservice.service.TestDataService;
import com.example.datageneratormicroservice.web.dto.DataDto;
import com.example.datageneratormicroservice.web.dto.DataTestOptionsDto;
import com.example.datageneratormicroservice.web.mapper.DataMapper;
import com.example.datageneratormicroservice.web.mapper.DataTestOptionsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataController {

    private final GRPCDataService grpcDataService;
    private final TestDataService testDataService;

    private final DataMapper dataMapper;
    private final DataTestOptionsMapper dataTestOptionsMapper;

    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody DataDto dataDto) {
        Data data = dataMapper.toEntity(dataDto);
        grpcDataService.send(data);
        return ResponseEntity.ok().body("Sensor's data was successfully sent!");
    }

    @PostMapping("/test/send")
    public ResponseEntity<String> testSend(@RequestBody DataTestOptionsDto dataTestOptionsDto) {
        DataTestOptions dataTestOptions = dataTestOptionsMapper.toEntity(dataTestOptionsDto);
        testDataService.sendMessages(dataTestOptions);
        return ResponseEntity.ok().body("Test request was successfully processed");
    }

}

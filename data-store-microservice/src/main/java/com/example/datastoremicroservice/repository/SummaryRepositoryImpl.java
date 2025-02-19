package com.example.datastoremicroservice.repository;

import com.example.datastoremicroservice.model.Data;
import com.example.datastoremicroservice.model.MeasurementType;
import com.example.datastoremicroservice.model.Summary;
import com.example.datastoremicroservice.model.SummaryType;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public class SummaryRepositoryImpl implements SummaryRepository{
    @Override
    public Optional<Summary> findBySensorId(long sensorId, Set<MeasurementType> measurementTypes, Set<SummaryType> summaryTypes) {
        return Optional.empty();
    }

    @Override
    public void handle(Data data) {

    }
}

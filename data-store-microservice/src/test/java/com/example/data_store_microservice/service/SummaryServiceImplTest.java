package com.example.data_store_microservice.service;

import com.example.datastoremicroservice.model.MeasurementType;
import com.example.datastoremicroservice.model.Summary;
import com.example.datastoremicroservice.model.SummaryType;
import com.example.datastoremicroservice.model.exception.SensorNotFoundException;
import com.example.datastoremicroservice.repository.SummaryRepository;
import com.example.datastoremicroservice.service.SummaryServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SummaryServiceImplTest {

    @Mock
    private SummaryRepository summaryRepository;

    @InjectMocks
    private SummaryServiceImpl serviceUnderTest;

    @Test
    @DisplayName("Get summary with specific measurement and summary types")
    public void givenSpecificMeasurementAndSummaryTypes_whenGet_thenReturnsCorrectSummary() {
        //given
        Long sensorId = 1L;
        Set<MeasurementType> measurementTypes = Set.of(MeasurementType.TEMPERATURE);
        Set<SummaryType> summaryTypes = Set.of(SummaryType.AVG);
        Summary summary = new Summary();
        summary.setSensorId(sensorId);
        when(summaryRepository.findBySensorId(
                eq(sensorId),
                eq(measurementTypes),
                eq(summaryTypes)
        )).thenReturn(Optional.of(summary));

        //when
        Summary actualSummary = serviceUnderTest.get(sensorId, measurementTypes, summaryTypes);

        //then
        assertNotNull(actualSummary);
        assertEquals(summary, actualSummary);
        verify(summaryRepository).findBySensorId(
                eq(sensorId),
                eq(measurementTypes),
                eq(summaryTypes)
        );

    }

    @Test
    @DisplayName("Get summary with null measurement types uses all measurement types functionality")
    public void givenNullMeasurementTypes_whenGet_thenUsesAllMeasurementTypes() {
        //given
        Long sensorId = 1L;
        Set<SummaryType> summaryTypes = Set.of(SummaryType.MAX);
        Summary summary = new Summary();
        summary.setSensorId(sensorId);

        when(summaryRepository.findBySensorId(
                eq(sensorId),
                eq(Set.of(MeasurementType.values())),
                eq(summaryTypes)
        )).thenReturn(Optional.of(summary));
        //when
        Summary actualSummary = serviceUnderTest.get(sensorId, null, summaryTypes);

        //then
        assertNotNull(actualSummary);
        assertEquals(summary, actualSummary);
        verify(summaryRepository).findBySensorId(
                eq(sensorId),
                eq(Set.of(MeasurementType.values())),
                eq(summaryTypes)
        );
    }

    @Test
    @DisplayName("Get summary with null summary types uses all summary types functionality")
    public void givenNullSummaryTypes_whenGet_thenUsesAllSummaryTypes() {
        //given
        Long sensorId = 1L;
        Set<MeasurementType> measurementTypes = Set.of(MeasurementType.POWER, MeasurementType.TEMPERATURE);
        Summary summary = new Summary();
        summary.setSensorId(sensorId);

        when(summaryRepository.findBySensorId(
                eq(sensorId),
                eq(measurementTypes),
                eq(Set.of(SummaryType.values()))
        )).thenReturn(Optional.of(summary));
        //when
        Summary actualSummary = serviceUnderTest.get(sensorId, measurementTypes, null);

        //then
        assertNotNull(actualSummary);
        assertEquals(summary, actualSummary);
        verify(summaryRepository).findBySensorId(
                eq(sensorId),
                eq(measurementTypes),
                eq(Set.of(SummaryType.values()))
        );
    }

    @Test
    @DisplayName("Get summary throws SensorNotFoundException when no summary found")
    public void givenNonexistentSensor_whenGet_thenThrowsSensorNotFoundException() {
        //given
        Long sensorId = 1L;

        when(summaryRepository.findBySensorId(
                eq(sensorId),
                eq(Set.of(MeasurementType.values())),
                eq(Set.of(SummaryType.values()))
        )).thenReturn(Optional.empty());
        //when
        assertThrows(
                SensorNotFoundException.class,
                () -> serviceUnderTest.get(sensorId, null, null)
        );
        //then
    }

}

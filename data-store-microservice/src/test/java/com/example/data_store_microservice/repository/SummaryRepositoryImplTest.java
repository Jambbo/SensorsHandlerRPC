package com.example.data_store_microservice.repository;


import com.example.datastoremicroservice.model.Data;
import com.example.datastoremicroservice.model.MeasurementType;
import com.example.datastoremicroservice.model.Summary;
import com.example.datastoremicroservice.model.SummaryType;
import com.example.datastoremicroservice.repository.SummaryRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SummaryRepositoryImplTest {

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    @InjectMocks
    private SummaryRepositoryImpl summaryRepository;

    @BeforeEach
    public void setUp(){
        when(jedisPool.getResource()).thenReturn(jedis);
    }

    @Test
    @DisplayName("Test find by id functionality")
    public void givenExistingSensor_whenFindById_thenReturnSummary(){
        //given
        long sensorId = 4;
        Set<MeasurementType> measurementTypes = Set.of(MeasurementType.TEMPERATURE);
        Set<SummaryType> summaryTypes = Set.of(SummaryType.MIN, SummaryType.MAX);
        String redisKey = "app:sensors:4:temperature";
        when(jedis.sismember("app:sensors",String.valueOf(sensorId))).thenReturn(true);
        when(jedis.hgetAll(redisKey)).thenReturn(Map.of("min","2.4","max","26.6","counter","14"));
        //when
        Optional<Summary> obtainedSummary = summaryRepository.findBySensorId(sensorId, measurementTypes, summaryTypes);
        //then
        assertThat(obtainedSummary).isNotNull();
        Summary summary = obtainedSummary.get();
        assertThat(summary.getSensorId()).isEqualTo(sensorId);
        Summary.SummaryEntry summaryMinEntry = summary.getValues().get(MeasurementType.TEMPERATURE).stream()
                .filter(entry -> entry.getType() == SummaryType.MIN)
                .findFirst()
                .orElseThrow();
        assertEquals(2.4,summaryMinEntry.getValue());
        assertEquals(14,summaryMinEntry.getCounter());
    }

    @Test
    @DisplayName("Test find by id with non existent sensor functionality")
    public void givenNonExistentSensor_whenFindById_thenReturnEmpty(){
        //given
        long sensorId = 5;
        when(jedis.sismember("app:sensors",String.valueOf(sensorId))).thenReturn(false);
        //when
        Optional<Summary> obtainedSummary = summaryRepository.findBySensorId(5,Set.of(),Set.of());
        //then
        assertThat(obtainedSummary).isEmpty();
    }

    @Test
    @DisplayName("Test correct update Redis functionality")
    public void givenNewData_whenHandle_thenStoreAndUpdateDataInRedis(){
        //given
        Long sensorId = 4L;
        Data data = new Data();
        data.setSensorId(sensorId);
        data.setTimestamp(LocalDateTime.now());
        data.setMeasurement(19.4);
        data.setMeasurementType(MeasurementType.TEMPERATURE);
        String summaryKey = "app:sensors:4:temperature";

        when(jedis.sismember("app:sensors",String.valueOf(sensorId))).thenReturn(false);
        when(jedis.hget(summaryKey,SummaryType.MIN.name().toLowerCase())).thenReturn("10");
        when(jedis.hget(summaryKey,SummaryType.MAX.name().toLowerCase())).thenReturn("20");
        when(jedis.hget(summaryKey,SummaryType.SUM.name().toLowerCase())).thenReturn("50");
        //when
        summaryRepository.handle(data);

        //then
        verify(jedis).sadd("app:sensors",String.valueOf(sensorId));

        verify(jedis, never()).hset(summaryKey, SummaryType.MIN.name().toLowerCase(),"10");
        verify(jedis, never()).hset(summaryKey, SummaryType.MAX.name().toLowerCase(),"20");

        verify(jedis).hincrByFloat(summaryKey,SummaryType.SUM.name().toLowerCase(),data.getMeasurement());
        verify(jedis).hincrBy(summaryKey,"counter",1);
    }

}

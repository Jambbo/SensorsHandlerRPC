package com.example.datastoremicroservice.repository;

import com.example.datastoremicroservice.config.RedisSchema;
import com.example.datastoremicroservice.model.Data;
import com.example.datastoremicroservice.model.MeasurementType;
import com.example.datastoremicroservice.model.Summary;
import com.example.datastoremicroservice.model.SummaryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class SummaryRepositoryImpl implements SummaryRepository {

    private final JedisPool jedisPool;

    @Override
    public Optional<Summary> findBySensorId(
            long sensorId,
            Set<MeasurementType> measurementTypes,
            Set<SummaryType> summaryTypes
    ) {

        try (Jedis jedis = jedisPool.getResource()) {
            if (!jedis.sismember(
                    RedisSchema.sensorKeys(),
                    String.valueOf(sensorId)
            )) {
                return Optional.empty();
            }
            Set<MeasurementType> resolvedMeasurementTypes = measurementTypes.isEmpty()
                    ? Set.of(MeasurementType.values())
                    : measurementTypes;

            Set<SummaryType> resolvedSummaryTypes = summaryTypes.isEmpty()
                    ? Set.of(SummaryType.values())
                    : summaryTypes;
            return getSummary(
                    sensorId,
                    resolvedMeasurementTypes,
                    resolvedSummaryTypes,
                    jedis
            );
        }


    }

    private Optional<Summary> getSummary(
            long sensorId,
            Set<MeasurementType> measurementTypes,
            Set<SummaryType> summaryTypes,
            Jedis jedis
    ) {
        Summary summary = new Summary();
        summary.setSensorId(sensorId);

        for (MeasurementType mType : measurementTypes) {
            String redisKey = RedisSchema.summaryKey(sensorId, mType);
            Map<String, String> redisDataMap = jedis.hgetAll(redisKey);
            Map<String, String> redisData = new HashMap<>(redisDataMap);

            if (redisData.isEmpty()) {
                redisData.put("counter", "0"); //default counter for non-exist data
            }

            for (SummaryType sType : summaryTypes) {
                String value = redisData.get(sType.name().toLowerCase());
                String counter = redisData.get("counter");

                if (value == null) {
                    value = "0";
                }
                if (counter == null) {
                    counter = "0";
                }

                Summary.SummaryEntry entry = new Summary.SummaryEntry();
                entry.setType(sType);
                entry.setValue(Double.parseDouble(value));
                entry.setCounter(Long.parseLong(counter));
                summary.addValue(mType, entry);
            }
        }
        return Optional.of(summary);
    }

    @Override
    public void handle(Data data) {
        try (Jedis jedis = jedisPool.getResource()) {
            String sensorId = String.valueOf(data.getSensorId());
            String summaryKey = getSummaryKey(data);

            ensureSensorExists(sensorId, jedis);
            updateMinOrMaxValue(summaryKey, SummaryType.MIN, data.getMeasurement(), jedis, true);
            updateMinOrMaxValue(summaryKey, SummaryType.MAX, data.getMeasurement(), jedis, false);
            updateSumAndAvgValue(summaryKey, data.getMeasurement(), jedis);
        }
    }

    private static void ensureSensorExists(String sensorId, Jedis jedis) {
        if (!jedis.sismember(
                RedisSchema.sensorKeys(),
                sensorId
        )) {
            jedis.sadd(RedisSchema.sensorKeys(), sensorId);
        }
    }

    private void updateMinOrMaxValue(String key, SummaryType type, Double measurement, Jedis jedis, boolean isMin) {
        String field = type.name().toLowerCase();
        String currentValue = jedis.hget(key, field);

        if (currentValue == null || (
                isMin ? measurement < Double.parseDouble(currentValue)
                        : measurement > Double.parseDouble(currentValue))
        ) {
            jedis.hset(key, field, String.valueOf(measurement));
        }
    }

    private void updateSumAndAvgValue(String key, Double measurement, Jedis jedis) {
        updateSumValue(key, measurement, jedis);

        long counter = jedis.hincrBy(key, "counter", 1);
        double sum = Double.parseDouble(jedis.hget(key, SummaryType.SUM.name().toLowerCase()));
        jedis.hset(key, SummaryType.AVG.name().toLowerCase(), String.valueOf(sum / counter));
    }

    private void updateSumValue(String key, Double measurement, Jedis jedis) {
        jedis.hincrByFloat(key, SummaryType.SUM.name().toLowerCase(), measurement);
    }

    private String getSummaryKey(Data data) {
        return RedisSchema.summaryKey(
                data.getSensorId(),
                data.getMeasurementType()
        );
    }
}

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
            for (SummaryType sType : summaryTypes) {
                Summary.SummaryEntry entry = new Summary.SummaryEntry();
                entry.setType(sType);
                String value = jedis.hget(
                        RedisSchema.summaryKey(sensorId, mType),
                        sType.name().toLowerCase()
                );
                if (value != null) {
                    entry.setValue(Double.parseDouble(value));
                }
                String counter = jedis.hget(
                        RedisSchema.summaryKey(sensorId, mType),
                        "counter"
                );
                if (counter != null) {
                    entry.setCounter(Long.parseLong(counter));
                }
                summary.addValue(mType, entry);
            }
        }
        return Optional.of(summary);
    }

    @Override
    public void handle(Data data) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (!jedis.sismember(
                    RedisSchema.sensorKeys(),
                    String.valueOf(data.getSensorId())
            )) {
                jedis.sadd(
                        RedisSchema.sensorKeys(),
                        String.valueOf(data.getSensorId())
                );
            }
            updateMinValue(data, jedis);
            updateMaxValue(data, jedis);
            updateSumAndAvgValue(data, jedis);
        }
    }

    private void updateMinValue(Data data, Jedis jedis) {
        String key = getSummaryKey(data);
        String field = SummaryType.MIN.name().toLowerCase();
        String value = jedis.hget(
                key,
                field
        );
        if (value == null || data.getMeasurement() < Double.parseDouble(value)) {
            jedis.hset(
                    key,
                    field,
                    String.valueOf(data.getMeasurement())
            );
        }
    }

    private void updateMaxValue(Data data, Jedis jedis) {
        String key = getSummaryKey(data);
        String field = SummaryType.MAX.name().toLowerCase();
        String value = jedis.hget(
                key,
                field
        );
        if (value == null || data.getMeasurement() > Double.parseDouble(value)) {
            jedis.hset(
                    key,
                    field,
                    String.valueOf(data.getMeasurement())
            );
        }
    }

    private void updateSumAndAvgValue(Data data, Jedis jedis) {
        updateSumValue(data, jedis);
        String key = getSummaryKey(data);
        String counter = jedis.hget(
                key,
                "counter"
        );
        if (counter == null) {
            counter = String.valueOf(
                    jedis.hset(
                            key,
                            "counter",
                            String.valueOf(1)
                    )
            );
        } else {
            counter = String.valueOf(
                    jedis.hincrBy(
                            key,
                            "counter",
                            1
                    )
            );
        }
        String sum = jedis.hget(
                key,
                SummaryType.SUM.name().toLowerCase()
        );
        jedis.hset(
                key,
                SummaryType.AVG.name().toLowerCase(),
                String.valueOf(
                        Double.parseDouble(sum) / Double.parseDouble(counter)
                )
        );

    }

    private void updateSumValue(Data data, Jedis jedis) {
        String key = getSummaryKey(data);
        String field = SummaryType.SUM.name().toLowerCase();
        String value = jedis.hget(
                key,
                field
        );
        if (value == null) {
            jedis.hset(
                    key,
                    field,
                    String.valueOf(data.getMeasurement())
            );
        } else {
            jedis.hincrByFloat(
                    key,
                    field,
                    data.getMeasurement()
            );
        }
    }


    private String getSummaryKey(Data data) {
        return RedisSchema.summaryKey(
                data.getSensorId(),
                data.getMeasurementType()
        );
    }
}

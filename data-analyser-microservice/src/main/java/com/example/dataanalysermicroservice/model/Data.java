package com.example.dataanalysermicroservice.model;

import com.example.grpccommon.GRPCData;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "data")
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class Data {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Long sensorId;
    LocalDateTime timestamp;
    double measurement;

    @Column(name = "type")
    @Enumerated(value = EnumType.STRING)
    MeasurementType measurementType;


    public enum MeasurementType {

        TEMPERATURE,
        VOLTAGE,
        POWER

    }

    public Data(GRPCData data) {
        this.id = data.getId();
        this.sensorId = data.getSensorId();
        this.timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(
                            data.getTimestamp().getSeconds(),
                            data.getTimestamp().getNanos()
                        ),
                ZoneId.systemDefault()
        );
        this.measurement = data.getMeasurement();
        this.measurementType = MeasurementType.valueOf(data.getMeasurementType().name());
    }


}

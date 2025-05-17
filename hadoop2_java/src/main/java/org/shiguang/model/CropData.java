package org.shiguang.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CropData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cropType;
    private String region;
    private Double area; // in hectares
    private Double yield; // in tons per hectare
    private LocalDate harvestDate;
    private Double temperature; // average temperature
    private Double rainfall; // in mm
    private Double soilMoisture; // percentage
    private String soilType;
}
package com.microfocus.example.contracts.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Prescription entity
 * WARNING: Deliberately vulnerable - IDOR and insufficient authorization checks
 */
public class Prescription implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long customerId;
    private String prescriptionNumber;
    private String medicationName;
    private String dosage;
    private Integer refillsRemaining;
    private String doctorName;
    private String doctorPhone;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String status; // ACTIVE, FILLED, EXPIRED, CANCELLED
    private LocalDateTime createdAt;
    private String instructions;
    
    public Prescription() {}
    
    public Prescription(Long id, Long customerId, String prescriptionNumber, 
                       String medicationName, String dosage, Integer refillsRemaining) {
        this.id = id;
        this.customerId = customerId;
        this.prescriptionNumber = prescriptionNumber;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.refillsRemaining = refillsRemaining;
        this.createdAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    
    public String getPrescriptionNumber() { return prescriptionNumber; }
    public void setPrescriptionNumber(String prescriptionNumber) { 
        this.prescriptionNumber = prescriptionNumber; 
    }
    
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    
    public Integer getRefillsRemaining() { return refillsRemaining; }
    public void setRefillsRemaining(Integer refillsRemaining) { 
        this.refillsRemaining = refillsRemaining; 
    }
    
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    
    public String getDoctorPhone() { return doctorPhone; }
    public void setDoctorPhone(String doctorPhone) { this.doctorPhone = doctorPhone; }
    
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}

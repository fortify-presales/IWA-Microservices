package com.microfocus.example.prescriptions.repository;

import com.microfocus.example.contracts.model.Prescription;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Prescription Repository
 * WARNING: No authorization checks - IDOR vulnerability
 */
@Repository
public class PrescriptionRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public PrescriptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS prescriptions (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
            "customer_id BIGINT, " +
            "prescription_number VARCHAR(50) UNIQUE, " +
            "medication_name VARCHAR(255), " +
            "dosage VARCHAR(100), " +
            "refills_remaining INT, " +
            "doctor_name VARCHAR(255), " +
            "doctor_phone VARCHAR(20), " +
            "issue_date DATE, " +
            "expiry_date DATE, " +
            "status VARCHAR(50), " +
            "instructions TEXT, " +
            "created_at TIMESTAMP)"
        );
        
        // Insert sample prescriptions
        String insertSql = "INSERT INTO prescriptions (customer_id, prescription_number, medication_name, " +
                          "dosage, refills_remaining, doctor_name, doctor_phone, issue_date, expiry_date, " +
                          "status, instructions, created_at) VALUES " +
                          "(1, 'RX-001-2024', 'Lisinopril', '10mg once daily', 3, 'Dr. Smith', '555-1001', " +
                          "CURRENT_DATE, DATEADD('MONTH', 6, CURRENT_DATE), 'ACTIVE', 'Take with food', CURRENT_TIMESTAMP), " +
                          "(2, 'RX-002-2024', 'Amoxicillin', '250mg twice daily', 0, 'Dr. Johnson', '555-1002', " +
                          "CURRENT_DATE, DATEADD('MONTH', 3, CURRENT_DATE), 'ACTIVE', 'Complete full course', CURRENT_TIMESTAMP), " +
                          "(3, 'RX-003-2024', 'Metformin', '500mg twice daily', 5, 'Dr. Williams', '555-1003', " +
                          "CURRENT_DATE, DATEADD('YEAR', 1, CURRENT_DATE), 'ACTIVE', 'Monitor blood sugar', CURRENT_TIMESTAMP)";
        
        try {
            jdbcTemplate.execute(insertSql);
        } catch (Exception e) {
            // Data already exists
        }
    }
    
    /**
     * VULNERABILITY: IDOR - No authorization check
     * Anyone can access any prescription by ID
     */
    public Prescription findById(Long id) {
        String sql = "SELECT * FROM prescriptions WHERE id = ?";
        List<Prescription> prescriptions = jdbcTemplate.query(sql, new PrescriptionRowMapper(), id);
        return prescriptions.isEmpty() ? null : prescriptions.get(0);
    }
    
    /**
     * VULNERABILITY: IDOR - No check if user has access
     */
    public List<Prescription> findByCustomerId(Long customerId) {
        String sql = "SELECT * FROM prescriptions WHERE customer_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new PrescriptionRowMapper(), customerId);
    }
    
    public List<Prescription> findAll() {
        String sql = "SELECT * FROM prescriptions ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new PrescriptionRowMapper());
    }
    
    /**
     * VULNERABILITY: IDOR - No check if user owns this prescription
     */
    public void updateRefills(Long prescriptionId, int refillsRemaining) {
        String sql = "UPDATE prescriptions SET refills_remaining = ? WHERE id = ?";
        jdbcTemplate.update(sql, refillsRemaining, prescriptionId);
    }
    
    /**
     * VULNERABILITY: IDOR - Anyone can modify prescription status
     */
    public void updateStatus(Long prescriptionId, String status) {
        String sql = "UPDATE prescriptions SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, prescriptionId);
    }
    
    private static class PrescriptionRowMapper implements RowMapper<Prescription> {
        @Override
        public Prescription mapRow(ResultSet rs, int rowNum) throws SQLException {
            Prescription prescription = new Prescription();
            prescription.setId(rs.getLong("id"));
            prescription.setCustomerId(rs.getLong("customer_id"));
            prescription.setPrescriptionNumber(rs.getString("prescription_number"));
            prescription.setMedicationName(rs.getString("medication_name"));
            prescription.setDosage(rs.getString("dosage"));
            prescription.setRefillsRemaining(rs.getInt("refills_remaining"));
            prescription.setDoctorName(rs.getString("doctor_name"));
            prescription.setDoctorPhone(rs.getString("doctor_phone"));
            prescription.setStatus(rs.getString("status"));
            prescription.setInstructions(rs.getString("instructions"));
            
            java.sql.Date issueDate = rs.getDate("issue_date");
            if (issueDate != null) {
                prescription.setIssueDate(issueDate.toLocalDate());
            }
            
            java.sql.Date expiryDate = rs.getDate("expiry_date");
            if (expiryDate != null) {
                prescription.setExpiryDate(expiryDate.toLocalDate());
            }
            
            java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
            if (createdTs != null) {
                prescription.setCreatedAt(createdTs.toLocalDateTime());
            }
            
            return prescription;
        }
    }
}

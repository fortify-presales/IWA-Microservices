package com.microfocus.example.prescriptions.service;

import com.microfocus.example.contracts.model.Prescription;
import com.microfocus.example.prescriptions.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import java.io.StringReader;


/**
 * Prescription Service
 */
@Service
public class PrescriptionService {
    
    private final PrescriptionRepository prescriptionRepository;
    
    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }
    
    public Prescription getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id);
    }
    
    public List<Prescription> getPrescriptionsByCustomerId(Long customerId) {
        return prescriptionRepository.findByCustomerId(customerId);
    }
    
    public List<Prescription> getAllPrescriptions() {
        return prescriptionRepository.findAll();
    }
    
    public void processRefill(Long prescriptionId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId);
        if (prescription != null && prescription.getRefillsRemaining() > 0) {
            prescriptionRepository.updateRefills(prescriptionId, 
                prescription.getRefillsRemaining() - 1);
        }
    }
    
    public void updatePrescriptionStatus(Long prescriptionId, String status) {
        prescriptionRepository.updateStatus(prescriptionId, status);
    }

    /**
     * VULNERABILITY: XML External Entity (XXE)
     * This method parses XML from user input using a DocumentBuilderFactory
     * without disabling external entity resolution or secure processing. This
     * can allow XXE attacks when untrusted XML is parsed.
     *
     * SECURE ALTERNATIVES:
     * - Disable external entities and enable secure processing on the factory.
     * - Use a library that disables XXE by default.
     */
    public String parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Intentionally vulnerable: not disabling external entities / secure processing
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        // Return the root element name for demonstration
        return doc.getDocumentElement().getNodeName();
    }
}

package com.microfocus.example.inventory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Inventory Service
 * WARNING: Contains XXE vulnerability - unsafe XML parsing
 */
@Service
public class InventoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    
    private final Map<Long, Integer> inventory = new HashMap<>();
    
    public InventoryService() {
        // Initialize some inventory
        inventory.put(1L, 100);
        inventory.put(2L, 50);
        inventory.put(3L, 150);
        inventory.put(4L, 75);
        inventory.put(5L, 200);
    }
    
    public Integer getStock(Long productId) {
        return inventory.getOrDefault(productId, 0);
    }
    
    public void updateStock(Long productId, Integer quantity) {
        inventory.put(productId, quantity);
    }
    
    public void adjustStock(Long productId, Integer adjustment) {
        Integer current = getStock(productId);
        inventory.put(productId, current + adjustment);
    }
    
    /**
     * VULNERABILITY: XML External Entity (XXE) Attack
     * This method parses XML without disabling external entities
     * Allows attackers to read local files, perform SSRF, or cause DoS
     * 
     * SECURE XML PARSING (uncomment the features below):
     * 
     * DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
     * // Disable external entities
     * factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
     * factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
     * // Disable DOCTYPE declarations entirely
     * factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
     * // Disable XInclude processing
     * factory.setXIncludeAware(false);
     * // Disable expansion of entity references
     * factory.setExpandEntityReferences(false);
     * 
     * ALTERNATIVE: Use JSON instead of XML for data interchange
     */
    public Map<String, Object> importInventoryFromXml(String xmlData) {
        try {
            logger.info("Importing inventory from XML");
            
            // VULNERABILITY: DocumentBuilderFactory with XXE enabled
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Intentionally NOT disabling external entities:
            // factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            // factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            // factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlData.getBytes()));
            
            int itemsProcessed = 0;
            NodeList items = doc.getElementsByTagName("item");
            
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                
                String productIdStr = item.getElementsByTagName("productId").item(0).getTextContent();
                String quantityStr = item.getElementsByTagName("quantity").item(0).getTextContent();
                
                Long productId = Long.parseLong(productIdStr);
                Integer quantity = Integer.parseInt(quantityStr);
                
                updateStock(productId, quantity);
                itemsProcessed++;
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("itemsProcessed", itemsProcessed);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing XML", e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", e.getMessage());
            return result;
        }
    }
    
    /**
     * Export inventory as XML
     */
    public String exportInventoryToXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<inventory>\n");
        
        for (Map.Entry<Long, Integer> entry : inventory.entrySet()) {
            xml.append("  <item>\n");
            xml.append("    <productId>").append(entry.getKey()).append("</productId>\n");
            xml.append("    <quantity>").append(entry.getValue()).append("</quantity>\n");
            xml.append("  </item>\n");
        }
        
        xml.append("</inventory>");
        return xml.toString();
    }
    
    public Map<Long, Integer> getAllInventory() {
        return new HashMap<>(inventory);
    }
}

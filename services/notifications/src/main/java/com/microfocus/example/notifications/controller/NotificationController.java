package com.microfocus.example.notifications.controller;

import com.microfocus.example.notifications.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.Map;

/**
 * Notifications REST Controller
 * WARNING: Contains intentional command injection vulnerabilities
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    private final NotificationService notificationService;
    
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * VULNERABILITY: Command Injection via email parameters
     */
    @PostMapping("/email")
    @Operation(summary = "Send email notification", description = "Sends an email; demo contains command injection vulnerabilities")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Email sent"), @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<Map<String, String>> sendEmail(@RequestBody Map<String, String> request) {
        String to = request.get("to");
        String subject = request.get("subject");
        String body = request.get("body");
        
        logger.info("Email notification request received");
        
        Map<String, String> result = notificationService.sendEmail(to, subject, body);
        return ResponseEntity.ok(result);
    }
    
    /**
     * VULNERABILITY: Command Injection via SMS parameters
     */
    @PostMapping("/sms")
    @Operation(summary = "Send SMS notification", description = "Sends an SMS message; demo contains command injection vulnerabilities")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "SMS sent"), @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<Map<String, String>> sendSms(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String message = request.get("message");
        
        logger.info("SMS notification request received");
        
        Map<String, String> result = notificationService.sendSms(phoneNumber, message);
        return ResponseEntity.ok(result);
    }
    
    /**
     * VULNERABILITY: Command Injection and Path Traversal via report generation
     */
    @PostMapping("/report")
    @Operation(summary = "Generate report", description = "Generates a report; demo may be vulnerable to command injection and path traversal")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Report generated"), @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<Map<String, String>> generateReport(@RequestBody Map<String, String> request) {
        String reportType = request.get("reportType");
        String outputPath = request.get("outputPath");
        
        logger.info("Report generation request received");
        
        Map<String, String> result = notificationService.generateReport(reportType, outputPath);
        return ResponseEntity.ok(result);
    }
    
    /**
     * VULNERABILITY: Arbitrary command execution
     */
    @PostMapping("/execute")
    @Operation(summary = "Execute script", description = "Executes a script on the host; intentionally unsafe in this demo")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Execution result"), @ApiResponse(responseCode = "400", description = "Bad request")})
    public ResponseEntity<Map<String, String>> executeScript(@RequestBody Map<String, String> request) {
        String scriptPath = request.get("scriptPath");
        String args = request.get("args");
        
        logger.info("Script execution request received");
        
        Map<String, String> result = notificationService.executeCustomScript(scriptPath, args);
        return ResponseEntity.ok(result);
    }
}

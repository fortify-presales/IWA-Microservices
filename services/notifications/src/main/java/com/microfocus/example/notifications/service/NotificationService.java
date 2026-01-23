package com.microfocus.example.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Notification Service
 * WARNING: Contains command injection vulnerabilities
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    /**
     * VULNERABILITY: Command Injection
     * User input is directly concatenated into system command
     * 
     * SECURE ALTERNATIVES:
     * 1. Use ProcessBuilder with separated arguments (prevents shell injection):
     *    ProcessBuilder pb = new ProcessBuilder("sendmail", "-t");
     *    pb.start();
     * 
     * 2. Validate and sanitize all input:
     *    - Whitelist allowed characters
     *    - Reject special shell characters like ;, |, &, $, etc.
     *    - Use email libraries instead of system commands
     * 
     * 3. Use proper email libraries like JavaMail API instead of system commands
     */
    public Map<String, String> sendEmail(String to, String subject, String body) {
        logger.info("Sending email to: {}", to);
        
        try {
            // VULNERABILITY: Command injection via unsanitized input
            String command = "echo 'Subject: " + subject + "\\nTo: " + to + 
                           "\\n\\n" + body + "' | sendmail -t";
            
            logger.debug("Executing command: {}", command);
            
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            int exitCode = process.waitFor();
            
            Map<String, String> result = new HashMap<>();
            result.put("status", exitCode == 0 ? "sent" : "failed");
            result.put("recipient", to);
            result.put("subject", subject);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error sending email", e);
            Map<String, String> result = new HashMap<>();
            result.put("status", "error");
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * VULNERABILITY: Command Injection via SMS
     */
    public Map<String, String> sendSms(String phoneNumber, String message) {
        logger.info("Sending SMS to: {}", phoneNumber);
        
        try {
            // VULNERABILITY: Command injection via phone number and message
            String command = "curl -X POST https://api.sms-provider.com/send " +
                           "-d 'to=" + phoneNumber + "&message=" + message + "'";
            
            logger.debug("Executing SMS command: {}", command);
            
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            process.waitFor();
            
            Map<String, String> result = new HashMap<>();
            result.put("status", "sent");
            result.put("phoneNumber", phoneNumber);
            result.put("response", output.toString());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error sending SMS", e);
            Map<String, String> result = new HashMap<>();
            result.put("status", "error");
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * VULNERABILITY: Command Injection via file operations
     */
    public Map<String, String> generateReport(String reportType, String outputPath) {
        logger.info("Generating report: {} to {}", reportType, outputPath);
        
        try {
            // VULNERABILITY: Command injection and path traversal
            String command = "python3 /opt/reports/generate.py --type " + reportType + 
                           " --output " + outputPath;
            
            logger.debug("Executing report command: {}", command);
            
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            int exitCode = process.waitFor();
            
            Map<String, String> result = new HashMap<>();
            result.put("status", exitCode == 0 ? "generated" : "failed");
            result.put("reportType", reportType);
            result.put("outputPath", outputPath);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error generating report", e);
            Map<String, String> result = new HashMap<>();
            result.put("status", "error");
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * VULNERABILITY: Unsafe execution of user-provided commands
     */
    public Map<String, String> executeCustomScript(String scriptPath, String args) {
        logger.info("Executing custom script: {} with args: {}", scriptPath, args);
        
        try {
            // VULNERABILITY: Executes arbitrary scripts
            String command = scriptPath + " " + args;
            
            logger.debug("Executing: {}", command);
            
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            Map<String, String> result = new HashMap<>();
            result.put("status", exitCode == 0 ? "success" : "failed");
            result.put("exitCode", String.valueOf(exitCode));
            result.put("output", output.toString());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing script", e);
            Map<String, String> result = new HashMap<>();
            result.put("status", "error");
            result.put("error", e.getMessage());
            return result;
        }
    }
}

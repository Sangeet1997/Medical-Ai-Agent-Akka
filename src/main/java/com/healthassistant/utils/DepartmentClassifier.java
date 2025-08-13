package com.healthassistant.utils;

/**
 * Utility class for department classification and routing
 */
public class DepartmentClassifier {
    
    /**
     * Classify query to determine appropriate department
     */
    public static String classifyQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return "general-medicine";
        }
        
        String lowerQuery = query.toLowerCase();
        
        // Pharmacy keywords
        String[] pharmacyKeywords = {
            "medication", "drug", "prescription", "pill", "tablet", "capsule",
            "dosage", "dose", "medicine", "pharmaceutical", "pharmacy",
            "ibuprofen", "aspirin", "antibiotic", "painkiller", "side effects",
            "drug interaction", "overdose", "withdrawal", "generic",
            "brand name", "over the counter", "otc"
        };
        
        for (String keyword : pharmacyKeywords) {
            if (lowerQuery.contains(keyword)) {
                return "pharmacy";
            }
        }
        
        // Radiology keywords  
        String[] radiologyKeywords = {
            "x-ray", "xray", "scan", "mri", "ct", "cat scan", "ultrasound",
            "imaging", "radiology", "radiologist", "contrast", "barium",
            "mammogram", "pet scan", "bone scan", "nuclear medicine",
            "fluoroscopy", "angiogram", "fracture", "broken bone"
        };
        
        for (String keyword : radiologyKeywords) {
            if (lowerQuery.contains(keyword)) {
                return "radiology";
            }
        }
        
        // Default to general medicine for everything else
        return "general-medicine";
    }
    
    /**
     * Get department-specific context for LLM
     */
    public static String getDepartmentContext(String department) {
        switch (department) {
            case "pharmacy":
                return "You are a knowledgeable pharmacy assistant. Provide accurate information about " +
                       "medications, dosages, side effects, drug interactions, and general pharmaceutical advice. " +
                       "Always emphasize the importance of consulting with pharmacists or healthcare providers " +
                       "for medication decisions and never recommend stopping prescribed medications without " +
                       "medical supervision.";
                       
            case "radiology":
                return "You are a helpful radiology information assistant. Provide information about " +
                       "medical imaging procedures, what they detect, preparation requirements, and general " +
                       "information about X-rays, CT scans, MRIs, ultrasounds, and other imaging techniques. " +
                       "Always emphasize that only qualified radiologists and healthcare providers can " +
                       "interpret imaging results and provide medical diagnoses.";
                       
            case "general-medicine":
            default:
                return "You are a helpful medical information assistant. Provide general health information, " +
                       "explain symptoms and conditions, and offer general wellness advice. Always emphasize " +
                       "the importance of consulting with healthcare professionals for proper medical diagnosis " +
                       "and treatment, especially for serious or persistent symptoms.";
        }
    }
    
    /**
     * Get human-readable department name
     */
    public static String getDepartmentDisplayName(String department) {
        switch (department) {
            case "pharmacy":
                return "Pharmacy Department";
            case "radiology":
                return "Radiology Department";
            case "general-medicine":
                return "General Medicine Department";
            default:
                return "Unknown Department";
        }
    }
}

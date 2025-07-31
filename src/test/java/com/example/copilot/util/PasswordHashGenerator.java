package com.example.copilot.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("Hashed passwords for import.sql:");
        System.out.println("password1: " + encoder.encode("password1"));
        System.out.println("password2: " + encoder.encode("password2"));
        System.out.println("password3: " + encoder.encode("password3"));
    }
}

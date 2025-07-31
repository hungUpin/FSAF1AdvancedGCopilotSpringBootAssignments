package com.example.copilot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UserDTO {
   private Long id;

   @NotNull(message = "Name must not be null")
   @NotBlank(message = "Name is mandatory")
   private String name;

   @NotBlank(message = "Email is mandatory")
   @Email(message = "Email should be valid")
   private String email;

   @NotBlank(message = "Role is mandatory")
   @jakarta.validation.constraints.Pattern(regexp = "^(USER|ADMIN)$", message = "Role must be USER or ADMIN")
   private String role;

   // Getters and Setters
   public Long getId() {
       return id;
   }

   public void setId(Long id) {
       this.id = id;
   }

   public String getName() {
       return name;
   }

   public void setName(String name) {
       this.name = name;
   }

   public String getEmail() {
       return email;
   }

   public void setEmail(String email) {
       this.email = email;
   }

   public String getRole() {
       return role;
   }

   public void setRole(String role) {
       this.role = role;
   }
}

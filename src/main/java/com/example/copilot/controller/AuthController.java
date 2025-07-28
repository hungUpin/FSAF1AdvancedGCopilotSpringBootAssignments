package com.example.copilot.controller;

import com.example.copilot.dto.AuthResponse;
import com.example.copilot.dto.LoginRequest;
import com.example.copilot.dto.RegisterRequest;
import com.example.copilot.entity.User;
import com.example.copilot.repository.UserRepository;
import com.example.copilot.security.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Tag(name = "Authentication", description = "User authentication and registration endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "User Login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Input sanitization - trim whitespace
            String email = loginRequest.getEmail().trim().toLowerCase();
            String password = loginRequest.getPassword();
            
            // Additional email format validation
            if (!isValidEmailFormat(email)) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "error", "Invalid email format",
                        "message", "Please provide a valid email address",
                        "timestamp", LocalDateTime.now().toString()
                    )
                );
            }
            
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            
            // Load user details and generate token
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            String token = jwtTokenUtil.generateToken(userDetails);
            
            // Get user information for response
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                AuthResponse response = new AuthResponse(token, user.getEmail(), user.getName(), user.getRole());
                
                log.info("User {} logged in successfully", email);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "User data retrieval failed")
                );
            }
            
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for email: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of(
                    "error", "Invalid credentials",
                    "message", "Email or password is incorrect",
                    "timestamp", LocalDateTime.now().toString()
                )
            );
        } catch (Exception e) {
            log.error("Login error for email {}: {}", loginRequest.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                    "error", "Login failed",
                    "message", "An unexpected error occurred",
                    "timestamp", LocalDateTime.now().toString()
                )
            );
        }
    }

    @Operation(summary = "User Registration", description = "Register a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Validation error or user already exists"),
        @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // Input sanitization
            String name = registerRequest.getName().trim();
            String email = registerRequest.getEmail().trim().toLowerCase();
            String password = registerRequest.getPassword();
            
            // Additional validation
            if (!isValidEmailFormat(email)) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "error", "Invalid email format",
                        "message", "Please provide a valid email address",
                        "timestamp", LocalDateTime.now().toString()
                    )
                );
            }
            
            // Check password confirmation
            if (!registerRequest.isPasswordConfirmed()) {
                return ResponseEntity.badRequest().body(
                    Map.of(
                        "error", "Password mismatch",
                        "message", "Password and confirmation password do not match",
                        "timestamp", LocalDateTime.now().toString()
                    )
                );
            }
            
            // Check if user already exists
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                        "error", "Email already registered",
                        "message", "A user with this email already exists",
                        "timestamp", LocalDateTime.now().toString()
                    )
                );
            }
            
            // Create new user
            User newUser = new User();
            newUser.setName(name);
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setRole(registerRequest.getRole()); // Always "USER" from RegisterRequest
            
            User savedUser = userRepository.save(newUser);
            
            log.info("New user registered: {}", email);
            
            AuthResponse response = new AuthResponse();
            response.setMessage("Registration successful! Please log in with your credentials.");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Registration error for email {}: {}", registerRequest.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                    "error", "Registration failed",
                    "message", "An unexpected error occurred during registration",
                    "timestamp", LocalDateTime.now().toString()
                )
            );
        }
    }
    
    @Operation(summary = "Validate Token", description = "Check if the provided JWT token is valid")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", "Invalid authorization header format")
                );
            }
            
            String token = authHeader.substring(7);
            String email = jwtTokenUtil.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            
            if (jwtTokenUtil.validateToken(token, userDetails)) {
                Optional<User> userOpt = userRepository.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "email", user.getEmail(),
                        "name", user.getName(),
                        "role", user.getRole()
                    ));
                }
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("valid", false, "error", "Invalid or expired token")
            );
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("valid", false, "error", "Token validation failed")
            );
        }
    }
    
    // Helper method for email validation
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Additional email validation beyond @Email annotation
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex) && email.length() <= 100;
    }
    
    // Exception handler for this controller
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        
        StringBuilder errors = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            errors.append(error.getDefaultMessage()).append("; ");
        });
        
        return ResponseEntity.badRequest().body(
            Map.of(
                "error", "Validation failed",
                "message", errors.toString().trim(),
                "timestamp", LocalDateTime.now().toString()
            )
        );
    }
}

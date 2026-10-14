package com.helpdesk.controller;

import com.helpdesk.dto.UserRegistrationDto;
import com.helpdesk.exception.HelpDeskException;
import com.helpdesk.model.Organization;
import com.helpdesk.model.Role;
import com.helpdesk.model.User;
import com.helpdesk.repository.OrganizationRepository;
import com.helpdesk.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, OrganizationRepository organizationRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationDto", new UserRegistrationDto());
        model.addAttribute("organizations", organizationRepository.findAll());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationDto") UserRegistrationDto dto,
                               BindingResult bindingResult,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("organizations", organizationRepository.findAll());
            return "register";
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            model.addAttribute("registrationError", "Passwords do not match");
            model.addAttribute("organizations", organizationRepository.findAll());
            return "register";
        }

        if (userRepository.existsByUsername(dto.getUsername())) {
            model.addAttribute("registrationError", "Username already taken: " + dto.getUsername());
            model.addAttribute("organizations", organizationRepository.findAll());
            return "register";
        }

        Organization org = organizationRepository.findBySlug(dto.getOrganizationSlug())
                .orElseThrow(() -> new HelpDeskException("Organization slug not found: " + dto.getOrganizationSlug()));

        User user = new User(
                dto.getUsername(),
                dto.getEmail(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getFullName(),
                org,
                Role.ROLE_CUSTOMER
        );
        userRepository.save(user);

        return "redirect:/login?registered=true";
    }
}

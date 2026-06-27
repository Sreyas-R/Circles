package com.circles.circles.Bizprocessor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import com.circles.circles.Model.ResponseObj;
import com.circles.circles.Model.User;
import com.circles.circles.Repository.UserRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Service
public class LoginProcessor {
    private final UserRepo userRepo;
    private final PasswordEncoder pwEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public LoginProcessor(UserRepo userRepo, PasswordEncoder pwEncoder, AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {
        this.userRepo = userRepo;
        this.pwEncoder = pwEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public ResponseObj registerUser(User user) {
        ResponseObj responseObj = new ResponseObj();
        String username = user.getUsername();
        String email = user.getEmail();
        String password = user.getPassword();

        // 1.Check if email is already used
        if (userRepo.existsByEmail(email)) {
            responseObj.setErrorMessage("Email_Used_Already");
            return responseObj;
        }

        if (userRepo.existsByUsername(username)) {
            responseObj.setErrorMessage("Username_Used_Already");
            return responseObj;
        }

        // 2.Password validation
        if (!passwordvalidation(password)) {
            responseObj.setErrorMessage("Invalid_Password");
            return responseObj;
        }

        // Validations passed , now we create entry and store password hashed
        String hashPw = pwEncoder.encode(password);
        if (hashPw != null) {
            user.setPassword(hashPw);
            userRepo.save(user);
            responseObj.setSuccMessage("Successful_Registration");
        }

        return responseObj;
    }

    public ResponseObj loginUser(User user, HttpServletRequest request, HttpServletResponse response) {
        ResponseObj responseObj = new ResponseObj();

        String identifier = user.getEmail() != null ? user.getEmail() : user.getUsername();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, user.getPassword()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            responseObj.setSuccMessage("LOGIN_SUCCESS");
        } catch (AuthenticationException e) {
            responseObj.setErrorMessage("INVALID_CREDENTIALS");
        }

        return responseObj;
    }

    private static boolean passwordvalidation(String passwd) {
        String pattern = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}";
        return passwd.matches(pattern);
    }
}

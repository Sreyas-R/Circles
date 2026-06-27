package com.circles.circles.Controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.circles.circles.Bizprocessor.LoginProcessor;
import com.circles.circles.Model.ResponseObj;
import com.circles.circles.Model.User;

@RestController
public class RegisterController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    private final LoginProcessor loginProcessor;

    public RegisterController(LoginProcessor loginProcessor) {
        this.loginProcessor = loginProcessor;
    }

    @PostMapping("/Register")
    public ResponseObj RegisterUser(@RequestBody User user) { // Ideally need to make User a separate DTO , and not pass
                                                              // it as entity
        logger.info("Received request to /Register for username: {}", user != null ? user.getUsername() : "null");
        ResponseObj responseObj;

        responseObj = loginProcessor.registerUser(user);

        return responseObj;
    }

    @PostMapping("/Login")
    public ResponseObj postMethodName(@RequestBody User user, jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        logger.info("Received request to /Login for username: {}", user != null ? user.getUsername() : "null");
        ResponseObj responseObj;
        responseObj = loginProcessor.loginUser(user, request, response);
        return responseObj;
    }

}

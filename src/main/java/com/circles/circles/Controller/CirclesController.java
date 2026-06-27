package com.circles.circles.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.circles.circles.Bizprocessor.CirclesProcessor;
import com.circles.circles.Model.Circles;
import com.circles.circles.Model.ResponseObj;


@RestController
public class CirclesController {
    private static final Logger logger = LoggerFactory.getLogger(CirclesController.class);
    
    private final CirclesProcessor circlesProcessor;

    public CirclesController(CirclesProcessor circlesProcessor){
        this.circlesProcessor = circlesProcessor;
    }
    
    @PostMapping("/CreateCircle")
    public ResponseObj createCircle(@RequestBody Circles userDetails) {
        logger.info("Received request to /CreateCircle for circle name: {}", userDetails != null ? userDetails.getName() : "null");
        //Necessary Inputs 
        ResponseObj res = circlesProcessor.createCircle(userDetails);
        return res;
    }

    @GetMapping("/InviteUser")
    public ResponseObj inviteMember(@RequestParam Long circleId) {
        logger.info("Received request to /InviteUser for circleId: {}", circleId);
        ResponseObj res = circlesProcessor.inviteUser(circleId);
        return res;
    }

    @GetMapping("/joinCircle/{token}")
    public ResponseObj joinCircle(@PathVariable("token") String token) {
        logger.info("Received request to /joinCircle with token: {}", token);
        ResponseObj res = circlesProcessor.joinCircle(token);
        return res;
    }
    
    @GetMapping("/GetCircles")
    public ResponseObj getUserCircles() {
        logger.info("Received request to /GetCircles");
        return circlesProcessor.getUserCircles();
    }
    
}

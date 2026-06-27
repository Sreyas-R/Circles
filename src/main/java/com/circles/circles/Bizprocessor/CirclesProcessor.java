package com.circles.circles.Bizprocessor;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.circles.circles.Helpers.CirclesConstants;
import com.circles.circles.Model.Circles;
import com.circles.circles.Model.CirclesInvite;
import com.circles.circles.Model.ResponseObj;
import com.circles.circles.Model.User;
import com.circles.circles.Model.circlesRelation;
import com.circles.circles.Repository.CircleInviteRepo;
import com.circles.circles.Repository.CircleRelRepo;
import com.circles.circles.Repository.CircleRepo;

import jakarta.transaction.Transactional;

@Service
public class CirclesProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CirclesProcessor.class);

    private final CircleRepo circleRepo;
    private final CircleRelRepo circleRelRepo;
    private final CircleInviteRepo circleInviteRepo;

    public CirclesProcessor(CircleRepo circleRepo , CircleRelRepo circleRelRepo , CircleInviteRepo circleInviteRepo) {
        this.circleRepo = circleRepo;
        this.circleRelRepo = circleRelRepo;
        this.circleInviteRepo = circleInviteRepo;
    }

    
    @Transactional
    public ResponseObj createCircle(Circles circle){
        ResponseObj res = new ResponseObj();
        String name = circle.getName();
        //Thread safe getting data from auth , this works because User implements UserDetails
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        Long userId = user.getId();
        
        circle.setCreatedBy(userId);
        circle.setIsActive(true);

        if(name != null && userId != null){
            logger.info("Attempting to create circle: '{}' by userId: {}", name, userId);
            try{
            Circles c = circleRepo.save(circle);
            Long circleId = c.getId();                  //Creating a circle

            circlesRelation cr = new circlesRelation(userId, circleId, CirclesConstants.ROLE_OWNER);
            circleRelRepo.save(cr);                     //Circle relations with owner
            logger.debug("Successfully created circle relations for circleId: {} and userId: {}", circleId, userId);
            }catch(Exception e){
                logger.error("Error occurred while creating circle '{}'", name, e);
                res.setErrorMessage("ERROR_CREATING_CIRCLE");
                return res;
            }
            res.setSuccMessage("CIRCLE_CREATED");
        }
        return res;

    }

    public ResponseObj getUserCircles() {
        ResponseObj res = new ResponseObj();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) auth.getPrincipal();
            Long userId = user.getId();

            java.util.List<circlesRelation> relations = circleRelRepo.findByUserId(userId);
            logger.debug("Found {} circle relations for userId: {}", relations.size(), userId);
            
            java.util.List<Long> circleIds = new java.util.ArrayList<>();
            for (circlesRelation cr : relations) {
                circleIds.add(cr.getCircleId());
            }

            java.util.List<Circles> userCircles = new java.util.ArrayList<>();
            if (!circleIds.isEmpty()) {
                logger.debug("Fetching circles for IDs: {}", circleIds);
                userCircles = circleRepo.findByIdIn(circleIds);
            }

            logger.info("Successfully fetched {} circles for userId: {}", userCircles.size(), userId);
            res.setData(userCircles);
            res.setSuccMessage("SUCCESS");
        } catch (Exception e) {
            logger.error("Error occurred while fetching user circles", e);
            res.setErrorMessage("ERROR_FETCHING_CIRCLES");
        }
        return res;
    }

    public ResponseObj inviteUser(Long circleId){
        //Creates an invite link - one time use that can be shared with anyone
        ResponseObj res = new ResponseObj();
        CirclesInvite inviteDets = new CirclesInvite();
        //1. Check if user is owner of the circleId in circles_membership
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication auth = context.getAuthentication();

        User user = (User) auth.getPrincipal();
        logger.info("User {} requesting invite link for circleId {}", user.getId(), circleId);
        
        boolean isOwner = circleRelRepo.findByUserIdAndCircleIdAndRole(user.getId(), circleId, CirclesConstants.ROLE_OWNER).isPresent();

        if(isOwner){
            
            //Checking if there is already an inviteLink to this circleId which is not used
            Optional<CirclesInvite> existingInvite = circleInviteRepo
            .findByUserIdAndCircleIdAndUsedDateIsNullAndExpiryDateAfter(
                user.getId(), 
                circleId, 
                Date.valueOf(LocalDate.now())
            );
            if(existingInvite.isPresent()){
                String token = existingInvite.get().getToken();
                String Link = CirclesConstants.BASE_INVITE_URL + token;
                res.setSuccMessage("SUCCESS");
                res.setInviteLink(Link);
                return res;
            }


            String token = UUID.randomUUID().toString().replace("-","");

            LocalDate expiryDt = LocalDate.now().plusDays(7);
            Date expDt = Date.valueOf(expiryDt);

            inviteDets.setUserId(user.getId());
            inviteDets.setCircleId(circleId);
            inviteDets.setExpiryDate(expDt);
            inviteDets.setToken(token);

            circleInviteRepo.save(inviteDets);

            String Link = CirclesConstants.BASE_INVITE_URL + token;
            res.setSuccMessage("SUCCESS");
            res.setInviteLink(Link);
        }else{
            res.setErrorMessage("INVALID_USER");
        }
        return res;
    }

    public ResponseObj joinCircle(String token){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();

        ResponseObj res = new ResponseObj();
        Date currDate = Date.valueOf(LocalDate.now());
        logger.info("User {} attempting to join circle using token", user.getId());
        
        Optional<CirclesInvite> c = circleInviteRepo.findByTokenAndExpiryDateAfterAndUsedDateIsNull(token , currDate);
        if(c.isPresent()){
            CirclesInvite invite = c.get();
            Long circleId = invite.getCircleId();
            //Check if user is already part of this circle or is the owner (circle_membership)
            boolean isExisting = circleRelRepo.findByUserIdAndCircleId(user.getId(), circleId).isPresent();

            if(isExisting){
                res.setErrorMessage("ALREADY_JOINED");
            }

            //Add user as member to existing circle as member
            circlesRelation newMember = new circlesRelation(user.getId() , circleId , CirclesConstants.ROLE_MEMBER);
            circleRelRepo.save(newMember);

            //Updating circle_invites with token as validated
            invite.setJoineeId(user.getId());
            invite.setUsedDate(currDate);

            circleInviteRepo.save(invite);

            res.setSuccMessage("SUCCESS");


        }else{
            res.setErrorMessage("INVALID_LINK");
        }

        return res;
    }
    
}

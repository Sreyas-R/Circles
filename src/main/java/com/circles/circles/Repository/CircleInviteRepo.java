package com.circles.circles.Repository;

import java.sql.Date;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.circles.circles.Model.CirclesInvite;

public interface  CircleInviteRepo extends  JpaRepository<CirclesInvite, Long>{

    Optional<CirclesInvite> findByUserIdAndCircleIdAndUsedDateIsNullAndExpiryDateAfter(Long userId , Long circleId , Date expirDate) ;

    Optional<CirclesInvite> findByTokenAndExpiryDateAfterAndUsedDateIsNull(String token , Date currDate);

}

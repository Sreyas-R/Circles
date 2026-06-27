package com.circles.circles.Repository;

import java.sql.Date;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.circles.circles.Model.CirclesInvite;

@Repository
public interface  CircleInviteRepo extends  JpaRepository<CirclesInvite, Long>{

    Optional<CirclesInvite> findByUserIdAndCircleIdAndUsedDateIsNullAndExpiryDateAfter(Long userId , Long circleId , Date expirDate) ;

    Optional<CirclesInvite> findByTokenAndExpiryDateAfterAndUsedDateIsNull(String token , Date currDate);

}

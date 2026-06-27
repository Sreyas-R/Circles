package com.circles.circles.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/// Links user and circle
@Entity
@Table(
    name = "circle_memberships",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "circle_id"})
    }
)
public class circlesRelation {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id" , nullable=false)
    private Long userId;

    @Column(name = "circle_id" , nullable = false)
    private Long circleId;
    //{Owner , Member}
    @Column(nullable = false)
    private String role;
    
    public circlesRelation(Long userId , Long circleId , String role){
        this.circleId = circleId;
        this.userId = userId;
        this.role = role;
    }

    public circlesRelation(){

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCircleId() {
        return circleId;
    }

    public void setCircleId(Long circleId) {
        this.circleId = circleId;
    }

}

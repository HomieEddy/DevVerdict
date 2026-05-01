package com.devverdict.review.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "framework_id", nullable = false)
    private Long frameworkId;

    @Column(nullable = false, length = 1000)
    private String comment;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "hidden", nullable = false)
    private Boolean hidden = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "pros", length = 500)
    private String pros;

    @Column(name = "cons", length = 500)
    private String cons;

    @Column(name = "helpful_votes", nullable = false)
    private Integer helpfulVotes = 0;

    @Column(name = "not_helpful_votes", nullable = false)
    private Integer notHelpfulVotes = 0;

    public Review() {
    }

    public Review(Long frameworkId, String comment, Integer rating) {
        this.frameworkId = frameworkId;
        this.comment = comment;
        this.rating = rating;
        this.helpfulVotes = 0;
        this.notHelpfulVotes = 0;
        this.createdAt = Instant.now();
    }

    public Review(Long frameworkId, String comment, Integer rating, Long userId, String username) {
        this.frameworkId = frameworkId;
        this.comment = comment;
        this.rating = rating;
        this.userId = userId;
        this.username = username;
        this.helpfulVotes = 0;
        this.notHelpfulVotes = 0;
        this.createdAt = Instant.now();
    }

    public Review(Long frameworkId, String comment, Integer rating, Long userId, String username, String pros, String cons) {
        this.frameworkId = frameworkId;
        this.comment = comment;
        this.rating = rating;
        this.userId = userId;
        this.username = username;
        this.pros = pros;
        this.cons = cons;
        this.helpfulVotes = 0;
        this.notHelpfulVotes = 0;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFrameworkId() {
        return frameworkId;
    }

    public void setFrameworkId(Long frameworkId) {
        this.frameworkId = frameworkId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getPros() {
        return pros;
    }

    public void setPros(String pros) {
        this.pros = pros;
    }

    public String getCons() {
        return cons;
    }

    public void setCons(String cons) {
        this.cons = cons;
    }

    public Integer getHelpfulVotes() {
        return helpfulVotes;
    }

    public void setHelpfulVotes(Integer helpfulVotes) {
        this.helpfulVotes = helpfulVotes;
    }

    public Integer getNotHelpfulVotes() {
        return notHelpfulVotes;
    }

    public void setNotHelpfulVotes(Integer notHelpfulVotes) {
        this.notHelpfulVotes = notHelpfulVotes;
    }

    public void incrementHelpfulVotes() {
        this.helpfulVotes++;
    }

    public void decrementHelpfulVotes() {
        this.helpfulVotes = Math.max(0, this.helpfulVotes - 1);
    }

    public void incrementNotHelpfulVotes() {
        this.notHelpfulVotes++;
    }

    public void decrementNotHelpfulVotes() {
        this.notHelpfulVotes = Math.max(0, this.notHelpfulVotes - 1);
    }
}

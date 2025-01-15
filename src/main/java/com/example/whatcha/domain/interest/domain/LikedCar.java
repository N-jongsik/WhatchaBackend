package com.example.whatcha.domain.interest.domain;

import com.example.whatcha.domain.usedCar.domain.UsedCar;
import com.example.whatcha.domain.user.domain.User;
import com.example.whatcha.global.entity.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "liked_cars")
public class LikedCar extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likedCarId;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_car_id", nullable = false)
    private UsedCar usedCar;

    private boolean isLiked;

    public void updateLiked(boolean isLiked) {
        this.isLiked = isLiked;
    }
}

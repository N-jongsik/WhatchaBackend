package com.example.whatcha.domain.interest.service;

import com.example.whatcha.domain.interest.dao.LikedCarRepository;
import com.example.whatcha.domain.interest.dao.UserCarAlertRepository;
import com.example.whatcha.domain.interest.domain.LikedCar;
import com.example.whatcha.domain.interest.domain.UserCarAlert;
import com.example.whatcha.domain.interest.dto.CarPreviewResponseDto;
import com.example.whatcha.domain.interest.dto.UserCarAlertResponseDto;
import com.example.whatcha.domain.usedCar.dao.ModelRepository;
import com.example.whatcha.domain.usedCar.dao.UsedCarRepository;
import com.example.whatcha.domain.usedCar.domain.Model;
import com.example.whatcha.domain.usedCar.domain.UsedCar;
import com.example.whatcha.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterestServiceImpl implements InterestService {

    private final LikedCarRepository likedCarRepository;
    private final UsedCarRepository usedCarRepository;
    private final UserCarAlertRepository userCarAlertRepository;
    private final ModelRepository modelRepository;

    @Transactional
    @Override
    public Page<CarPreviewResponseDto> getLikedCarList(Long userId, Pageable pageable) {

        Page<LikedCar> likedCars = likedCarRepository.findByUserIdAndIsLikedTrue(userId, pageable);

        return likedCars.map(likedCar -> {
            UsedCar usedCar = likedCar.getUsedCar();

            return CarPreviewResponseDto.builder()
                    .usedCarId(usedCar.getUsedCarId())
                    .mainImage(usedCar.getMainImage())
                    .modelName(usedCar.getModelName())
                    .registrationDate(usedCar.getRegistrationDate())
                    .mileage(usedCar.getMileage())
                    .vhclRegNo(usedCar.getVhclRegNo())
                    .price(usedCar.getPrice())
                    .likeCount(usedCar.getLikeCount())
                    .isLiked(likedCar.isLiked())
                    .build();
        });
    }

    @Transactional
    @Override
    public boolean toggleLike(Long userId, Long usedCarId) {
        Optional<LikedCar> likedCarOptional = likedCarRepository.findByUserIdAndUsedCar_UsedCarId(userId, usedCarId);

        if (likedCarOptional.isPresent()) {
            LikedCar likedCar = likedCarOptional.get();
            boolean newLikeStatus = !likedCar.isLiked();
            // isLiked 값 토글
            likedCar.updateLiked(newLikeStatus);

            // likeCount 업데이트
            UsedCar usedCar = likedCar.getUsedCar();
            if (newLikeStatus) {
                usedCar.incrementLikeCount();
            } else {
                usedCar.decrementLikeCount();
            }
            return newLikeStatus; // 변경된 상태 반환

        } else {
            UsedCar usedCar = usedCarRepository.findById(usedCarId)
                    .orElseThrow(() -> new EntityNotFoundException("UsedCar not found with id: " + usedCarId));
            // LikedCar 생성 및 저장
            LikedCar newLikedCar = LikedCar.builder()
                    .userId(userId)
                    .usedCar(usedCar)
                    .isLiked(true)
                    .build();
            likedCarRepository.save(newLikedCar);
            usedCar.incrementLikeCount();
            return true;
        }
    }

    @Transactional
    @Override
    public List<UserCarAlertResponseDto> getAlertedModelList(Long userId) {
        List<UserCarAlert> userCarAlertList = userCarAlertRepository.findAllByUserId(userId);

        return userCarAlertList.stream().map(userCarAlert -> UserCarAlertResponseDto.builder()
                .userCarAlertId(userCarAlert.getUserCarAlertId())
                .userId(userId)
                .modelId(userCarAlert.getModel().getModelId())
                .modelName(userCarAlert.getModel().getModelName())
                .alertExpirationDate(userCarAlert.getAlertExpirationDate())
                .build()).toList();
    }

    @Transactional
    @Override
    public void deleteAlertByUserAndModel(Long userId, Long modelId) {
        userCarAlertRepository.deleteByUserIdAndModel_ModelId(userId, modelId);
    }

    @Override
    public List<CarPreviewResponseDto> getRecommendedCarList(User user, int limit) {

        // 최신순 정렬
        Sort sort = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(0, limit, sort);

        // 빈 리스트 생성
        List<UsedCar> recommendCars = new ArrayList<>();
        // 중고차 추천 데이터 조회
        if (user.getPreferenceModel1() != null || user.getPreferenceModel2() != null || user.getPreferenceModel3() != null) {
            recommendCars = usedCarRepository.findRecommendedCars(
                    user.getBudgetMin(),
                    user.getBudgetMax(),
                    user.getPreferenceModel1(),
                    user.getPreferenceModel2(),
                    user.getPreferenceModel3(),
                    pageable);
        }
        if (recommendCars.size() < limit) {
            int remaining = limit - recommendCars.size();
            List<Long> excludeIds = recommendCars.stream()
                    .map(UsedCar::getUsedCarId)
                    .collect(Collectors.toList());

            List<UsedCar> additionalCars = usedCarRepository.findByIdNotIn(
                    excludeIds.isEmpty() ? List.of(-1L) : excludeIds,
                    PageRequest.of(0, remaining, sort));

            recommendCars.addAll(additionalCars);
        }
        return recommendCars.stream()
                .map(item -> CarPreviewResponseDto.builder()
                        .usedCarId(item.getUsedCarId())
                        .mainImage(item.getMainImage())
                        .modelName(item.getModelName())
                        .registrationDate(item.getRegistrationDate())
                        .mileage(item.getMileage())
                        .vhclRegNo(item.getVhclRegNo())
                        .price(item.getPrice())
                        .build()).toList();
    }


    @Override
    public UserCarAlert addUserCarAlert(Long userId, String modelName, LocalDate alertExpirationDate) {
        Model model = modelRepository.findByModelName(modelName)
                .orElseThrow(() -> new EntityNotFoundException("Model not found with name: " + modelName));

        return userCarAlertRepository.findByUserIdAndModel(userId, model)
                .map(alert -> {
                    alert.updateAlertExpirationDate(alertExpirationDate); // 만료일 업데이트
                    return userCarAlertRepository.save(alert); // 업데이트 후 저장
                })
                .orElseGet(() -> userCarAlertRepository.save( // 새로운 알림 생성 및 저장
                        UserCarAlert.builder()
                                .userId(userId)
                                .model(model)
                                .alertExpirationDate(alertExpirationDate)
                                .build()
                ));
    }

    @Override
    public List<CarPreviewResponseDto> getMostLikedCarList(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<UsedCar> topLikedCars = usedCarRepository.findByStatusOrderByLikeCountDesc("구매 가능", pageable);

        return topLikedCars.stream().map(usedCar -> CarPreviewResponseDto.builder()
                .usedCarId(usedCar.getUsedCarId())
                .mainImage(usedCar.getMainImage())
                .modelName(usedCar.getModelName())
                .registrationDate(usedCar.getRegistrationDate())
                .mileage(usedCar.getMileage())
                .vhclRegNo(usedCar.getVhclRegNo())
                .price(usedCar.getPrice())
                .build()).toList();
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul") // 매일 자정 실행
    public void deleteExpiredAlerts() {
        userCarAlertRepository.deleteExpiredAlerts(LocalDate.now());
    }
}

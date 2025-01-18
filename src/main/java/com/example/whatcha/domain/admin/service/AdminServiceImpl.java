package com.example.whatcha.domain.admin.service;

import com.example.whatcha.domain.admin.dto.request.RegisterCarReqDto;
import com.example.whatcha.domain.admin.dto.response.*;
import com.example.whatcha.domain.branchStore.dao.BranchStoreRepository;
import com.example.whatcha.domain.branchStore.domain.BranchStore;
import com.example.whatcha.domain.coupon.dao.CouponRepository;
import com.example.whatcha.domain.coupon.dao.UserCouponsRepository;
import com.example.whatcha.domain.coupon.domain.Coupon;
import com.example.whatcha.domain.coupon.dto.request.CouponReqDto;
import com.example.whatcha.domain.coupon.dto.response.CouponAdminResDto;
import com.example.whatcha.domain.coupon.exception.CouponNotFoundException;
import com.example.whatcha.domain.order.dao.OrderRepository;
import com.example.whatcha.domain.order.domain.Order;
import com.example.whatcha.domain.usedCar.dao.ModelRepository;
import com.example.whatcha.domain.usedCar.dao.UsedCarRepository;
import com.example.whatcha.domain.usedCar.domain.Model;
import com.example.whatcha.domain.usedCar.domain.UsedCar;
import com.example.whatcha.domain.user.dao.UserRepository;
import com.example.whatcha.domain.user.domain.User;
import com.example.whatcha.domain.user.dto.response.UserInfoResDto;
import com.example.whatcha.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final CouponRepository couponRepository;
    private final UserCouponsRepository userCouponsRepository;
    private final UserRepository userRepository;
    private final BranchStoreRepository branchStoreRepository;
    private final OrderRepository orderRepository;
    private final UsedCarRepository usedCarRepository;
    private final ModelRepository modelRepository;

    @Override
    public void addAdminCoupon(CouponReqDto couponReqDto) {
        String randomCouponCode = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        Coupon coupon = Coupon.builder()
                .couponCode(randomCouponCode)
                .couponName(couponReqDto.getCouponName())
                .discountPercentage(couponReqDto.getDiscountPercentage())
                .discountAmount(couponReqDto.getDiscountAmount())
                .maxDiscountAmount(couponReqDto.getMaxDiscountAmount())
                .build();
        couponRepository.save(coupon);
    }

    @Override
    public Page<CouponAdminResDto> getAllAdminCoupon(Pageable pageable) {
        return couponRepository.findAll(pageable)
                .map(coupon -> CouponAdminResDto.builder()
                        .couponId(coupon.getCouponId())
                        .couponCode(coupon.getCouponCode())
                        .couponName(coupon.getCouponName())
                        .discountPercentage(coupon.getDiscountPercentage())
                        .discountAmount(coupon.getDiscountAmount())
                        .maxDiscountAmount(coupon.getMaxDiscountAmount())
                        .build());
    }


    @Override
    public CouponAdminResDto getCouponById(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id: " + couponId));

        return CouponAdminResDto.builder()
                .couponId(coupon.getCouponId())
                .couponCode(coupon.getCouponCode())
                .couponName(coupon.getCouponName())
                .discountPercentage(coupon.getDiscountPercentage())
                .discountAmount(coupon.getDiscountAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .build();
    }

    @Override
    @Transactional
    public void deleteCoupon(Long couponId) {
        // 쿠폰 존재 여부 확인
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id: " + couponId));

        // 해당 쿠폰과 관련된 userCoupons 삭제
        userCouponsRepository.deleteByCouponCouponId(couponId);

        // 쿠폰 삭제
        couponRepository.deleteById(couponId);
    }

    @Override
    public List<UserInfoResDto> getAllUser() {
        // User 엔티티 전체 조회
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(UserInfoResDto::entityToResDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserInfoResDto getUserById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return UserInfoResDto.entityToResDto(user);
    }

    @Override
    public List<AgeStatisticsDto> getAgeStatistics() {
        List<User> users = userRepository.findAll();

        // 연령대별 사용자 수 계산
        Map<Integer, Long> ageStatistics = users.stream()
                .filter(user -> user.getAgeGroup() != null) // ageGroup이 null인 사용자는 제외
                .collect(Collectors.groupingBy(User::getAgeGroup, Collectors.counting()));

        // Map 데이터를 DTO 리스트로 변환
        return ageStatistics.entrySet().stream()
                .map(entry -> AgeStatisticsDto.builder()
                        .ageRange(entry.getKey()) // ageGroup 값
                        .count(entry.getValue().intValue()) // 사용자 수
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<GenderStatisticsDto> getGenderStatistics() {
        List<User> users = userRepository.findAll();

        // 연령대별 사용자 수 계산
        Map<String, Long> genderStatistics = users.stream()
                .filter(user -> user.getGender() != null) // ageGroup이 null인 사용자는 제외
                .collect(Collectors.groupingBy(User::getGender, Collectors.counting()));

        // Map 데이터를 DTO 리스트로 변환
        return genderStatistics.entrySet().stream()
                .map(entry -> GenderStatisticsDto.builder()
                        .gender(entry.getKey()) // ageGroup 값
                        .count(entry.getValue().intValue()) // 사용자 수
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<BranchStoreResDto> getAllBranchStore() {
        List<BranchStore> branchStores = branchStoreRepository.findAll();

        return branchStores.stream()
                .map(BranchStoreResDto::entityToResDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UsedCarByBranchResDto> getBranchStoreById(Long branchStoreId) {
        // 브랜치 ID에 해당하는 차량 리스트 조회
        List<UsedCar> usedCars = usedCarRepository.findByBranchStore_BranchStoreId(branchStoreId);

        return usedCars.stream()
                .map(usedCar -> new UsedCarByBranchResDto(
                        usedCar.getUsedCarId(),
                        usedCar.getModelName(),
                        usedCar.getPrice(),
                        usedCar.getRegistrationDate(),
                        usedCar.getMileage(),
                        usedCar.getFuelType(),
                        usedCar.getEngineCapacity(),
                        usedCar.getExteriorColor(),
                        usedCar.getInteriorColor(),
                        usedCar.getModelType(),
                        usedCar.getPassengerCapacity(),
                        usedCar.getDriveType(),
                        usedCar.getVhclRegNo(),
                        usedCar.getYears(),
                        usedCar.getTransmission(),
                        usedCar.getStatus(),
                        usedCar.getGoodsNo(),
                        usedCar.getMainImage()))
                .collect(Collectors.toList());
    }

    //관리자 대시보드 회원수, 판매차량, 판매금액, 차량재고
    @Override
    public DashBoardResDto getDashBoard() {
        //유저수
        Long userCount = userRepository.count();

        //판매차량
        Long orderCount = orderRepository.count();

        //총 판매량
        Long totalSales = orderRepository.getTotalSales();

        Long wholeCarCont = usedCarRepository.count();

        //차량 재고
        Long carStock = wholeCarCont - orderCount;

        return DashBoardResDto.builder()
                .userCount(userCount)
                .orderCount(orderCount)
                .totalSales(totalSales)
                .carStock(carStock)
                .build();
    }

    @Override
    public List<TradeHistoryResDto> getTradeHistory() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();

        // TradeHistoryResDto 리스트 생성
        List<TradeHistoryResDto> tradeHistoryList = orders.stream()
                .map(order -> {
                    // usedCarId로 UsedCar 정보 조회
                    Optional<UsedCar> usedCarOpt = usedCarRepository.findById(order.getUsedCarId());
                    if (usedCarOpt.isPresent()) {
                        UsedCar usedCar = usedCarOpt.get();
                        // TradeHistoryResDto 생성
                        return TradeHistoryResDto.builder()
                                .goodsNo(usedCar.getGoodsNo())
                                .modelName(usedCar.getModelName())
                                .years(usedCar.getYears())
                                .price(usedCar.getPrice())
                                .modelType(usedCar.getModelType())
                                .status(usedCar.getStatus())
                                .build();
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull) // null 값을 제외
                .collect(Collectors.toList());

        return tradeHistoryList;
    }

    @Override
    public void registerCar(RegisterCarReqDto registerCarReqDto) {
        Long branchStoreId = registerCarReqDto.getBranchStoreId();
        BranchStore branchStore = branchStoreRepository.findById(branchStoreId).get();

        UsedCar usedCar = UsedCar.builder()
                .driveType(registerCarReqDto.getDriveType())
                .engineCapacity(registerCarReqDto.getEngineCapacity())
                .exteriorColor(registerCarReqDto.getExteriorColor())
                .fuelType(registerCarReqDto.getFuelType())
                .goodsNo(registerCarReqDto.getGoodsNo())
                .interiorColor(registerCarReqDto.getInteriorColor())
                .likeCount(0)
                .mainImage(registerCarReqDto.getMainImage())
                .mileage(registerCarReqDto.getMileage())
                .modelName(registerCarReqDto.getModelName())
                .modelType(registerCarReqDto.getModelType())
                .passengerCapacity(registerCarReqDto.getPassengerCapacity())
                .price(registerCarReqDto.getPrice())
                .registrationDate(registerCarReqDto.getRegistrationDate())
                .status("구매 가능")
                .transmission(registerCarReqDto.getTransmission())
                .vhclRegNo(registerCarReqDto.getVhclRegNo())
                .years(registerCarReqDto.getYears())
                .model(registerCarReqDto.getModel())
                .branchStore(branchStore)
                .option(registerCarReqDto.getOption())
                .model(registerCarReqDto.getModel())
                .build();

        usedCarRepository.save(usedCar);
    }

    @Override
    public List<OrderStatisticsByDayResDto> getOrderStatisticsByDay() {
        List<Order> orders = orderRepository.findAll();

        Map<LocalDate, Long> orderStatistics = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt(), // createdAt기준
                        Collectors.counting() // 그룹별 개수 카운팅
                ));

        return orderStatistics.entrySet().stream()
                .map(entry -> OrderStatisticsByDayResDto.builder()
                        .date(entry.getKey())
                        .count(entry.getValue().intValue())
                        .build())
                .sorted(Comparator.comparing(OrderStatisticsByDayResDto::getDate)) // 날짜순 정렬
                .collect(Collectors.toList());
    }

    @Override
    public List<CarStatisticsByModelResDto> getCarStatisticsByModel() {
        // 분류 기준 키워드
        List<String> keywords = Arrays.asList("G80", "그랜저", "베뉴", "GV80", "아반떼", "쏘나타", "싼타페", "팰리세이드", "GV70", "투싼", "코나", "아이오닉6", "G70", "캐스퍼");

        // 모든 모델 조회
        List<Model> models = modelRepository.findAll();

        // 키워드별 orderCount 합산
        Map<String, Integer> groupedStatistics = new HashMap<>();
        for (String keyword : keywords) {
            int totalOrderCount = models.stream()
                    .filter(model -> model.getModelName().contains(keyword)) // 키워드 포함 여부 확인
                    .mapToInt(Model::getOrderCount) // orderCount 추출
                    .sum(); // 합산
            if (totalOrderCount > 0) { // orderCount가 0보다 클 경우만 저장
                groupedStatistics.put(keyword, totalOrderCount);
            }
        }

        // Map 데이터를 DTO 리스트로 변환
        List<CarStatisticsByModelResDto> result = groupedStatistics.entrySet().stream()
                .map(entry -> CarStatisticsByModelResDto.builder()
                        .modelName(entry.getKey())
                        .orderCount(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        return result;
    }


}

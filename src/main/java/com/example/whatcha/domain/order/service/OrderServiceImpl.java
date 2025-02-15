package com.example.whatcha.domain.order.service;

import com.example.whatcha.domain.admin.dto.response.BranchStoreResDto;
import com.example.whatcha.domain.branchStore.dao.BranchStoreRepository;
import com.example.whatcha.domain.branchStore.domain.BranchStore;
import com.example.whatcha.domain.coupon.dao.CouponRepository;
import com.example.whatcha.domain.coupon.dao.UserCouponsRepository;
import com.example.whatcha.domain.coupon.domain.Coupon;
import com.example.whatcha.domain.coupon.domain.UserCoupons;
import com.example.whatcha.domain.coupon.dto.response.CouponResDto;
import com.example.whatcha.domain.order.dao.OrderProcessRepository;
import com.example.whatcha.domain.order.dao.OrderRepository;
import com.example.whatcha.domain.order.domain.Order;
import com.example.whatcha.domain.order.domain.OrderProcess;
import com.example.whatcha.domain.order.dto.request.PathInfoReqDto;
import com.example.whatcha.domain.order.dto.response.*;
import com.example.whatcha.domain.usedCar.dao.ModelRepository;
import com.example.whatcha.domain.usedCar.dao.UsedCarRepository;
import com.example.whatcha.domain.usedCar.domain.Model;
import com.example.whatcha.domain.usedCar.domain.UsedCar;
import com.example.whatcha.domain.user.dao.UserRepository;
import com.example.whatcha.domain.user.domain.User;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderProcessRepository orderProcessRepository;
    private final UserCouponsRepository userCouponsRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final UsedCarRepository usedCarRepository;
    private final BranchStoreRepository branchStoreRepository;
    private final ModelRepository modelRepository;
    private final OkHttpClient okHttpClient;

    @Value("${secret.naver.clientId}")
    private String clientId;

    @Value("${secret.naver.clientSecret}")
    private String clientSecret;

    @Override
    public OrderProcessResDto getOrderProcess(Long orderId) {
        //orderId를 통해 OrderProcess를 조회
        OrderProcess orderProcess = orderProcessRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("OrderProcess not found for orderId: " + orderId));

        //OrderProcess를 OrderProcessResDto로 변환
        OrderProcessResDto orderProcessResDto = OrderProcessResDto.toDto(orderProcess);
        return orderProcessResDto;
    }

    @Override
    public OrderResDto getOrder(Long orderId) {
        //orderId를 통해 Order를 조회
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for orderId: " + orderId));

        //Order를 OrderResDto로 변환
        OrderResDto orderResDto = OrderResDto.toDto(order);
        return orderResDto;
    }

    //이때 order, orderprocess save해야함
    @Override
    @Transactional
    public DepositResDto payDeposit(String email, Long usedCarId, int fullPayment, int deposit, Long userCouponId) {

        // usedCar 찾기
        UsedCar usedCar = usedCarRepository.findById(usedCarId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid usedCarId: " + usedCarId));

        System.out.println("111111111111111111111111");

        if (!"구매 가능".equals(usedCar.getStatus())) {
            throw new IllegalArgumentException("해당 매물은 계약 진행중인 매물입니다!");
        }

        try {
            usedCar.changeStatus("거래중");

            usedCarRepository.save(usedCar);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        // 해당 branch ownedCarCount -1 해주기
        Long branchStoreId = usedCar.getBranchStore().getBranchStoreId();
        BranchStore branchStore = branchStoreRepository.findById(branchStoreId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid branchStoreId: " + branchStoreId));

        BranchStore updatedBranchStore = BranchStore.builder()
                .branchStoreId(branchStore.getBranchStoreId())
                .branchStoreName(branchStore.getBranchStoreName())
                .location(branchStore.getLocation())
                .latitude(branchStore.getLatitude())
                .longitude(branchStore.getLongitude())
                .ownedCarCount(branchStore.getOwnedCarCount() - 1)
                .phone(branchStore.getPhone())
                .build();

        branchStoreRepository.save(updatedBranchStore);

        // model 찾아서 판매 갯수 +1 해주기
        Long modelId = usedCar.getModel().getModelId();
        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid modelId: " + modelId));

        Model updatedModel = Model.builder()
                .modelId(model.getModelId())
                .modelName(model.getModelName())
                .modelType(model.getModelType())
                .factoryPrice(model.getFactoryPrice())
                .orderCount(model.getOrderCount() + 1)
                .build();

        modelRepository.save(updatedModel);

        // user 찾기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userEmail: " + email));

        // Order 저장
        Order.OrderBuilder orderBuilder = Order.builder()
                .usedCarId(usedCarId)
                .userId(user.getUserId())
                .fullPayment(fullPayment)
                .deposit(deposit);

        // userCoupons 처리
        UserCoupons userCoupons = null;
        if (userCouponId != null) {
            userCoupons = userCouponsRepository.findById(userCouponId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid userCouponId: " + userCouponId));
            orderBuilder.userCoupons(userCoupons);
            userCoupons.deactivate();
        }

        Order order = orderBuilder.build();
        orderRepository.save(order);

        // orderProcess 저장
        OrderProcess orderProcess = OrderProcess.builder()
                .order(order)
                .userCoupons(userCoupons)
                .depositPaid(true)
                .contractSigned(false)
                .deliveryService(false)
                .fullyPaid(false)
                .deliveryCompleted(false)
                .build();

        orderProcessRepository.save(orderProcess);

        // DepositResDto 반환
        DepositResDto depositResDto = DepositResDto.builder()
                .orderId(order.getOrderId())
                .nickName(user.getName())
                .vhclRegNo(usedCar.getVhclRegNo())
                .registrationDate(order.getCreatedAt())
                .modelName(usedCar.getModelName())
                .deposit(deposit)
                .remainingAmount(fullPayment - deposit)
                .build();

        return depositResDto;
    }


    @Override
    @Transactional
    public void fullPayment(Long orderId) {
        //orderId를 통해 OrderProcess를 조회
        OrderProcess orderProcess = orderProcessRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("OrderProcess not found for orderId: " + orderId));

        orderProcess.markAsFullyPaid();
    }

    @Override
    @Transactional
    public void writeContract(Long orderId) {
        //orderId를 통해 OrderProcess를 조회
        OrderProcess orderProcess = orderProcessRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("OrderProcess not found for orderId: " + orderId));

        orderProcess.signContract();
    }

    @Override
    @Transactional
    public void deliveryService(Long orderId) {
        //orderId를 통해 OrderProcess를 조회
        OrderProcess orderProcess = orderProcessRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("OrderProcess not found for orderId: " + orderId));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for orderId: " + orderId));

        UsedCar usedCar = usedCarRepository.findById(order.getUsedCarId()).orElseThrow(() -> new IllegalArgumentException("Invalid usedCarId: " + order.getUsedCarId()));

        orderProcess.enableDeliveryService();
        usedCar.changeStatus("거래 완료");
    }

    @Override
    public List<OrderListResDto> getAllOrders(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userEmail: " + email));

        List<Order> orders = orderRepository.findByUserId(user.getUserId());

        List<OrderListResDto> orderListResDtos = orders.stream()
                .map(order -> {
                    // UsedCar 정보를 조회
                    UsedCar usedCar = usedCarRepository.findById(order.getUsedCarId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid UsedCarId: " + order.getUsedCarId()));

                    // OrderProcess 정보를 조회
                    OrderProcess orderProcess = orderProcessRepository.findByOrder_OrderId(order.getOrderId())
                            .orElseThrow(() -> new IllegalArgumentException("OrderProcess not found for orderId: " + order.getOrderId()));

                    // 각 주문의 process 값 설정
                    int process = 0;
                    if (!orderProcess.getFullyPaid()) {
                        process = 1; //잔금 결제중
                    } else if (!orderProcess.getContractSigned()) {
                        process = 2; //계약서 작성중
                    } else if (!orderProcess.getDeliveryService()) {
                        process = 3; //수령방법 선택중
                    } else if(!orderProcess.getDeliveryCompleted()){
                        process = 4; // 배송중
                    }

                    return OrderListResDto.builder()
                            .orderId(order.getOrderId())
                            .mainImage(usedCar.getMainImage())
                            .modelName(usedCar.getModelName())
                            .process(process)
                            .orderDate(order.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return orderListResDtos;
    }

    @Override
    public OrderSheetResDto getOrderSheet(Long orderId) {
        // Order 객체 조회
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for orderId: " + orderId));

        // UsedCar 객체 조회
        UsedCar usedCar = usedCarRepository.findById(order.getUsedCarId())
                .orElseThrow(() -> new IllegalArgumentException("UsedCar not found for usedCarId: " + order.getUsedCarId()));

        // userCoupons 처리
        UserCoupons userCoupons = null;
        CouponResDto couponResDto = null;

        // userCouponId가 null이 아닌 경우에만 쿠폰 정보 처리
        if (order.getUserCoupons() != null && order.getUserCoupons().getUserCouponId() != null) {
            Long userCouponId = order.getUserCoupons().getUserCouponId();
            userCoupons = userCouponsRepository.findById(userCouponId).orElse(null);

            if (userCoupons != null) {
                // 쿠폰 객체 가져오기 및 비활성화 처리
                Coupon coupon = userCoupons.getCoupon();
                couponResDto = CouponResDto.builder()
                        .userCouponId(userCouponId)
                        .couponName(coupon.getCouponName())
                        .discountPercentage(coupon.getDiscountPercentage())
                        .maxDiscountAmount(coupon.getMaxDiscountAmount())
                        .expiryDate(userCoupons.getExpiryDate())
                        .build();
            }
        }

        // OrderProcess 객체 조회
        OrderProcess orderProcess = orderProcessRepository.findByOrder_OrderId(order.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("OrderProcess not found for orderId: " + order.getOrderId()));

        OrderResDto orderResDto = OrderResDto.builder()
                .orderId(order.getOrderId())
                .usedCarId(usedCar.getUsedCarId())
                .fullPayment(order.getFullPayment())
                .deposit(order.getDeposit())
                .build();

        OrderProcessResDto orderProcessResDto = OrderProcessResDto.toDto(orderProcess);

        // BranchStore 객체 조회
        BranchStore branchStore = branchStoreRepository.findById(usedCar.getBranchStore().getBranchStoreId())
                .orElseThrow(() -> new IllegalArgumentException("BranchStore not found for usedCarId: " + usedCar.getUsedCarId()));

        BranchStoreResDto branchStoreResDto = BranchStoreResDto.entityToResDto(branchStore);

        // 최종 OrderSheetResDto 생성 및 반환
        return OrderSheetResDto.builder()
                .price(usedCar.getPrice())
                .modelName(usedCar.getModelName())
                .registrationDate(usedCar.getRegistrationDate())
                .vhclRegNo(usedCar.getVhclRegNo())
                .mainImage(usedCar.getMainImage())
                .mileage(usedCar.getMileage())
                .couponInfo(couponResDto)
                .orderInfo(orderResDto)
                .orderProcessInfo(orderProcessResDto)
                .branchStoreInfo(branchStoreResDto)
                .build();
    }

    @Override
    public PathInfoResDto getPathInfo(PathInfoReqDto request) throws Exception {
        // 네이버 지도 API 사용
        String url = String.format(
                "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?start=%s,%s&goal=%s,%s",
                request.getFromLng(),
                request.getFromLat(),
                request.getToLng(),
                request.getToLat()
        );

        // HTTP 요청 생성하기 with okHttpRequest
        Request okHttpRequest = new Request.Builder()
                .url(url)
                .addHeader("x-ncp-apigw-api-key-id", clientId)
                .addHeader("x-ncp-apigw-api-key", clientSecret)
                .get()
                .build();

        // API 호출해서 response가져오기
        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Request failed with status code: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : null;

            if (responseBody != null) {
                Gson gson = new Gson();
                PathInfoResDto pathInfo = gson.fromJson(responseBody, PathInfoResDto.class);
                return pathInfo;
            } else {
                throw new Exception("No response body");
            }
        }
    }
}

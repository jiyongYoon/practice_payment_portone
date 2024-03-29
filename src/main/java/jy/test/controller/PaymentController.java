package jy.test.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siot.IamportRestClient.response.IamportResponse;
import jy.test.dto.PaymentCallbackRequest;
import jy.test.dto.RequestPayDto;
import jy.test.enumeration.PaymentCheckType;
import jy.test.enumeration.PaymentType;
import jy.test.service.PaymentService;
import jy.test.service.RegularPaymentScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final RegularPaymentScheduler scheduler;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    /**
     * 주문 번호를 가지고 결제 페이지를 요청하는 api
     * @param orderUid 주문 번호
     * @return 결제를 위한 정보(RequestDto, 포트원에 전달할 내 가맹점, PG사의 정보)를 담은 결제 페이지
     */
    @GetMapping("/payment/{id}")
    public String paymentPage(@PathVariable(name = "id", required = false) String orderUid,
                              Model model) {

        RequestPayDto requestDto = paymentService.findRequestDto(orderUid);
        model.addAttribute("requestDto", requestDto);
        return "payment";
    }

    ///////////////// 단건 결제
    /**
     * PortOne에 결제요청을 하여 성공 시 Callback함수로 호출되는 api <br>
     * 결제 고유번호(imp_uid)와 가맹점 주문번호(merchant_uid)를 수신하게 된다.
     */
    @ResponseBody
    @PostMapping("/payment")
    public ResponseEntity<IamportResponse<com.siot.IamportRestClient.response.Payment>> validationPayment(
            @RequestBody String request) throws JsonProcessingException {
        PaymentCallbackRequest paymentCallbackRequest = PaymentCallbackRequest.fromString(request);

        IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                paymentService.validatePayment(paymentCallbackRequest, PaymentCheckType.CALLBACK);

        log.info("결제 응답={}", objectMapper.writeValueAsString(iamportResponse.getResponse()));

        return new ResponseEntity<>(iamportResponse, HttpStatus.OK);
    }

    /**
     * 포트원 결제가 진행되면 Webhook으로 호출되는 api <br>
     * 결제 고유번호(imp_uid)와 가맹점 주문번호(merchant_uid)를 수신하게 된다. <br>
     *  <br>
     * 포트원 웹훅(webhook)은 다음과 같은 경우에 호출됩니다. <br>
     * <br>
     * 결제가 승인되었을 때(모든 결제 수단) - (status : paid) <br>
     * 가상계좌가 발급되었을 때 - (status : ready) <br>
     * 가상계좌에 결제 금액이 입금되었을 때 - (status : paid) <br>
     * 예약결제가 시도되었을 때 - (status : paid or failed) <br>
     * 관리자 콘솔에서 결제 취소되었을 때 - (status : cancelled) <br>
     * 결제 실패 시에는 웹훅이 호출되지 않음
     */
    @ResponseBody
    @PostMapping("/payment/webhook")
    public ResponseEntity<?> webhookRequest(@RequestBody String request) {
        PaymentCallbackRequest paymentCallbackRequest = PaymentCallbackRequest.fromString(request);
        paymentCallbackRequest.setPaymentType(PaymentType.IRREGULAR);

        log.info("webhookRequest={}", paymentCallbackRequest);

        paymentService.validatePayment(paymentCallbackRequest, PaymentCheckType.WEBHOOK);

        return ResponseEntity.ok().body("웹훅 수신 완료");
    }

    ///////////////// 정기 결제
    @ResponseBody
    @PostMapping("/payment/webhook/again")
    public ResponseEntity<?> webhookAutoRequest(@RequestBody String request) {
        PaymentCallbackRequest paymentCallbackRequest = PaymentCallbackRequest.fromString(request);
        paymentCallbackRequest.setPaymentType(PaymentType.REGULAR);

        log.info("webhookAgainRequest={}", paymentCallbackRequest);

        paymentService.validatePayment(paymentCallbackRequest, PaymentCheckType.WEBHOOK);

        return ResponseEntity.ok().body("웹훅 수신 완료");
    }

    /**
     * 포트원 정기결제 웹훅. 포트원 서버에 등록한 정기결제일에 맞게 웹훅이 오며, 결제가 진행된 후 호출된다. <br>
     * 결제 고유번호(imp_uid)와 가맹점 주문번호(merchant_uid)를 수신하게 된다.
     */
    @ResponseBody
    @PostMapping("/payment/webhook/schedule")
    public ResponseEntity<?> portOneSchedulingWebhook(@RequestBody String request) {
        PaymentCallbackRequest paymentCallbackRequest = PaymentCallbackRequest.fromString(request);
        paymentCallbackRequest.setPaymentType(PaymentType.REGULAR);

        log.info("portOneSchedulingWebhook={}", paymentCallbackRequest);
        paymentService.paymentByIamportScheduling(paymentCallbackRequest);
        return ResponseEntity.ok(paymentService.validatePayment(paymentCallbackRequest, PaymentCheckType.WEBHOOK));
    }

    /**
     * 포트원 최초결제 및 정기결제등록이 진행되면 Callback으로 호출되는 api <br>
     * 결제 고유번호(imp_uid)와 가맹점 주문번호(merchant_uid), 그리고 <br>
     * 가맹점 서버가 제공한 빌링키 1:1 매핑 고유값(customer_uid)를 수신하게 된다.
     */
    @PostMapping("/billings")
    public ResponseEntity<?> issueBillingKeyAndFirstPay(@RequestBody String request) {
        PaymentCallbackRequest paymentCallbackRequest = PaymentCallbackRequest.fromString(request);

        log.info("issueBillingKeyAndFirstPay={}", paymentCallbackRequest);
        paymentService.issuedBillingKey(paymentCallbackRequest);
        return ResponseEntity.ok().body("ok");
    }

    @GetMapping("/success-payment")
    public String successPaymentPage() {
        return "success-payment";
    }

    @GetMapping("/fail-payment")
    public String failPaymentPage() {
        return "fail-payment";
    }

    @GetMapping("/success-billings")
    public String successBillingsPate() {
        return "success-billings";
    }

    @GetMapping("/fail-billings")
    public String failBillingsPage() {
        return "fail-billings";
    }
}

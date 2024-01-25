package jy.test.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.siot.IamportRestClient.response.*;
import jy.test.dto.PaymentCallbackRequest;
import jy.test.dto.RequestPayDto;
import jy.test.service.PaymentService;
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

    /**
     * PortOne에 결제요청을 하여 성공 시 Callback함수로 호출되는 api <br>
     * 결제 고유번호(imp_uid)와 가맹점 주문번호(merchant_uid)를 수신하게 된다.
     */
    @ResponseBody
    @PostMapping("/payment")
    public ResponseEntity<IamportResponse<com.siot.IamportRestClient.response.Payment>> validationPayment(
            @RequestBody PaymentCallbackRequest request) throws JsonProcessingException {

        IamportResponse<com.siot.IamportRestClient.response.Payment> iamportResponse =
                paymentService.paymentByCallback(request);

        log.info("결제 응답={}", objectMapper.writeValueAsString(iamportResponse.getResponse()));

        return new ResponseEntity<>(iamportResponse, HttpStatus.OK);
    }

    /**
     * 포트원 결제가 진행되면 Webhook으로 호출되는 api <br>
     * 결제 고유번호(imp_uid)와 가맹점 주문번호(merchant_uid)를 수신하게 된다.
     */
    @ResponseBody
    @PostMapping("/payment/webhook")
    public ResponseEntity<?> webhookRequest(@RequestBody String request) {
        PaymentCallbackRequest paymentCallbackRequest = makePaymentCallbackRequest(request);

        log.info("webhookRequest={}", paymentCallbackRequest);

        paymentService.paymentByWebhook(paymentCallbackRequest);

        return ResponseEntity.ok().body("웹훅 수신 완료");
    }

    @GetMapping("/success-payment")
    public String successPaymentPage() {
        return "success-payment";
    }

    @GetMapping("/fail-payment")
    public String failPaymentPage() {
        return "fail-payment";
    }

    private PaymentCallbackRequest makePaymentCallbackRequest(String request) {
        JsonObject requestJson = JsonParser.parseString(request).getAsJsonObject();
        return PaymentCallbackRequest.builder()
                .paymentUid(requestJson.get("imp_uid").getAsString())
                .orderUid(requestJson.get("merchant_uid").getAsString())
                .build();
    }
}

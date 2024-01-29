package jy.test.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.ScheduleData;
import com.siot.IamportRestClient.request.ScheduleEntry;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Schedule;
import jy.test.entity.Member;
import jy.test.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduleService {

    private final OrderService orderService;
    private final IamportClient iamportClient;
    private final ChronoUnit nextPaymentUnit = ChronoUnit.SECONDS;
    private final long nextPaymentAmount = 10;

//    private final ChronoUnit nextPaymentUnit = ChronoUnit.MONTHS;
//    private final long nextPaymentAmount = 1;

    //TODO 일단 스케쥴 등록되면 웹훅 오는것까지는 확인됨!
    public void makeNextOrderAndReserve(Member member) {
        member.isBillingIssued();
        Instant now = Instant.now();

        Order autoOrder = orderService.autoOrder(member, "다음달 정기결제", now);

        ScheduleData scheduleData = new ScheduleData(member.getBillingKey());
        ScheduleEntry scheduleEntry =
                new ScheduleEntry(
                        autoOrder.getOrderUid(),
                        new Date(now.plus(nextPaymentAmount, nextPaymentUnit).toEpochMilli()),
                        new BigDecimal(autoOrder.getPayment().getPrice()));
        scheduleEntry.setNoticeUrl("https://1b6d-211-55-64-30.ngrok-free.app/payment/webhook/auto");
        scheduleData.addSchedule(scheduleEntry);
        IamportResponse<List<Schedule>> listIamportResponse = new IamportResponse<>();
        try {
            listIamportResponse = iamportClient.subscribeSchedule(scheduleData);
        } catch (IamportResponseException | IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("listIamportResponse.getCode() = " + listIamportResponse.getCode());
        System.out.println("listIamportResponse.getMessage() = " + listIamportResponse.getMessage());
        System.out.println("listIamportResponse.getResponse().get(0) = " + listIamportResponse.getResponse().get(0));
        Schedule schedule = listIamportResponse.getResponse().get(0);
        System.out.println("schedule = " + schedule);
        System.out.println("schedule.getSchedule_at() = " + schedule.getScheduleAt());
        System.out.println("schedule.getCustomerUid() = " + schedule.getCustomerUid());
        System.out.println("schedule.getExecuted_at() = " + schedule.getExecuted_at());
        System.out.println("schedule.getRevoked_at() = " + schedule.getRevoked_at());
        System.out.println("schedule.getSchedule_status() = " + schedule.getSchedule_status());
        System.out.println("schedule.getPayment_status() = " + schedule.getPayment_status());
        System.out.println("schedule.getPayment_status() = " + schedule.getPayment_status());
        System.out.println("schedule.getName() = " + schedule.getName());
        System.out.println("listIamportResponse = " + listIamportResponse);

    }
}

package jy.test.controller;

import jy.test.entity.Member;
import jy.test.entity.Order;
import jy.test.persistence.impl.MemberRepositoryImpl;
import jy.test.service.MemberService;
import jy.test.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final MemberService memberService;
    private final MemberRepositoryImpl memberRepository;
    private final OrderService orderService;

    @GetMapping("/order")
    public String order(@RequestParam(name = "message", required = false) String message,
                        @RequestParam(name = "orderUid", required = false) String id,
                        Model model) {

        model.addAttribute("message", message);
        model.addAttribute("orderUid", id);

        return "order";
    }

    @PostMapping("/order")
    public String autoOrder() {
        Member member = memberRepository.findById(1L).orElse(null);
        if (member == null) {
            member = memberService.autoRegister();
        }

        Order order = orderService.autoOrder(member, "단건 결제상품", Instant.now());

        String message = "주문 실패";
        if(order != null) {
            message = "주문 성공";
        }

        String encode = URLEncoder.encode(message, StandardCharsets.UTF_8);

        return "redirect:/order?message="+encode+"&orderUid="+ order.getOrderUid();
    }
}

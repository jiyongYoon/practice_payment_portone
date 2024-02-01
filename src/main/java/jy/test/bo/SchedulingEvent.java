package jy.test.bo;

import jy.test.entity.Member;
import jy.test.entity.Order;
import jy.test.enumeration.SchedulingType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SchedulingEvent {
    private Order nextOrder;
    private Member member;
    private SchedulingType type;
}

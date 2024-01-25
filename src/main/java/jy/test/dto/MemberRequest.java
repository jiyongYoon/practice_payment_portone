package jy.test.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MemberRequest {

    private String username;
    private String email;
    private String address;
}

package com.example.easymornings;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class AlarmConfig {

    long alarm_time;
    long start_fade_fade;
    long off_time;
    @Builder.Default boolean enabled = true;
    @Builder.Default boolean monday = true;
    @Builder.Default boolean tuesday = true;
    @Builder.Default boolean wednesday = true;
    @Builder.Default boolean thursday = true;
    @Builder.Default boolean friday = true;
    @Builder.Default boolean saturday = true;
    @Builder.Default boolean sunday = true;

}
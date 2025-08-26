package com.dlyk.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
/* copy by ShigureYukina,from 2025/8/25-下午2:52 */
public enum DicEnum {

    APPELLATION("appellation"),
    NEED_LOAN("needLoan"),
    INTENTION_STATE("intentionState"),
    CLUE_STATE("clueState"),
    SOURCE("source"),
    PRODUCT("product"),
    ACTIVITY("activity");

    @Getter
    @Setter
    private String code;
}

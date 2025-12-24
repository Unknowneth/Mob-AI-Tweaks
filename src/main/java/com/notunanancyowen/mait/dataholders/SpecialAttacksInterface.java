package com.notunanancyowen.mait.dataholders;

public interface SpecialAttacksInterface {
    void setSpecialCooldown(int i);
    int getSpecialCooldown();
    default void forceSpecialAttack() {}
    default String attackType() { return "NONE"; }
}

package com.notunanancyowen.dataholders;

public interface SpecialAttacksInterface {
    void setSpecialCooldown(int i);
    int getSpecialCooldown();
    default void forceSpecialAttack() {}
}

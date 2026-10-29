package com.example.dropall.mixin;

import com.example.dropall.DropAllHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    @Shadow protected int y;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dropall$addButtons(CallbackInfo ci) {
        Object self = this;
        boolean container = self instanceof GenericContainerScreen
                || self instanceof ShulkerBoxScreen
                || self instanceof Generic3x3ContainerScreen
                || self instanceof HopperScreen;
        if (!container) return;

        int bw = 80;
        int bh = 20;
        int gap = 4;
        int left = (this.width - 176) / 2;

        int dropX = left - bw - gap;
        if (dropX < 2) dropX = 2;
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Выкинуть все"), button -> DropAllHelper.dropContainerItems())
                        .dimensions(dropX, this.y, bw, bh)
                        .build()
        );

        int takeX = left + 176 + gap;
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Забрать все"), button -> DropAllHelper.takeContainerItems())
                        .dimensions(takeX, this.y, bw, bh)
                        .build()
        );
    }
}

package com.example.dropall.mixin;

import com.example.dropall.JunkDrop;
import com.example.dropall.TakeItems;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class TakeButtonMixin<T extends ScreenHandler> extends Screen {

    @Unique private static final int TAKE_W = 60;
    @Unique private static final int TAKE_H = 20;

    @Shadow @Final protected T handler;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;

    @Unique private ButtonWidget take$btn;

    protected TakeButtonMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void take$init(CallbackInfo ci) {
        take$btn = null;
        if ((Object) this instanceof CreativeInventoryScreen) return;
        if (handler instanceof PlayerScreenHandler) return;
        if (!JunkDrop.isSupported(handler)) return;

        take$btn = ButtonWidget.builder(Text.literal("Забрать"), b -> TakeItems.run(handler))
            .dimensions(0, 0, TAKE_W, TAKE_H).build();
        this.addDrawableChild(take$btn);
        take$layout();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void take$render(CallbackInfo ci) {
        take$layout();
    }

    // справа от окна контейнера, на уровне его верхнего края
    @Unique
    private void take$layout() {
        if (take$btn == null) return;
        int bx = Math.min(this.width - TAKE_W - 2, this.x + this.backgroundWidth + 6);
        take$btn.setPosition(bx, this.y);
    }
}

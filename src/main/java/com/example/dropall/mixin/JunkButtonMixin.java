package com.example.dropall.mixin;

import com.example.dropall.JunkDrop;
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
public abstract class JunkButtonMixin<T extends ScreenHandler> extends Screen {

    @Unique private static final int JUNK_W = 84;
    @Unique private static final int JUNK_H = 20;

    @Shadow @Final protected T handler;
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;

    @Unique private ButtonWidget junk$btn;

    protected JunkButtonMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void junk$init(CallbackInfo ci) {
        junk$btn = null;
        if ((Object) this instanceof CreativeInventoryScreen) return;
        if (!JunkDrop.isSupported(handler)) return;

        junk$btn = ButtonWidget.builder(Text.literal("Выкинуть мусор"), b -> JunkDrop.run(handler))
            .dimensions(0, 0, JUNK_W, JUNK_H).build();
        this.addDrawableChild(junk$btn);
        junk$layout();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void junk$render(CallbackInfo ci) {
        junk$layout();
    }

    // выше окна, правее середины; в контейнерах на ряд выше остальных кнопок
    @Unique
    private void junk$layout() {
        if (junk$btn == null) return;
        int center = this.x + this.backgroundWidth / 2;
        int rowsUp = (handler instanceof PlayerScreenHandler) ? 22 : 44;
        junk$btn.setPosition(center + 4, Math.max(2, this.y - rowsUp));
    }
}

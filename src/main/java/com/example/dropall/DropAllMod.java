package com.example.dropall;

import net.fabricmc.api.ClientModInitializer;

public class DropAllMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("DropAll loaded");
    }
}

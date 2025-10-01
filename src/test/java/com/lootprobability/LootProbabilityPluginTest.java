package com.lootprobability;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LootProbabilityPluginTest
{
    public static void main(String[] args) throws Exception
    {
        // Load our plugin
        ExternalPluginManager.loadBuiltin(LootProbabilityPlugin.class);

        // Start RuneLite
        RuneLite.main(args);
    }
}
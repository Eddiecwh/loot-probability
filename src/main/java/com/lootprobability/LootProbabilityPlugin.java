package com.lootprobability;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
        name = "Loot Probability"
)
public class LootProbabilityPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private LootProbabilityConfig config;

    // Track kills for Moss Giant
    private int mossGiantKills = 0;

    // Moss giant has a 1/128 drop rate for various items (like nature rune drops)
    // Using 1/128 as example drop rate
    private static final double MOSS_GIANT_DROP_RATE = 128.0;

    @Override
    protected void startUp() throws Exception
    {
        log.info("Loot Probability plugin started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.info("Loot Probability plugin stopped!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                    "Loot Probability plugin loaded!", null);
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath actorDeath)
    {
        // Check if the thing that died is an NPC
        if (actorDeath.getActor() instanceof NPC)
        {
            NPC npc = (NPC) actorDeath.getActor();
            String npcName = npc.getName();

            // Track Moss Giant kills
            if (npcName != null && npcName.equals("Moss giant"))
            {
                mossGiantKills++;

                // Calculate probability of having gotten the drop by now
                double chanceToHaveDrop = calculateProbability(MOSS_GIANT_DROP_RATE, mossGiantKills);

                // Calculate how unlucky you are (percentile)
                double dryPercentile = calculateDryPercentile(MOSS_GIANT_DROP_RATE, mossGiantKills);

                // Show message with probability
                String message = String.format(
                        "Moss Giant #%d | Chance for rare drop: %.2f%% | Drier than %.1f%% of players",
                        mossGiantKills,
                        chanceToHaveDrop * 100,
                        dryPercentile
                );

                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);

                log.info("Moss Giant killed! KC: {} | Probability: {}%",
                        mossGiantKills, chanceToHaveDrop * 100);
            }
        }
    }

    /**
     * Calculate the probability of getting at least 1 drop in X kills
     *
     * Formula: 1 - (1 - 1/dropRate)^kills
     *
     * Example: If drop rate is 1/128 and you have 128 kills:
     * - Chance per kill: 1/128 = 0.0078125 (0.78%)
     * - Chance of NOT getting it per kill: 1 - 0.0078125 = 0.9921875
     * - Chance of not getting it in 128 kills: 0.9921875^128 = 0.367
     * - Chance of getting it at least once: 1 - 0.367 = 0.633 (63.3%)
     */
    private double calculateProbability(double dropRate, int kills)
    {
        if (kills == 0)
        {
            return 0.0;
        }

        // Chance of getting the drop on a single kill
        double chancePerKill = 1.0 / dropRate;

        // Chance of NOT getting the drop on a single kill
        double chanceOfNotGetting = 1.0 - chancePerKill;

        // Chance of not getting it in X kills (multiply probability X times)
        double chanceOfNotGettingInXKills = Math.pow(chanceOfNotGetting, kills);

        // Chance of getting at least 1 drop = opposite of never getting it
        return 1.0 - chanceOfNotGettingInXKills;
    }

    /**
     * Calculate what percentile of dryness you're in
     *
     * This tells you: "X% of players would have gone this long without the drop"
     *
     * Example: If result is 5.0, that means only 5% of players go this dry
     * (you're in the unluckiest 5%)
     */
    private double calculateDryPercentile(double dropRate, int kills)
    {
        // This is just the chance of not getting the drop
        double chancePerKill = 1.0 / dropRate;
        double chanceOfBeingThisDry = Math.pow(1.0 - chancePerKill, kills);

        // Convert to percentage
        return chanceOfBeingThisDry * 100;
    }

    @Provides
    LootProbabilityConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(LootProbabilityConfig.class);
    }
}
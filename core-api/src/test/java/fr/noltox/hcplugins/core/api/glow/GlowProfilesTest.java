package fr.noltox.hcplugins.core.api.glow;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlowProfilesTest {

    @Test
    void generatedProfilesHaveUniqueIdsAndCarriers() {
        var profiles = GlowProfiles.all();
        assertFalse(profiles.isEmpty());
        assertEquals(profiles.size(), profiles.stream().map(GlowProfiles.Profile::id).collect(java.util.stream.Collectors.toSet()).size());
        assertEquals(profiles.size(), profiles.stream().map(profile -> profile.carrier().name()).collect(java.util.stream.Collectors.toSet()).size());
        assertEquals(profiles.size(), profiles.stream().map(profile -> profile.carrier().rgb()).collect(java.util.stream.Collectors.toSet()).size());
    }

    @Test
    void catalogResolvesProfilesAndReservedRgb() {
        var profile = GlowProfiles.find("rainbow").orElseThrow();
        assertEquals(GlowProfiles.EffectType.RAINBOW, profile.effectType());
        assertTrue(GlowProfiles.isReservedCarrierRgb(profile.carrier().rgb()));
        assertFalse(GlowProfiles.isReservedCarrierRgb(0x123456));
    }
}

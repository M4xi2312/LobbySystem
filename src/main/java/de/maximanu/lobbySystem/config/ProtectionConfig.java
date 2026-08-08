package de.maximanu.lobbySystem.config;

public record ProtectionConfig(
   boolean enabled,
   boolean damage,
   boolean hunger,
   boolean blockBreak,
   boolean blockPlace,
   boolean interact,
   boolean entityInteract,
   boolean inventory,
   boolean itemDrop,
   boolean itemPickup,
   boolean farmlandTrample,
   boolean mobSpawning,
   boolean portalUse,
   boolean buckets,
   boolean armorStandEdit,
   boolean itemFrameRotate,
   boolean hangingBreak,
   boolean manageGameRules,
   boolean weatherLock,
   boolean timeLock,
   long lockedTime
) {
}

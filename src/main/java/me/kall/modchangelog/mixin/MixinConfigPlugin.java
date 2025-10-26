package me.kall.modchangelog.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MixinConfigPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    static {
        try {
            Path dir = FMLPaths.GAMEDIR.get().resolve("modchangelog");
            Files.createDirectories(dir);

            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path currentPath = dir.resolve(timeStamp + ".txt");

            Path lastPath = getLastLogFile(dir, currentPath);

            writeReport(currentPath, getCurrentMods(), readLastMods(lastPath));
        } catch (Exception e) {
            LogManager.getLogger().error("Error checking mods", e);
        }
    }

    private static List<String> getCurrentMods() {
        return FMLLoader.getLoadingModList().getMods().stream()
                .map(info -> info.getModId() + "=" + info.getVersion().toString())
                .sorted()
                .collect(Collectors.toList());
    }

    private static List<String> readLastMods(Path path) throws IOException {
        if (path == null || !Files.exists(path)) return Collections.emptyList();

        List<String> lines = Files.readAllLines(path);
        int start = lines.indexOf("=== Current ModList ===");
        int next = lines.indexOf("=== Last ModList ===");

        if (start >= 0 && next > start) {
            return lines.subList(start + 1, next)
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static void writeReport(Path path, List<String> currentMods, List<String> lastMods) throws IOException {
        List<String> added = new ArrayList<>(currentMods);
        added.removeAll(lastMods);

        List<String> removed = new ArrayList<>(lastMods);
        removed.removeAll(currentMods);

        List<String> result = new ArrayList<>();
        result.add("=== Current ModList ===");
        result.addAll(currentMods);
        result.add("");
        result.add("=== Last ModList ===");
        result.addAll(lastMods);
        result.add("");
        result.add("=== Difference ===");
        if (added.isEmpty() && removed.isEmpty()) {
            result.add("ModList no change.");
        } else {
            if (!added.isEmpty()) {
                result.add("[Addition/Modification]");
                result.addAll(added);
            }
            if (!removed.isEmpty()) {
                result.add("[Removal]");
                result.addAll(removed);
            }
        }

        Files.write(path, result);
    }

    private static Path getLastLogFile(Path dir, Path currentPath) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> !p.equals(currentPath))
                    .filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .min((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .orElse(null);
        }
    }

    @Override public String getRefMapperConfig() {return "";}
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {return true;}
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() {return Collections.emptyList();}
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
package net.nekoyuni.SimpleEnemyMod.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.nekoyuni.SimpleEnemyMod.client.gui.overlay.CommanderOverlayRenderer;
import net.nekoyuni.SimpleEnemyMod.client.system.ClientGlowManager;
import net.nekoyuni.SimpleEnemyMod.entity.ai.goals.utils.SquadData;
import net.nekoyuni.SimpleEnemyMod.entity.ai.orders.OrderType;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.entity.unit.PmcUnitEntity;
import net.nekoyuni.SimpleEnemyMod.network.ModNetworking;
import net.nekoyuni.SimpleEnemyMod.network.packets.PacketIssueOrder;

import java.util.*;

public class CommanderMenuScreen extends Screen {

    private static final int COL_WIDTH = 100;
    private static final int ITEM_HEIGHT = 15;
    private static final int PADDING = 10;

    private static final int BG_COLOR = 0x90000000;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int HOVER_COLOR = 0xFFD700;
    private static final int SELECTED_COLOR = 0x00FF00;
    private static final int HEADER_COLOR = 0xFFA500;

    private final List<PmcUnitEntity> nearbyLeaders = new ArrayList<>();
    private final List<PmcUnitEntity> nearbyUnits = new ArrayList<>();
    private final Set<Integer> selectedEntityIds = new HashSet<>();

    private static final String[] MAIN_OPTIONS = {
            "Select Unit", "Select All",
            "Hold Position", "Follow Me",
            "Move To...", "Attack that...",
            "Cease Fire", "Free Fire",
            "F: Wedge", "F: Column"
    };

    private boolean showSubmenus = false;
    private static long lastOrderTime = 0;


    public CommanderMenuScreen() {
        super(Component.literal("Commander"));
    }

    @Override
    protected void init() {
        super.init();
        scanForUnits();
    }

    @Override
    public void removed() {

        ClientGlowManager.clear();

        super.removed();
    }

    private void scanForUnits() {
        nearbyLeaders.clear();
        nearbyUnits.clear();

        if (this.minecraft == null || this.minecraft.player == null) return;

        var player = this.minecraft.player;
        var level = player.level();


        List<PmcUnitEntity> allAllies = level.getEntitiesOfClass(
                PmcUnitEntity.class,
                player.getBoundingBox().inflate(64.0) // TODO Add this value to Config
        );

        for (PmcUnitEntity unit : allAllies) {
            if (!unit.isOwnedBy(player)) continue;

            if (isSquadLeader(unit)) {
                nearbyLeaders.add(unit);

            } else if (!SquadData.hasValidSquadData(unit)) {
                nearbyUnits.add(unit);
            }
        }

        nearbyLeaders.sort(Comparator.comparingInt(Entity::getId));
        nearbyUnits.sort(Comparator.comparingInt(Entity::getId));
    }


    private boolean isSquadLeader(PmcUnitEntity unit) {
        return unit.getRole() == UnitRole.FRIENDLY_SQUAD_LEADER;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // RENDER
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {

        int x1 = PADDING;
        int yStart = PADDING + 20;
        int h1 = (MAIN_OPTIONS.length * ITEM_HEIGHT) + (PADDING * 2);

        guiGraphics.fill(x1, yStart, x1 + COL_WIDTH, yStart + h1, BG_COLOR);
        guiGraphics.drawCenteredString(this.font, "COMMANDER", x1 + (COL_WIDTH/2), yStart + 2, HEADER_COLOR);

        for (int i = 0; i < MAIN_OPTIONS.length; i++) {

            int y = yStart + 15 + (i * ITEM_HEIGHT);
            boolean isHover = isMouseOver(mouseX, mouseY, x1, y, COL_WIDTH, ITEM_HEIGHT);

            int color = isHover ? HOVER_COLOR : TEXT_COLOR;
            if (i == 0 && showSubmenus) color = HOVER_COLOR;

            String textToDisplay = MAIN_OPTIONS[i];

            if (i == 1) {
                textToDisplay = areAllSelected() ? "Deselect All" : "Select All";
                if (areAllSelected()) color = 0xAAAAAA;
            }

            guiGraphics.drawString(this.font, textToDisplay, x1 + 5, y + 4, color, false);
        }

        // SUBMENUS
        if (showSubmenus) {
            int x2 = x1 + COL_WIDTH + 5; // Posición Columna 2 (Squads)
            int x3 = x2 + COL_WIDTH + 5; // Posición Columna 3 (Units)

            if (!nearbyLeaders.isEmpty()) {
                renderUnitColumn(guiGraphics, mouseX, mouseY, x2, yStart, "SQUADS", nearbyLeaders, true);
            }

            int unitsX = nearbyLeaders.isEmpty() ? x2 : x3;

            if (!nearbyUnits.isEmpty()) {
                renderUnitColumn(guiGraphics, mouseX, mouseY, unitsX, yStart, "UNITS", nearbyUnits, false);
            }
        }
    }


    private void renderUnitColumn(GuiGraphics g, int mx, int my, int x, int yStart,
                                  String title, List<PmcUnitEntity> units, boolean isSquad) {

        int contentHeight = (units.size() * ITEM_HEIGHT) + (PADDING * 2);

        // Background
        g.fill(x, yStart, x + COL_WIDTH, yStart + contentHeight, BG_COLOR);
        g.drawCenteredString(this.font, title, x + (COL_WIDTH/2), yStart + 2, HEADER_COLOR);

        // List
        for (int i = 0; i < units.size(); i++) {
            PmcUnitEntity unit = units.get(i);

            int y = yStart + 15 + (i * ITEM_HEIGHT);

            boolean isHover = isMouseOver(mx, my, x, y, COL_WIDTH, ITEM_HEIGHT);
            boolean isSelected = selectedEntityIds.contains(unit.getId());

            int color = isSelected ? SELECTED_COLOR : (isHover ? HOVER_COLOR : TEXT_COLOR);

            String text = isSquad ? "SQUAD [" + (i+1) + "]" : "[" + (i+1) + "] Unit";
            if (isSelected) text = "> " + text;

            g.drawString(this.font, text, x + 5, y + 4, color, false);
        }
    }

    private boolean isMouseOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // INTERACTION
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int x1 = PADDING;
        int yStart = PADDING + 20;

        for (int i = 0; i < MAIN_OPTIONS.length; i++) {
            int y = yStart + 15 + (i * ITEM_HEIGHT);

            if (isMouseOver(mouseX, mouseY, x1, y, COL_WIDTH, ITEM_HEIGHT)) {
                handleMainAction(i);
                return true;
            }
        }

        if (showSubmenus) {
            int x2 = x1 + COL_WIDTH + 5;
            int x3 = x2 + COL_WIDTH + 5;

            int unitsX = nearbyLeaders.isEmpty() ? x2 : x3;

            if (!nearbyLeaders.isEmpty()) {
                checkUnitListClick(mouseX, mouseY, x2, yStart, nearbyLeaders);
            }

            if (!nearbyUnits.isEmpty()) {
                checkUnitListClick(mouseX, mouseY, unitsX, yStart, nearbyUnits);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void checkUnitListClick(double mx, double my, int x, int yStart, List<PmcUnitEntity> units) {

        for (int i = 0; i < units.size(); i++) {

            int y = yStart + 15 + (i * ITEM_HEIGHT);

            if (isMouseOver(mx, my, x, y, COL_WIDTH, ITEM_HEIGHT)) {

                int id = units.get(i).getId();

                if (selectedEntityIds.contains(id)) {

                    selectedEntityIds.remove(id);
                    ClientGlowManager.removeEntity(id);

                } else {

                    selectedEntityIds.add(id);
                    ClientGlowManager.addEntity(id);

                }

                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                );
            }
        }
    }

    private void handleMainAction(int index) {
        switch (index) {
            case 0: // Select Unit (Toggle Submenus)
                this.showSubmenus = !this.showSubmenus;
                break;

            case 1: // SelectAll - DeselectAll
                toggleSelectAll();
                break;

            case 2: // Hold Position
                issueOrderToSelected(OrderType.HOLD_POSITION, -1);
                break;

            case 3: // Follow Me
                issueOrderToSelected(OrderType.FOLLOW_COMMANDER, -1);
                break;

            case 4: // Move To
                if (selectedEntityIds.isEmpty()) {
                    sendMessage("§cSelect at least one unit first!");

                } else {

                    CommanderOverlayRenderer.isSelectingPosition = true;
                    CommanderOverlayRenderer.selectedUnitsSnapshot = new HashSet<>(this.selectedEntityIds);

                    this.onClose();
                    sendMessage("§aSelect position...");
                }
                break;

            case 5: // Attack that
                if (selectedEntityIds.isEmpty()) {
                    sendMessage("§cSelect at least one unit first!");

                } else {

                    CommanderOverlayRenderer.isSelectingTarget = true;
                    CommanderOverlayRenderer.selectedUnitsSnapshot = new HashSet<>(this.selectedEntityIds);

                    this.onClose();
                    sendMessage("§aSelect a target...");
                }
                break;

            case 6: // Cease Fire
                issueOrderToSelected(OrderType.CEASE_FIRE, -1);
                break;

            case 7: // Free Fire
                issueOrderToSelected(OrderType.FREE_FIRE, -1);
                break;

            case 8: // Formation
                issueOrderToSelected(OrderType.FORM_WEDGE, -1);
                break;

            case 9: // Formation
                issueOrderToSelected(OrderType.FORM_COLUMN, -1);
                break;

        }
    }

    // TACZ FIX
    public static boolean shouldSuppressFire() {
        return System.currentTimeMillis() - lastOrderTime < 200;
    }

    private void issueOrderToSelected(OrderType order, int targetId) {
        if (selectedEntityIds.isEmpty()) {
            sendMessage("§cNo units selected!");
            return;
        }

        // 1. Convertir Set a List para poder ordenar
        List<Integer> sortedIds = new ArrayList<>(selectedEntityIds);
        sortedIds.sort((id1, id2) -> Integer.compare(id2, id1));

        // TACZ FIX
        lastOrderTime = System.currentTimeMillis();

        int count = 0;
        for (int i = 0; i < sortedIds.size(); i++) {
            int unitId = sortedIds.get(i);
            int newIndex = i;

            ModNetworking.sendToServer(new PacketIssueOrder(unitId, order, Vec3.ZERO, newIndex, targetId));
            count++;
        }

        sendMessage("§eOrder sent to " + count + " units.");
        this.onClose();
    }

    private void sendMessage(String msg) {
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(msg), true);
        }
    }

    private void toggleSelectAll() {

        int totalUnits = nearbyLeaders.size() + nearbyUnits.size();

        // Si todo está seleccionado → deseleccionar todo
        if (selectedEntityIds.size() >= totalUnits && totalUnits > 0) {

            selectedEntityIds.clear();
            ClientGlowManager.clear();

        } else {

            // Seleccionar todos los líderes
            for (PmcUnitEntity leader : nearbyLeaders) {

                int id = leader.getId();

                selectedEntityIds.add(id);
                ClientGlowManager.addEntity(id);
            }

            // Seleccionar todas las unidades
            for (PmcUnitEntity unit : nearbyUnits) {

                int id = unit.getId();

                selectedEntityIds.add(id);
                ClientGlowManager.addEntity(id);
            }
        }

        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }

    private boolean areAllSelected() {
        int totalUnits = nearbyLeaders.size() + nearbyUnits.size();
        return totalUnits > 0 && selectedEntityIds.size() >= totalUnits;
    }



}

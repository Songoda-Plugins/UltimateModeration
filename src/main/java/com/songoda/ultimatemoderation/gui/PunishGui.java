package com.songoda.ultimatemoderation.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.AnvilGui;
import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.utils.ItemUtils;
import com.songoda.core.utils.TextUtils;
import com.songoda.core.utils.TimeUtils;
import com.songoda.ultimatemoderation.UltimateModeration;
import com.songoda.ultimatemoderation.punish.Punishment;
import com.songoda.ultimatemoderation.punish.PunishmentType;
import com.songoda.ultimatemoderation.punish.template.Template;
import com.songoda.ultimatemoderation.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class PunishGui extends Gui {
    private final UltimateModeration plugin;
    private final Player player;
    private final OfflinePlayer toModerate;

    private Template template;
    private boolean justSaved = false;

    private PunishmentType type = PunishmentType.BAN;
    private long duration = -1;
    private String reason = null;

    private String templateName = null;

    private int task;

    public PunishGui(UltimateModeration plugin, OfflinePlayer toModerate, Template template, Player player) {
        super(5);
        setDefaultItem(null);
        this.player = player;
        this.plugin = plugin;
        this.toModerate = toModerate;
        if (template != null) {
            this.template = template;
            this.type = template.getPunishmentType();
            this.duration = template.getDuration();
            this.reason = template.getReason();
            this.templateName = template.getName();
        }

        setTitle(toModerate == null ? plugin.getLocale().getMessage("gui.punish.title.template").getMessage()
                : plugin.getLocale().getMessage("gui.punish.title")
                .processPlaceholder("toModerate", toModerate.getName()).getMessage());
        if (toModerate != null) {
            runTask();
        }

        setOnClose((event) -> Bukkit.getScheduler().cancelTask(this.task));
        paint();
    }

    public void paint() {
        if (this.inventory != null) {
            this.inventory.clear();
        }
        setActionForRange(0, 53, null);

        // decorate the edges
        ItemStack glass2 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_2.getMaterial(CompatibleMaterial.BLUE_STAINED_GLASS_PANE));
        ItemStack glass3 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_3.getMaterial(CompatibleMaterial.LIGHT_BLUE_STAINED_GLASS_PANE));

        // edges will be type 3
        mirrorFill(0, 2, true, true, glass3);
        mirrorFill(1, 1, true, true, glass3);

        // decorate corners with type 2
        mirrorFill(0, 0, true, true, glass2);
        mirrorFill(1, 0, true, true, glass2);
        mirrorFill(2, 0, false, true, glass2);
        mirrorFill(0, 1, true, true, glass2);

        if (this.toModerate != null) {
            setItem(13, GuiUtils.createButtonItem(ItemUtils.getPlayerSkull(this.toModerate),
                    TextUtils.formatText("&6&l" + this.toModerate.getName())));
        }

        if (this.player.hasPermission("um." + this.type.toString().toLowerCase())) {
            setButton(22, GuiUtils.createButtonItem(CompatibleMaterial.EMERALD_BLOCK,
                            this.plugin.getLocale().getMessage("gui.punish.submit").getMessage()),
                    (event) -> {
                        if (!this.player.hasPermission("um." + this.type.toString().toLowerCase())) {
                            return;
                        }
                        if (this.duration == -1 && this.type == PunishmentType.BAN && !this.player.hasPermission("um.ban.permanent")) {
                            return;
                        }

                        if (this.toModerate == null) {
                            if (this.reason == null || this.templateName == null) {
                                return;
                            }

                            if (this.template == null) {
                                finishTemplate();
                            } else {
                                updateTemplate();
                            }
                            return;
                        }


                        switch (this.type) {
                            case BAN:
                            case MUTE:
                            case WARNING:
                                new Punishment(this.type, this.duration, this.reason).execute(this.player, this.toModerate);
                                break;
                            case KICK:
                                new Punishment(this.type, this.reason).execute(this.player, this.toModerate);
                                break;
                        }
                        this.player.closeInventory();
                    });
        }

        setButton(8, GuiUtils.createButtonItem(CompatibleMaterial.OAK_DOOR,
                        this.plugin.getLocale().getMessage("gui.general.back").getMessage()),
                (event) -> {
                    if (this.toModerate != null) {
                        this.guiManager.showGUI(this.player, new PlayerGui(this.plugin, this.toModerate, this.player));
                    } else {
                        this.guiManager.showGUI(this.player, new TemplateManagerGui(this.plugin, this.player));
                    }
                });

        setButton(28, GuiUtils.createButtonItem(CompatibleMaterial.ANVIL,
                        this.plugin.getLocale().getMessage("gui.punish.type.punishment").getMessage(),
                        TextUtils.formatText("&7" + this.type.getTranslation()),
                        "",
                        this.plugin.getLocale().getMessage("gui.punish.type.punishment.click").getMessage()),
                (event) -> {
                    this.type = this.type.next();
                    this.justSaved = false;
                    paint();
                });

        ItemStack templateItem = this.toModerate != null ? GuiUtils.createButtonItem(CompatibleMaterial.MAP,
                this.plugin.getLocale().getMessage("gui.punish.type.template").getMessage(),
                this.plugin.getLocale().getMessage("gui.punish.type.template.current")
                        .processPlaceholder("template",
                                this.template == null
                                        ? this.plugin.getLocale().getMessage("gui.general.none").getMessage()
                                        : this.template.getName()).getMessage(),
                "",
                this.plugin.getLocale().getMessage(this.plugin.getTemplateManager().getTemplates().size() == 0
                        ? "gui.punish.type.template.none"
                        : "gui.punish.type.template.click").getMessage())
                : GuiUtils.createButtonItem(CompatibleMaterial.MAP,
                this.plugin.getLocale().getMessage("gui.punish.type.name").getMessage(),
                this.plugin.getLocale().getMessage("gui.punish.type.name.current")
                        .processPlaceholder("name",
                                this.templateName == null
                                        ? this.plugin.getLocale().getMessage("gui.punish.type.name.current").getMessage()
                                        : this.templateName).getMessage(),
                "",
                this.plugin.getLocale().getMessage("gui.punish.type.name.current.click").getMessage());

        setButton(30, templateItem, (event) -> {
            if (this.toModerate == null) {
                nameTemplate();
                return;
            }
            if (this.plugin.getTemplateManager().getTemplates().size() == 0) {
                return;
            }

            if (this.player.hasPermission("um.templates.use")) {
                this.guiManager.showGUI(this.player, new TemplateSelectorGui(this.plugin, this, this.player));
            }
        });

        if (this.type != PunishmentType.KICK) {
            setButton(32, GuiUtils.createButtonItem(CompatibleMaterial.CLOCK,
                            this.plugin.getLocale().getMessage("gui.punish.type.duration").getMessage(),
                            this.plugin.getLocale().getMessage("gui.punish.type.duration.leftclick").getMessage(),
                            this.plugin.getLocale().getMessage("gui.punish.type.duration.rightclick").getMessage(),
                            "",
                            this.plugin.getLocale().getMessage("gui.punish.type.duration.current").getMessage(),
                            TextUtils.formatText("&6" + (this.duration == -1 ? this.plugin.getLocale().getMessage("gui.general.permanent").getMessage()
                                    : TimeUtils.makeReadable(this.duration)))),
                    (event) -> {
                        if (this.type == PunishmentType.KICK) {
                            return;
                        }
                        if (event.clickType == ClickType.LEFT) {
                            AnvilGui gui = new AnvilGui(this.player, this);
                            gui.setAction(evt -> {
                                this.duration = TimeUtils.parseTime(gui.getInputText());
                                this.justSaved = false;
                                this.guiManager.showGUI(this.player, this);
                                paint();
                            });

                            ItemStack item = new ItemStack(Material.PAPER);
                            ItemMeta meta = item.getItemMeta();

                            meta.setDisplayName(this.duration == -1 || this.duration == 0 ? "1d 1h 1m" : TimeUtils.makeReadable(this.duration));
                            item.setItemMeta(meta);

                            gui.setInput(item);
                            this.guiManager.showGUI(this.player, gui);
                        } else {
                            this.duration = -1;
                            paint();
                        }
                    });
        }

        setButton(34, GuiUtils.createButtonItem(CompatibleMaterial.PAPER,
                this.plugin.getLocale().getMessage("gui.punish.type.reason").getMessage(),
                this.plugin.getLocale().getMessage("gui.punish.type.reason.click").getMessage(),
                "",
                this.plugin.getLocale().getMessage("gui.punish.type.reason.current").getMessage(),
                TextUtils.formatText("&6" + this.reason)), (event) -> {

            AnvilGui gui = new AnvilGui(this.player, this);
            gui.setAction(evnt -> {
                this.reason = gui.getInputText();
                this.justSaved = false;
                this.guiManager.showGUI(this.player, this);
                paint();
            });

            ItemStack item = GuiUtils.createButtonItem(CompatibleMaterial.PAPER,
                    this.reason == null ? this.plugin.getLocale().getMessage("gui.general.reason").getMessage() : this.reason);

            gui.setInput(item);
            this.guiManager.showGUI(this.player, gui);
        });
    }

    private void notifyTemplate() {
        if (this.reason == null || (this.justSaved && this.template != null)) {
            this.inventory.setItem(4, null);
            return;
        }

        CompatibleMaterial material = CompatibleMaterial.WHITE_WOOL;
        String name = this.plugin.getLocale().getMessage("gui.punish.template.create").getMessage();
        ArrayList<String> lore = new ArrayList<>();
        lore.add(this.plugin.getLocale().getMessage("gui.punish.template.create2").getMessage());

        if (!this.justSaved && this.template != null) {
            name = this.plugin.getLocale().getMessage("gui.punish.template.leftclick").getMessage();
            lore.clear();
            lore.add(this.plugin.getLocale().getMessage("gui.punish.template.leftclick2")
                    .processPlaceholder("template", this.template.getName()).getMessage());
            lore.add("");
            lore.add(this.plugin.getLocale().getMessage("gui.punish.template.rightclick").getMessage());
        }

        if (getItem(4) != null && CompatibleMaterial.getMaterial(getItem(4)) == CompatibleMaterial.WHITE_WOOL) {
            material = CompatibleMaterial.YELLOW_WOOL;
        }

        setButton(4, GuiUtils.createButtonItem(material, name, lore), (event) -> {

            if (this.reason == null || this.duration == 0) {
                return;
            }

            if (this.template != null && event.clickType == ClickType.LEFT) {
                updateTemplate();
                return;
            }
            nameTemplate();
        });
    }

    public void runTask() {
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, this::notifyTemplate, 10L, 10L);
    }

    private void nameTemplate() {
        AnvilGui gui = new AnvilGui(this.player, this);
        gui.setAction(event -> {
            this.templateName = gui.getInputText();

            if (this.reason != null && this.templateName != null) {
                if (this.template == null) {
                    finishTemplate();
                } else {
                    updateTemplate();
                }
            }

            this.justSaved = true;
            this.guiManager.showGUI(this.player, this);
            paint();
        });

        ItemStack item = GuiUtils.createButtonItem(CompatibleMaterial.PAPER,
                this.template == null ? this.plugin.getLocale().getMessage("gui.general.templatename").getMessage() : this.template.getName());

        gui.setInput(item);
        this.guiManager.showGUI(this.player, gui);
    }

    private void updateTemplate() {
        Template template = new Template(this.type, this.duration, this.reason, this.template.getCreator(), this.templateName);
        this.plugin.getTemplateManager().removeTemplate(this.template);
        this.plugin.getTemplateManager().addTemplate(template);
        this.plugin.getDataManager().deleteTemplate(this.template);
        this.plugin.getDataManager().createTemplate(template);
        this.justSaved = true;
        if (this.toModerate == null) {
            this.guiManager.showGUI(this.player, new TemplateManagerGui(this.plugin, this.player));
        }
    }

    private void finishTemplate() {
        Template template = new Template(this.type, this.duration, this.reason, this.player, this.templateName);
        this.plugin.getTemplateManager().addTemplate(template);
        this.plugin.getDataManager().createTemplate(template);
        this.template = template;
        if (this.toModerate == null) {
            this.guiManager.showGUI(this.player, new TemplateManagerGui(this.plugin, this.player));
        }
    }

    public void setTemplate(Template template) {
        this.justSaved = true;
        this.template = template;
    }

    public void setType(PunishmentType type) {
        this.type = type;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

package me.chancesd.sdutils.display;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

public class BossBarBuilder {

	private String barTitle = "";
	private BarColor barColor;
	private BarStyle barStyle;
	private BarFlag[] barFlags;

	public static BossBarBuilder create() {
		return new BossBarBuilder();
	}

	public BossBarBuilder title(final String title) {
		this.barTitle = title;
		return this;
	}

	public BossBarBuilder barColor(final BarColor color) {
		this.barColor = color;
		return this;
	}

	public BossBarBuilder barStyle(final BarStyle style) {
		this.barStyle = style;
		return this;
	}

	public BossBarBuilder barFlags(final BarFlag... flags) {
		this.barFlags = flags;
		return this;
	}

	public BossBar build() {
		if (barFlags != null)
			return Bukkit.createBossBar(barTitle, barColor, barStyle, barFlags);
		return Bukkit.createBossBar(barTitle, barColor, barStyle);
	}
}

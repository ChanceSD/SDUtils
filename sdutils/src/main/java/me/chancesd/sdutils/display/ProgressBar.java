package me.chancesd.sdutils.display;

import com.google.common.base.Strings;

import me.chancesd.sdutils.utils.Utils;

public class ProgressBar {

	private final int totalBars;
	private final String symbol;
	private final String originalMessage;
	private String message;
	private long goal;
	private double progress;
	private boolean dirty = true;

	public ProgressBar(final String message, final int totalBars, final long goal, final String symbol) {
		this.originalMessage = message;
		this.totalBars = totalBars;
		this.goal = goal;
		this.symbol = symbol;
	}

	public ProgressBar(final String message, final int totalBars, final long goal, final String symbol, final double secondsPassed) {
		this(message, totalBars, goal, symbol);
		setProgress(secondsPassed);
		calculate();
	}

	public ProgressBar setProgress(final double progress) {
		if (this.progress == progress)
			return this;
		this.progress = progress;
		this.dirty = true;
		return this;
	}

	public ProgressBar setGoal(final long goal) {
		if (this.goal == goal)
			return this;
		this.goal = goal;
		this.dirty = true;
		return this;
	}

	public ProgressBar calculate() {
		if (!dirty)
			return this;
		final double percent = progress / goal;
		final int progressBars = (int) (totalBars * percent);
		message = originalMessage.replace("<barsLeft>", Strings.repeat(symbol, totalBars - progressBars))
				.replace("<barsPassed>", Strings.repeat(symbol, progressBars))
				.replace("<time>", Double.toString(Utils.roundTo1Decimal(goal - progress)));
		this.dirty = false;
		return this;
	}

	public double getProgress() {
		return progress;
	}

	public String getMessage() {
		return message;
	}

	public int getTotalBars() {
		return totalBars;
	}

	public String getSymbol() {
		return symbol;
	}

	public long getGoal() {
		return goal;
	}

	@Override
	public String toString() {
		return message;
	}

}

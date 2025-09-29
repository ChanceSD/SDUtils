package me.chancesd.sdutils.display.chat;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.chancesd.sdutils.utils.ChatUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a line in a chat menu with optional click and hover actions
 * Supports both simple single-component lines and complex multi-component lines
 */
public class ChatLine {

	private final List<Component> components;

	public ChatLine(@NotNull final String text, @Nullable final ClickAction clickAction, @Nullable final HoverAction hoverAction) {
		this.components = new ArrayList<>();
		this.components.add(new Component(text, clickAction, hoverAction));
	}

	public ChatLine(@NotNull final String text) {
		this(text, null, null);
	}

	// Constructor for multi-component lines
	private ChatLine(@NotNull final List<Component> components) {
		this.components = new ArrayList<>(components);
	}

	private ComponentBuilder toComponentBuilder() {
		final ComponentBuilder mainComponent = new ComponentBuilder();

		for (final Component comp : components) {
			final TextComponent textComp = new TextComponent(ChatUtils.colorizeComponent(comp.text));

			if (comp.clickAction != null) {
				textComp.setClickEvent(new ClickEvent(comp.clickAction.type, comp.clickAction.value));
			}

			if (comp.hoverAction != null) {
				textComp.setHoverEvent(new HoverEvent(comp.hoverAction.type,
						new Text(ChatUtils.colorizeComponent(comp.hoverAction.value))));
			}

			mainComponent.append(textComp);
		}
		return mainComponent;
	}

	public BaseComponent toComponent() {
		return toComponentBuilder().build();
	}

	public BaseComponent[] toComponentArray() {
		return toComponentBuilder().create();
	}

	/**
	 * Creates a builder for complex multi-component chat lines
	 */
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final List<Component> components = new ArrayList<>();

		/**
		 * Add a text component with click and hover actions
		 */
		public Builder add(@NotNull final String text, @Nullable final ClickAction clickAction, @Nullable final HoverAction hoverAction) {
			components.add(new Component(text, clickAction, hoverAction));
			return this;
		}

		/**
		 * Add a simple text component without actions
		 */
		public Builder add(@NotNull final String text) {
			return add(text, null, null);
		}

		/**
		 * Add a clickable text component
		 */
		public Builder addClickable(@NotNull final String text, @NotNull final ClickAction clickAction, @Nullable final HoverAction hoverAction) {
			return add(text, clickAction, hoverAction);
		}

		/**
		 * Add a clickable text component with hover text
		 */
		public Builder addClickable(@NotNull final String text, @NotNull final ClickAction clickAction, @NotNull final String hoverText) {
			return add(text, clickAction, HoverAction.showText(hoverText));
		}

		public ChatLine build() {
			return new ChatLine(components);
		}
	}

	private static class Component {
		private final String text;
		private final ClickAction clickAction;
		private final HoverAction hoverAction;

		public Component(@NotNull final String text, @Nullable final ClickAction clickAction, @Nullable final HoverAction hoverAction) {
			this.text = text;
			this.clickAction = clickAction;
			this.hoverAction = hoverAction;
		}
	}

	public static class ClickAction {
		private final ClickEvent.Action type;
		private final String value;

		public ClickAction(final ClickEvent.Action type, final String value) {
			this.type = type;
			this.value = value;
		}

		public static ClickAction runCommand(final String command) {
			return new ClickAction(ClickEvent.Action.RUN_COMMAND, command);
		}

		public static ClickAction suggestCommand(final String command) {
			return new ClickAction(ClickEvent.Action.SUGGEST_COMMAND, command);
		}

		public static ClickAction openUrl(final String url) {
			return new ClickAction(ClickEvent.Action.OPEN_URL, url);
		}
	}

	public static class HoverAction {
		private final HoverEvent.Action type;
		private final String value;

		public HoverAction(final HoverEvent.Action type, final String value) {
			this.type = type;
			this.value = value;
		}

		public static HoverAction showText(final String text) {
			return new HoverAction(HoverEvent.Action.SHOW_TEXT, text);
		}
	}

	/**
	 * Get the plain text content of this ChatLine (without formatting or actions)
	 * @return The combined text of all components
	 */
	public String getPlainText() {
		final StringBuilder result = new StringBuilder();
		for (final Component comp : components) {
			result.append(comp.text);
		}
		return result.toString();
	}
}

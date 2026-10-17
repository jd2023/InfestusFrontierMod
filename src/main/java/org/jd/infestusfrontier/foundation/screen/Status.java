package org.jd.infestusfrontier.foundation.screen;

import net.minecraft.network.chat.Component;
import org.jd.infestusfrontier.ui.StatusKind;

/** What an organ is doing, as its status line shows it: one glyph and a few words. */
public record Status(StatusKind kind, Component text) {}

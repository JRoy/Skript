/*
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * Copyright 2011, 2012 Peter Güttinger
 * 
 */

package ch.njol.skript.util;

import java.util.ArrayList;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.api.Debuggable;
import ch.njol.skript.api.exception.ParseException;
import ch.njol.skript.lang.ExprParser;
import ch.njol.skript.lang.SimpleVariable;
import ch.njol.skript.lang.Variable;

/**
 * 
 * represents a string that may contain variables.
 * 
 * @author Peter Güttinger
 * 
 */
public class VariableString implements Debuggable {
	private final ArrayList<Object> string = new ArrayList<Object>();
	private Event last = null;
	private String lastString = null;
	private final boolean isSimple;
	
	public VariableString(final String s) throws ParseException {
		if (!s.contains("%")) {
			lastString = s;
			isSimple = true;
			return;
		}
		int c = s.indexOf('%');
		string.add(s.substring(0, c));
		while (c != s.length()) {
			final int c2 = s.indexOf('%', c + 1);
			if (c2 == -1) {
				Skript.error("The percent sign is used for variables (e.g. %player%). To insert a %, type it twice: %%. (found in \"" + s + "\")");
				isSimple = true;
				return;
			}
			if (c + 1 == c2) {
				string.add("%");
			} else {
				final Variable<?> var = (Variable<?>) ExprParser.parse(s.substring(c + 1, c2), Skript.getVariables().iterator(), false);
				if (var == null) {
					throw new ParseException("can't understand the variable %" + s.substring(c + 1, c2) + "%");
				} else {
					string.add(var);
				}
			}
			c = s.indexOf('%', c2 + 1);
			if (c == -1)
				c = s.length();
			string.add(s.substring(c2 + 1, c));
		}
		isSimple = false;
	}
	
	public static VariableString[] makeStrings(final String[] args) {
		final VariableString[] strings = new VariableString[args.length];
		for (int i = 0; i < args.length; i++) {
			try {
				strings[i] = new VariableString(args[i]);
			} catch (final ParseException e) {
				Skript.error(e.getError());
			}
		}
		return strings;
	}
	
	public static VariableString[] makeStringsFromQuoted(final String[] args) {
		final VariableString[] strings = new VariableString[args.length];
		for (int i = 0; i < args.length; i++) {
			try {
				strings[i] = new VariableString(args[i].substring(1, args[i].length() - 1));
			} catch (final ParseException e) {
				Skript.error(e.getError());
			}
		}
		return strings;
	}
	
	/**
	 * Parses all variables in the string and returns it. The returned string is cached as long as this method is always called with the same event argument.
	 * 
	 * @param e Event to pass to the variables.
	 * @return The input string with all variables replaced.
	 */
	public String get(final Event e) {
		if (isSimple || last == e)
			return lastString;
		final StringBuilder b = new StringBuilder();
		for (final Object o : string) {
			if (o instanceof Variable<?>) {
				if (((Variable<?>) o).isSingle())
					b.append(Skript.toString(((Variable<?>) o).getSingle(e)));
				else
					b.append(Skript.toString(((Variable<?>) o).getArray(e), ((SimpleVariable<?>) o).getAnd()));
			} else {
				b.append(o);
			}
		}
		last = e;
		return lastString = b.toString();
	}
	
	@Override
	public String getDebugMessage(final Event e) {
		if (isSimple)
			return '"' + lastString + '"';
		if (e != null)
			return '"' + get(e) + '"';
		final StringBuilder b = new StringBuilder("\"");
		for (final Object o : string) {
			if (o instanceof Variable) {
				b.append("%" + ((Variable<?>) o).getDebugMessage(e) + "%");
			} else {
				b.append(o);
			}
		}
		b.append('"');
		return b.toString();
	}
	
	public boolean isSimple() {
		return isSimple;
	}
	
}

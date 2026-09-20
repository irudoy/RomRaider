/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2026 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.logger.external.udp.plugin;

import java.util.Stack;

import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;

/**
 * Integer arithmetic an ECU applies to a value and the expression parser
 * lacks: <code>floor(x)</code>, <code>round(x)</code>, <code>min(a, b)</code>
 * and <code>max(a, b)</code>.
 */
final class UdpFunctions {

    private UdpFunctions() {
    }

    static void addTo(JEP parser) {
        parser.addFunction("floor", new Unary() {
            double apply(double x) {
                return Math.floor(x);
            }
        });
        parser.addFunction("round", new Unary() {
            double apply(double x) {
                return Math.floor(x + 0.5);
            }
        });
        parser.addFunction("min", new Binary() {
            double apply(double a, double b) {
                return Math.min(a, b);
            }
        });
        parser.addFunction("max", new Binary() {
            double apply(double a, double b) {
                return Math.max(a, b);
            }
        });
    }

    private static double pop(Stack<Object> stack) throws ParseException {
        final Object value = stack.pop();
        if (value instanceof Number) return ((Number) value).doubleValue();
        throw new ParseException("Invalid parameter type");
    }

    private abstract static class Unary extends PostfixMathCommand {
        Unary() {
            numberOfParameters = 1;
        }

        abstract double apply(double x);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void run(Stack stack) throws ParseException {
            checkStack(stack);
            stack.push(Double.valueOf(apply(pop(stack))));
        }
    }

    private abstract static class Binary extends PostfixMathCommand {
        Binary() {
            numberOfParameters = 2;
        }

        abstract double apply(double a, double b);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void run(Stack stack) throws ParseException {
            checkStack(stack);
            final double b = pop(stack);
            final double a = pop(stack);
            stack.push(Double.valueOf(apply(a, b)));
        }
    }
}

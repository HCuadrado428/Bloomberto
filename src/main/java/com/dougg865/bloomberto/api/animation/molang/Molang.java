package com.dougg865.bloomberto.api.animation.molang;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A small Molang interpreter, the expression language of Blockbench / Bedrock animations: arithmetic, comparisons,
 * {@code ?:}, {@code &&}, {@code ||}, {@code math.*} functions (angles in degrees), {@code query.*} and {@code variable.*}
 * reads, and {@code ;}-separated statements with {@code variable.x = ...} assignments and {@code return}.
 *
 * <pre>{@code
 * Molang m = Molang.compile("math.sin(query.anim_time * 90) * 15");
 * double v = m.eval(new MolangContext().set("query.anim_time", 0.5));
 * }</pre>
 *
 * Compile once, evaluate every frame. Not supported: arrays, structs, loops, {@code ??}, resource-reference queries.
 */
public final class Molang {
    private interface Node {
        double eval(MolangContext ctx);
    }

    private final Node root;
    private final String source;

    private Molang(String source, Node root) {
        this.source = source;
        this.root = root;
    }

    public static Molang compile(String expression) throws MolangException {
        Parser p = new Parser(expression);
        Node n = p.parseProgram();
        return new Molang(expression, n);
    }

    /** Parses {@code text} as a plain number if it is one, or as an expression otherwise. */
    public static boolean isNumber(String text) {
        try {
            Double.parseDouble(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public double eval(MolangContext context) {
        return root.eval(context);
    }

    public String source() {
        return source;
    }

    @Override
    public String toString() {
        return source;
    }

    /** Normalizes aliases ({@code q.}, {@code v.}, {@code t.}, {@code m.}, {@code c.}) and case. */
    static String canonical(String name) {
        String n = name.trim().toLowerCase(Locale.ROOT);
        int dot = n.indexOf('.');
        if (dot < 0) {
            return n;
        }
        String prefix = n.substring(0, dot);
        String rest = n.substring(dot);
        return switch (prefix) {
            case "q" -> "query" + rest;
            case "v" -> "variable" + rest;
            case "t" -> "temp" + rest;
            case "m" -> "math" + rest;
            case "c" -> "context" + rest;
            default -> n;
        };
    }

    // ---- parser ----

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        Node parseProgram() throws MolangException {
            List<Node> statements = new ArrayList<>();
            skip();
            while (pos < s.length()) {
                if (peek() == ';') {
                    pos++;
                    skip();
                    continue;
                }
                statements.add(parseStatement());
                skip();
                if (pos < s.length()) {
                    if (peek() != ';') {
                        throw error("Unexpected '" + peek() + "'");
                    }
                }
            }
            if (statements.isEmpty()) {
                throw new MolangException("Empty expression");
            }
            return ctx -> {
                double result = 0;
                for (Node st : statements) {
                    result = st.eval(ctx);
                    if (st instanceof ReturnNode) {
                        return result;
                    }
                }
                return result;
            };
        }

        private record ReturnNode(Node value) implements Node {
            @Override
            public double eval(MolangContext ctx) {
                return value.eval(ctx);
            }
        }

        private Node parseStatement() throws MolangException {
            skip();
            int save = pos;
            String word = readName();
            if (word != null && word.equals("return")) {
                return new ReturnNode(parseTernary());
            }
            if (word != null && (word.startsWith("variable.") || word.startsWith("v.") || word.startsWith("temp.")
                    || word.startsWith("t."))) {
                skip();
                if (peek() == '=' && peekAt(1) != '=') {
                    pos++;
                    Node value = parseTernary();
                    String name = canonical(word);
                    return ctx -> {
                        double v = value.eval(ctx);
                        ctx.set(name, v);
                        return v;
                    };
                }
            }
            pos = save;
            return parseTernary();
        }

        private Node parseTernary() throws MolangException {
            Node cond = parseOr();
            skip();
            if (peek() == '?') {
                pos++;
                Node a = parseTernary();
                skip();
                if (peek() != ':') {
                    // "a ? b" with no else branch evaluates to 0 when false
                    return ctx -> cond.eval(ctx) != 0 ? a.eval(ctx) : 0;
                }
                pos++;
                Node b = parseTernary();
                return ctx -> cond.eval(ctx) != 0 ? a.eval(ctx) : b.eval(ctx);
            }
            return cond;
        }

        private Node parseOr() throws MolangException {
            Node left = parseAnd();
            while (true) {
                skip();
                if (peek() == '|' && peekAt(1) == '|') {
                    pos += 2;
                    Node l = left;
                    Node r = parseAnd();
                    left = ctx -> (l.eval(ctx) != 0 || r.eval(ctx) != 0) ? 1 : 0;
                } else {
                    return left;
                }
            }
        }

        private Node parseAnd() throws MolangException {
            Node left = parseEquality();
            while (true) {
                skip();
                if (peek() == '&' && peekAt(1) == '&') {
                    pos += 2;
                    Node l = left;
                    Node r = parseEquality();
                    left = ctx -> (l.eval(ctx) != 0 && r.eval(ctx) != 0) ? 1 : 0;
                } else {
                    return left;
                }
            }
        }

        private Node parseEquality() throws MolangException {
            Node left = parseComparison();
            while (true) {
                skip();
                boolean eq = peek() == '=' && peekAt(1) == '=';
                boolean ne = peek() == '!' && peekAt(1) == '=';
                if (!eq && !ne) {
                    return left;
                }
                pos += 2;
                Node l = left;
                Node r = parseComparison();
                left = eq ? ctx -> l.eval(ctx) == r.eval(ctx) ? 1 : 0 : ctx -> l.eval(ctx) != r.eval(ctx) ? 1 : 0;
            }
        }

        private Node parseComparison() throws MolangException {
            Node left = parseAdd();
            while (true) {
                skip();
                char c = peek();
                if (c != '<' && c != '>') {
                    return left;
                }
                boolean orEqual = peekAt(1) == '=';
                pos += orEqual ? 2 : 1;
                Node l = left;
                Node r = parseAdd();
                left = switch (c) {
                    case '<' -> orEqual ? ctx -> l.eval(ctx) <= r.eval(ctx) ? 1 : 0 : ctx -> l.eval(ctx) < r.eval(ctx) ? 1 : 0;
                    default -> orEqual ? ctx -> l.eval(ctx) >= r.eval(ctx) ? 1 : 0 : ctx -> l.eval(ctx) > r.eval(ctx) ? 1 : 0;
                };
            }
        }

        private Node parseAdd() throws MolangException {
            Node left = parseMul();
            while (true) {
                skip();
                char c = peek();
                if (c != '+' && c != '-') {
                    return left;
                }
                pos++;
                Node l = left;
                Node r = parseMul();
                left = c == '+' ? ctx -> l.eval(ctx) + r.eval(ctx) : ctx -> l.eval(ctx) - r.eval(ctx);
            }
        }

        private Node parseMul() throws MolangException {
            Node left = parseUnary();
            while (true) {
                skip();
                char c = peek();
                if (c != '*' && c != '/') {
                    return left;
                }
                pos++;
                Node l = left;
                Node r = parseUnary();
                left = c == '*' ? ctx -> l.eval(ctx) * r.eval(ctx) : ctx -> {
                    double d = r.eval(ctx);
                    return d == 0 ? 0 : l.eval(ctx) / d; // Molang: division by zero gives 0
                };
            }
        }

        private Node parseUnary() throws MolangException {
            skip();
            char c = peek();
            if (c == '-') {
                pos++;
                Node n = parseUnary();
                return ctx -> -n.eval(ctx);
            }
            if (c == '+') {
                pos++;
                return parseUnary();
            }
            if (c == '!') {
                pos++;
                Node n = parseUnary();
                return ctx -> n.eval(ctx) == 0 ? 1 : 0;
            }
            return parsePrimary();
        }

        private Node parsePrimary() throws MolangException {
            skip();
            char c = peek();
            if (c == '(') {
                pos++;
                Node n = parseTernary();
                skip();
                if (peek() != ')') {
                    throw error("Missing ')'");
                }
                pos++;
                return n;
            }
            if (Character.isDigit(c) || c == '.') {
                int start = pos;
                while (pos < s.length() && (Character.isDigit(peek()) || peek() == '.')) {
                    pos++;
                }
                if (pos < s.length() && (peek() == 'f' || peek() == 'F')) {
                    pos++;
                }
                String text = s.substring(start, pos).replaceAll("[fF]$", "");
                try {
                    double v = Double.parseDouble(text);
                    return ctx -> v;
                } catch (NumberFormatException e) {
                    throw error("Bad number '" + text + "'");
                }
            }
            String name = readName();
            if (name == null) {
                throw error(pos >= s.length() ? "Unexpected end" : "Unexpected '" + c + "'");
            }
            skip();
            if (peek() == '(') {
                pos++;
                List<Node> args = new ArrayList<>();
                skip();
                if (peek() != ')') {
                    do {
                        args.add(parseTernary());
                        skip();
                    } while (consume(','));
                }
                if (peek() != ')') {
                    throw error("Missing ')' after arguments of " + name);
                }
                pos++;
                return function(canonical(name), args);
            }
            return switch (name) {
                case "true" -> ctx -> 1;
                case "false" -> ctx -> 0;
                case "math.pi" -> ctx -> Math.PI;
                default -> {
                    String key = canonical(name);
                    yield ctx -> ctx.get(key);
                }
            };
        }

        private boolean consume(char c) {
            skip();
            if (peek() == c) {
                pos++;
                return true;
            }
            return false;
        }

        private Node function(String name, List<Node> a) throws MolangException {
            return switch (name) {
                case "math.sin" -> one(name, a, x -> Math.sin(Math.toRadians(x)));
                case "math.cos" -> one(name, a, x -> Math.cos(Math.toRadians(x)));
                case "math.tan" -> one(name, a, x -> Math.tan(Math.toRadians(x)));
                case "math.asin" -> one(name, a, x -> Math.toDegrees(Math.asin(x)));
                case "math.acos" -> one(name, a, x -> Math.toDegrees(Math.acos(x)));
                case "math.atan" -> one(name, a, x -> Math.toDegrees(Math.atan(x)));
                case "math.abs" -> one(name, a, Math::abs);
                case "math.sqrt" -> one(name, a, x -> x < 0 ? 0 : Math.sqrt(x));
                case "math.exp" -> one(name, a, Math::exp);
                case "math.ln" -> one(name, a, x -> x <= 0 ? 0 : Math.log(x));
                case "math.floor" -> one(name, a, Math::floor);
                case "math.ceil" -> one(name, a, Math::ceil);
                case "math.round" -> one(name, a, x -> (double) Math.round(x));
                case "math.trunc" -> one(name, a, x -> x < 0 ? Math.ceil(x) : Math.floor(x));
                case "math.sign" -> one(name, a, Math::signum);
                case "math.atan2" -> two(name, a, (y, x) -> Math.toDegrees(Math.atan2(y, x)));
                case "math.pow" -> two(name, a, Math::pow);
                case "math.min" -> two(name, a, Math::min);
                case "math.max" -> two(name, a, Math::max);
                case "math.mod" -> two(name, a, (x, y) -> y == 0 ? 0 : x % y);
                case "math.clamp" -> three(name, a, (x, lo, hi) -> Math.max(lo, Math.min(hi, x)));
                case "math.lerp" -> three(name, a, (x, y, t) -> x + (y - x) * t);
                case "math.lerprotate" -> three(name, a, (x, y, t) -> {
                    double d = ((y - x) % 360 + 540) % 360 - 180;
                    return x + d * t;
                });
                case "math.hermite_blend" -> one(name, a, t -> 3 * t * t - 2 * t * t * t);
                case "math.random" -> {
                    if (a.size() == 2) {
                        yield ctx -> {
                            double lo = a.get(0).eval(ctx);
                            double hi = a.get(1).eval(ctx);
                            return lo + (hi - lo) * ctx.random();
                        };
                    }
                    yield ctx -> ctx.random();
                }
                default -> throw new MolangException("Unknown function '" + name + "'");
            };
        }

        private interface F1 {
            double f(double x);
        }

        private interface F2 {
            double f(double x, double y);
        }

        private interface F3 {
            double f(double x, double y, double z);
        }

        private static Node one(String n, List<Node> a, F1 f) throws MolangException {
            need(n, a, 1);
            return ctx -> f.f(a.get(0).eval(ctx));
        }

        private static Node two(String n, List<Node> a, F2 f) throws MolangException {
            need(n, a, 2);
            return ctx -> f.f(a.get(0).eval(ctx), a.get(1).eval(ctx));
        }

        private static Node three(String n, List<Node> a, F3 f) throws MolangException {
            need(n, a, 3);
            return ctx -> f.f(a.get(0).eval(ctx), a.get(1).eval(ctx), a.get(2).eval(ctx));
        }

        private static void need(String n, List<Node> a, int count) throws MolangException {
            if (a.size() != count) {
                throw new MolangException(n + " takes " + count + " argument(s), got " + a.size());
            }
        }

        /** Reads {@code identifier(.identifier)*}, lower-cased, or {@code null} if there is none. */
        private String readName() {
            skip();
            int start = pos;
            while (pos < s.length() && (Character.isLetterOrDigit(peek()) || peek() == '_' || peek() == '.')) {
                if (pos == start && Character.isDigit(peek())) {
                    return null;
                }
                pos++;
            }
            return pos == start ? null : s.substring(start, pos).toLowerCase(Locale.ROOT);
        }

        private void skip() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        private char peekAt(int offset) {
            return pos + offset < s.length() ? s.charAt(pos + offset) : '\0';
        }

        private MolangException error(String message) {
            return new MolangException(message + " at position " + pos + " in '" + s + "'");
        }
    }
}

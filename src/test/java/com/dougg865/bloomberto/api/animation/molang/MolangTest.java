package com.dougg865.bloomberto.api.animation.molang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MolangTest {
    private static double eval(String expr) throws MolangException {
        return Molang.compile(expr).eval(new MolangContext());
    }

    private static double eval(String expr, MolangContext ctx) throws MolangException {
        return Molang.compile(expr).eval(ctx);
    }

    @Test
    void arithmeticAndPrecedence() throws Exception {
        assertEquals(14, eval("2 + 3 * 4"), 1e-9);
        assertEquals(20, eval("(2 + 3) * 4"), 1e-9);
        assertEquals(-3, eval("-(1 + 2)"), 1e-9);
        assertEquals(2.5, eval("5 / 2"), 1e-9);
        assertEquals(0.5, eval(".5"), 1e-9);
        assertEquals(1.5, eval("1.5f"), 1e-9);
    }

    @Test
    void divisionByZeroIsZero() throws Exception {
        assertEquals(0, eval("1 / 0"), 1e-9);
    }

    @Test
    void comparisonsLogicAndTernary() throws Exception {
        assertEquals(1, eval("3 > 2"), 1e-9);
        assertEquals(0, eval("3 <= 2"), 1e-9);
        assertEquals(1, eval("2 == 2 && 1 != 2"), 1e-9);
        assertEquals(1, eval("0 || 5"), 1e-9);
        assertEquals(1, eval("!0"), 1e-9);
        assertEquals(10, eval("1 > 0 ? 10 : 20"), 1e-9);
        assertEquals(20, eval("1 < 0 ? 10 : 20"), 1e-9);
        assertEquals(0, eval("1 < 0 ? 10"), 1e-9);
        assertEquals(1, eval("true"), 1e-9);
    }

    @Test
    void mathFunctionsUseDegrees() throws Exception {
        assertEquals(1, eval("math.sin(90)"), 1e-9);
        assertEquals(-1, eval("math.cos(180)"), 1e-9);
        assertEquals(3, eval("math.abs(-3)"), 1e-9);
        assertEquals(8, eval("math.pow(2, 3)"), 1e-9);
        assertEquals(2, eval("math.max(1, 2)"), 1e-9);
        assertEquals(5, eval("math.clamp(9, 0, 5)"), 1e-9);
        assertEquals(15, eval("math.lerp(10, 20, 0.5) + 5 - 5 + 0"), 1e-9);
        assertEquals(1, eval("math.mod(7, 3)"), 1e-9);
        assertEquals(Math.PI, eval("math.pi"), 1e-9);
        assertEquals(90, eval("math.atan2(1, 0)"), 1e-9);
        assertEquals(360, eval("math.lerprotate(340, 20, 0.5)"), 1e-9);
    }

    @Test
    void readsQueriesAndVariablesWithAliases() throws Exception {
        MolangContext ctx = new MolangContext().set("query.anim_time", 0.5).set("v.speed", 4);
        assertEquals(2, eval("q.anim_time * variable.speed", ctx), 1e-9);
        assertEquals(0, eval("variable.missing", ctx), 1e-9);
        assertEquals(2, eval("Query.Anim_Time * V.Speed", ctx), 1e-9);
    }

    @Test
    void assignmentsAndReturn() throws Exception {
        MolangContext ctx = new MolangContext();
        assertEquals(6, eval("variable.a = 2; variable.b = variable.a * 3; variable.b", ctx), 1e-9);
        assertEquals(6, ctx.get("variable.b"), 1e-9);
        assertEquals(1, eval("return 1; 2", ctx), 1e-9);
        assertEquals(1, eval("v.x = 1; v.x == 1", ctx), 1e-9);
    }

    @Test
    void randomIsInRange() throws Exception {
        MolangContext ctx = new MolangContext();
        Molang m = Molang.compile("math.random(5, 6)");
        for (int i = 0; i < 20; i++) {
            double v = m.eval(ctx);
            assertEquals(true, v >= 5 && v <= 6);
        }
    }

    @Test
    void badInputIsRejectedWithAMessage() {
        assertThrows(MolangException.class, () -> Molang.compile(""));
        assertThrows(MolangException.class, () -> Molang.compile("1 +"));
        assertThrows(MolangException.class, () -> Molang.compile("(1"));
        assertThrows(MolangException.class, () -> Molang.compile("math.nothing(1)"));
        assertThrows(MolangException.class, () -> Molang.compile("math.sin(1, 2)"));
        assertThrows(MolangException.class, () -> Molang.compile("1 2"));
    }

    @Test
    void isNumberDistinguishesNumbers() {
        assertEquals(true, Molang.isNumber("1.5"));
        assertEquals(false, Molang.isNumber("math.sin(1)"));
    }
}

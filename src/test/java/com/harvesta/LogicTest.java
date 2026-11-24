package com.harvesta;

import com.harvesta.web.Fn;
import com.harvesta.web.TreeServlet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure logic that needs no database: display formats, escaping, JSON, sale totals, option sets, .env parsing. */
class LogicTest {

    @Test
    void formatsFollowPrd() {
        assertEquals("21 Oct 2026", Fn.date(LocalDate.of(2026, 10, 21)));
        assertEquals("1,250,000.50", Fn.number(new BigDecimal("1250000.5")));
        assertEquals("IDR 1,250,000", Fn.money(new BigDecimal("1250000")));
        assertEquals("12", Fn.qty(new BigDecimal("12.00")));
        assertEquals("12.50", Fn.qty(new BigDecimal("12.5")));
        assertEquals("—", Fn.money(null));
    }

    @Test
    void escapesHtml() {
        assertEquals("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;", Fn.e("<script>alert('x')</script> & \""));
        assertEquals("", Fn.e(null));
    }

    @Test
    void buildsChartJson() {
        Map<String, Object> chart = new java.util.LinkedHashMap<>();
        chart.put("labels", List.of("Jan \"26"));
        chart.put("kg", List.of(new BigDecimal("1.50")));
        assertEquals("{\"labels\":[\"Jan \\\"26\"],\"kg\":[1.50]}", Fn.json(chart));
        assertEquals("[1,null,\"a\"]", Fn.json(java.util.Arrays.asList(1, null, "a")));
    }

    @Test
    void dueLabels() {
        assertEquals("Due today", Fn.due(LocalDate.now()));
        assertEquals("Due tomorrow", Fn.due(LocalDate.now().plusDays(1)));
        assertEquals("Overdue by 2 days", Fn.due(LocalDate.now().minusDays(2)));
        assertEquals("danger", Fn.dueTone(LocalDate.now().minusDays(1)));
    }

    @Test
    void saleTotalIsQuantityTimesPrice() {
        assertEquals(new BigDecimal("37500.00"), SaleService.total(new BigDecimal("2.5"), new BigDecimal("15000")));
        assertEquals(new BigDecimal("0.33"), SaleService.total(new BigDecimal("0.333"), BigDecimal.ONE));
    }

    @Test
    void imageTypeComesFromMagicBytes() {
        assertEquals("image/jpeg", TreeServlet.imageType(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}));
        assertEquals("image/png", TreeServlet.imageType(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0, 0}));
        assertEquals("image/webp", TreeServlet.imageType("RIFF0000WEBPVP8 ".getBytes()));
        assertNull(TreeServlet.imageType("<?php echo 1; ?>".getBytes()));
    }

    @Test
    void optionSetsValidateAndLabel() {
        assertTrue(Options.valid(Options.TreeStatus.values(), "needs_attention"));
        assertFalse(Options.valid(Options.TreeStatus.values(), "NEEDS_ATTENTION"));
        assertEquals("Needs attention", Options.label("needs_attention"));
        assertEquals("Pest control", Options.label("pest_control"));
        assertEquals("unknown", Options.label("unknown"));
    }

    @Test
    void readsDotEnv(@TempDir Path dir) throws IOException {
        Path file = dir.resolve(".env");
        Files.writeString(file, "# comment\nAPP_NAME=\"My Farm\"\nDB_PORT=5433\nBROKEN\nEMPTY=\n");
        Map<String, String> values = Env.load(file);
        assertEquals("My Farm", values.get("APP_NAME"));
        assertEquals("5433", values.get("DB_PORT"));
        assertFalse(values.containsKey("BROKEN"));
    }
}

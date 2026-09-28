package com.checkmarx.jenkins.unit;

import com.checkmarx.jenkins.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StringUtilsTest {

    @Test
    public void nullAndEmptyStringsAreEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isNotEmpty(null));
        assertFalse(StringUtils.isNotEmpty(""));
    }

    @Test
    public void whitespaceAndTextAreNotEmpty() {
        for (String value : new String[]{" ", "\t", "\r\n", "main", " main "}) {
            assertFalse(StringUtils.isEmpty(value));
            assertTrue(StringUtils.isNotEmpty(value));
        }
    }

    @Test
    public void chompPreservesNullAndEmptyStrings() {
        assertNull(StringUtils.chomp(null));
        assertEquals("", StringUtils.chomp(""));
    }

    @Test
    public void chompRemovesEachSupportedLineEnding() {
        for (String ending : new String[]{"\r\n", "\n", "\r"}) {
            assertEquals("", StringUtils.chomp(ending));
            assertEquals("1234567890", StringUtils.chomp("1234567890" + ending));
        }
    }

    @Test
    public void chompRemovesOnlyOneLineEnding() {
        assertEquals("123\n", StringUtils.chomp("123\n\n"));
        assertEquals("123\r", StringUtils.chomp("123\r\r"));
        assertEquals("123\r\n", StringUtils.chomp("123\r\n\r\n"));
        assertEquals("123\n", StringUtils.chomp("123\n\r"));
    }

    @Test
    public void chompPreservesOtherWhitespaceAndEmbeddedNewlines() {
        for (String value : new String[]{"123", " 123 ", "123\t", "\n123", "12\n3", "123\n ", "123\u2028"}) {
            assertEquals(value, StringUtils.chomp(value));
        }
        assertEquals(" 123 \t", StringUtils.chomp(" 123 \t\r\n"));
    }
}

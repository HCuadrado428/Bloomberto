package com.dougg865.bloomberto.impl;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dougg865.bloomberto.impl.network.VersionCheck;
import org.junit.jupiter.api.Test;

class VersionCheckTest {
    @Test
    void sameLevelSaysNothing() {
        assertNull(VersionCheck.message(2, 2));
    }

    @Test
    void olderClientIsToldToUpdate() {
        assertTrue(VersionCheck.message(2, 3).contains("update Bloomberto"));
        assertTrue(VersionCheck.message(3, 2).contains("server's Bloomberto is older"));
    }
}

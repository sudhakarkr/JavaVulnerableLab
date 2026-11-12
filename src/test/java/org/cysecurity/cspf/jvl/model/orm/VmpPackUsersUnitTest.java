package org.cysecurity.cspf.jvl.model.orm;

import junit.framework.TestCase;

public class VmpPackUsersUnitTest extends TestCase {

    // --- getAbout / setAbout ---

    public void testGetAboutInitiallyNull() {
        Users users = new Users();
        assertNull(users.getAbout());
    }

    public void testSetAndGetAbout() {
        Users users = new Users();
        users.setAbout("Some bio text");
        assertEquals("Some bio text", users.getAbout());
    }

    public void testSetAboutNull() {
        Users users = new Users();
        users.setAbout("initial");
        users.setAbout(null);
        assertNull(users.getAbout());
    }

    public void testSetAboutEmptyString() {
        Users users = new Users();
        users.setAbout("");
        assertEquals("", users.getAbout());
    }

    // --- getUsername / setUsername ---

    public void testGetUsernameInitiallyNull() {
        Users users = new Users();
        assertNull(users.getUsername());
    }

    public void testSetAndGetUsername() {
        Users users = new Users();
        users.setUsername("testuser");
        assertEquals("testuser", users.getUsername());
    }

    public void testSetUsernameNull() {
        Users users = new Users();
        users.setUsername("testuser");
        users.setUsername(null);
        assertNull(users.getUsername());
    }

    public void testSetUsernameEmptyString() {
        Users users = new Users();
        users.setUsername("");
        assertEquals("", users.getUsername());
    }

    public void testSetUsernameOverwrite() {
        Users users = new Users();
        users.setUsername("first");
        users.setUsername("second");
        assertEquals("second", users.getUsername());
    }

    // --- getId ---
    // setId is private, so we access the package-private field directly

    public void testGetIdInitiallyNull() {
        Users users = new Users();
        assertNull(users.getId());
    }

    public void testGetIdReturnsFieldValue() {
        Users users = new Users();
        users.id = 42L;
        assertEquals(Long.valueOf(42L), users.getId());
    }

    public void testGetIdReturnsZero() {
        Users users = new Users();
        users.id = 0L;
        assertEquals(Long.valueOf(0L), users.getId());
    }

    public void testGetIdReturnsNegative() {
        Users users = new Users();
        users.id = -1L;
        assertEquals(Long.valueOf(-1L), users.getId());
    }

    public void testGetIdReturnsLargeValue() {
        Users users = new Users();
        users.id = Long.MAX_VALUE;
        assertEquals(Long.valueOf(Long.MAX_VALUE), users.getId());
    }
}

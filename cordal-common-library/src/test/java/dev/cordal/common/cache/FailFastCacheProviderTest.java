package dev.cordal.common.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class FailFastCacheProviderTest {

    @Test
    void testGetThrows() {
        FailFastCacheProvider provider = new FailFastCacheProvider();
        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> provider.get("k", String.class)
        );
        assertTrue(ex.getMessage().contains("get"));
    }

    @Test
    void testWriteAndManagementOperationsThrow() {
        FailFastCacheProvider provider = new FailFastCacheProvider();

        assertThrows(UnsupportedOperationException.class, () -> provider.put("k", "v"));
        assertThrows(UnsupportedOperationException.class, () -> provider.put("k", "v", Duration.ofSeconds(1)));
        assertThrows(UnsupportedOperationException.class, () -> provider.remove("k"));
        assertThrows(UnsupportedOperationException.class, () -> provider.removePattern("p*"));
        assertThrows(UnsupportedOperationException.class, provider::clear);
        assertThrows(UnsupportedOperationException.class, provider::size);
        assertThrows(UnsupportedOperationException.class, () -> provider.containsKey("k"));
        assertThrows(UnsupportedOperationException.class, provider::getStatistics);
        assertThrows(UnsupportedOperationException.class, provider::cleanup);
    }
}
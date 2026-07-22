package com.rishikanth.linkforge.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    void encodesZeroAsFirstAlphabetChar() {
        assertThat(encoder.encode(0)).isEqualTo("0");
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 61, 62, 63, 1000, 123456, Long.MAX_VALUE / 2})
    void encodeThenDecodeRoundTrips(long id) {
        String code = encoder.encode(id);
        long decoded = encoder.decode(code);
        assertThat(decoded).isEqualTo(id);
    }

    @Test
    void differentIdsProduceDifferentCodes() {
        assertThat(encoder.encode(1000)).isNotEqualTo(encoder.encode(1001));
    }

    @Test
    void codesAreMonotonicallyLongerAsIdsGrow() {
        // Sanity check on the encoding scheme's growth rate (base62 alphabet size)
        assertThat(encoder.encode(61).length()).isEqualTo(1);
        assertThat(encoder.encode(62).length()).isEqualTo(2);
    }

    @Test
    void decodeRejectsInvalidCharacters() {
        assertThatThrownBy(() -> encoder.decode("abc!"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

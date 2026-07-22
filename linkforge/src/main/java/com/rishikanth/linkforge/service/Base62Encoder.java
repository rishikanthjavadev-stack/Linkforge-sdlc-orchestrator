package com.rishikanth.linkforge.service;

import org.springframework.stereotype.Component;

/**
 * Encodes/decodes the auto-increment DB id to/from a Base62 string.
 *
 * Trade-off (documented in architecture-decisions.md):
 * Base62-over-counter was chosen over random-hash generation because it
 * guarantees uniqueness for free (no collision retries needed) at the cost
 * of making short codes sequentially guessable/enumerable. For a prototype
 * this is an accepted risk; production hardening options are listed in the
 * risk-register doc (e.g. XOR-masking the id before encoding, or salting).
 */
@Component
public class Base62Encoder {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    public String encode(long id) {
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        long value = id;
        while (value > 0) {
            int remainder = (int) (value % BASE);
            sb.append(ALPHABET.charAt(remainder));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    public long decode(String code) {
        long result = 0;
        for (char c : code.toCharArray()) {
            int digit = ALPHABET.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result = result * BASE + digit;
        }
        return result;
    }
}

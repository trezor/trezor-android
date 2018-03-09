package com.satoshilabs.trezor.app.common;

import android.support.annotation.Nullable;

import java.util.Arrays;

public class Blake2s {

    int MAX_DIGEST_LENGTH = 32;
    int BLOCK_LENGTH = 64;
    int MAX_KEY_LENGTH = 32;
    long[] IV = {
            0x6a09e667,
            0xbb67ae85,
            0x3c6ef372,
            0xa54ff53a,
            0x510e527f,
            0x9b05688c,
            0x1f83d9ab,
            0x5be0cd19
    };
    int digestLength;
    boolean isFinished;
    long[] h;
    private long nx;
    private long t0;
    private long t1;
    private long f0;
    private long f1;
    private byte[] x;
    private byte[] result;

    public Blake2s(Integer digestLength, @Nullable byte[] key) {
        if (digestLength == null) {
            digestLength = MAX_DIGEST_LENGTH;
        }
        if (digestLength <= 0 || digestLength > MAX_DIGEST_LENGTH) {
            throw new RuntimeException("bad digestLength");
        }
        this.digestLength = digestLength;

        if (key == null) {
            key = new byte[0];
        }

        if (key.length > MAX_KEY_LENGTH) {
            throw new RuntimeException("key is too long");
        }

        this.isFinished = false;

        // Hash state.
        this.h = IV.clone();

        long[] param = {digestLength & 0xff, key.length, 1, 1};
        this.h[0] ^= param[0] & 0xFF | (param[1] & 0xFF) << 8 | (param[2] & 0xff) << 16 | (param[3] &
                0xff) << 24;

        this.x = new byte[BLOCK_LENGTH];
        this.nx = 0;

        // Byte counter.
        this.t0 = 0;
        this.t1 = 0;

        // Flags.
        this.f0 = 0;
        this.f1 = 0;

        // Fill buffer with key, if present.
        if (key.length > 0) {
            for (int i = 0; i < key.length; ++i) {
                this.x[i] = key[i];
            }
            for (int i = key.length; i < BLOCK_LENGTH; i++) {
                this.x[i] = 0;
            }
            this.nx = BLOCK_LENGTH;
        }
        digestLength = MAX_DIGEST_LENGTH;
//    blockLength = BLOCK_LENGTH;
//    keyLength = MAX_KEY_LENGTH;
    }


    private void processBlock(long length) {
        this.t0 += length;
        if (t0 != this.t0 >>> 0) {
            t0 = 0;
            t1++;
        }

        long v0 = this.h[0],
                v1 = this.h[1],
                v2 = this.h[2],
                v3 = this.h[3],
                v4 = this.h[4],
                v5 = this.h[5],
                v6 = this.h[6],
                v7 = this.h[7],
                v8 = IV[0],
                v9 = IV[1],
                v10 = IV[2],
                v11 = IV[3],
                v12 = IV[4] ^ this.t0,
                v13 = IV[5] ^ this.t1,
                v14 = IV[6] ^ this.f0,
                v15 = IV[7] ^ this.f1;

        int m0 = x[0] & 0xff | (x[1] & 0xff) << 8 | (x[2] & 0xff) << 16 | (x[3] & 0xff) << 24,
                m1 = x[4] & 0xff | (x[5] & 0xff) << 8 | (x[6] & 0xff) << 16 | (x[7] & 0xff) << 24,
                m2 = x[8] & 0xff | (x[9] & 0xff) << 8 | (x[10] & 0xff) << 16 | (x[11] & 0xff) << 24,
                m3 = x[12] & 0xff | (x[13] & 0xff) << 8 | (x[14] & 0xff) << 16 | (x[15] & 0xff) << 24,
                m4 = x[16] & 0xff | (x[17] & 0xff) << 8 | (x[18] & 0xff) << 16 | (x[19] & 0xff) << 24,
                m5 = x[20] & 0xff | (x[21] & 0xff) << 8 | (x[22] & 0xff) << 16 | (x[23] & 0xff) << 24,
                m6 = x[24] & 0xff | (x[25] & 0xff) << 8 | (x[26] & 0xff) << 16 | (x[27] & 0xff) << 24,
                m7 = x[28] & 0xff | (x[29] & 0xff) << 8 | (x[30] & 0xff) << 16 | (x[31] & 0xff) << 24,
                m8 = x[32] & 0xff | (x[33] & 0xff) << 8 | (x[34] & 0xff) << 16 | (x[35] & 0xff) << 24,
                m9 = x[36] & 0xff | (x[37] & 0xff) << 8 | (x[38] & 0xff) << 16 | (x[39] & 0xff) << 24,
                m10 = x[40] & 0xff | (x[41] & 0xff) << 8 | (x[42] & 0xff) << 16 | (x[43] & 0xff) << 24,
                m11 = x[44] & 0xff | (x[45] & 0xff) << 8 | (x[46] & 0xff) << 16 | (x[47] & 0xff) << 24,
                m12 = x[48] & 0xff | (x[49] & 0xff) << 8 | (x[50] & 0xff) << 16 | (x[51] & 0xff) << 24,
                m13 = x[52] & 0xff | (x[53] & 0xff) << 8 | (x[54] & 0xff) << 16 | (x[55] & 0xff) << 24,
                m14 = x[56] & 0xff | (x[57] & 0xff) << 8 | (x[58] & 0xff) << 16 | (x[59] & 0xff) << 24,
                m15 = x[60] & 0xff | (x[61] & 0xff) << 8 | (x[62] & 0xff) << 16 | (x[63] & 0xff) << 24;


        // Round 1.
        v0 = (v0 + m0) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16) & 0xFFFFFFFFL) | ((v12 >>> 16) & 0xFFFFFFFFL);
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m2) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m4) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m6) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m5) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m7) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m3) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m1) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m8) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m10) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m12) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m14) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m13) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m15) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m11) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m9) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;


        // Round 2.
        v0 = (v0 + m14) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m4) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m9) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m13) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m15) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m6) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m8) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m10) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m1) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m0) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m11) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m5) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m7) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m3) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m2) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m12) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;

        // Round 3.
        v0 = (v0 + m11) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m12) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m5) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m15) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m2) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m13) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m0) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m8) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m10) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m3) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m7) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m9) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m1) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m4) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m6) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m14) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;

        // Round 4.
        v0 = (v0 + m7) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m3) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m13) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m11) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m12) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m14) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m1) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m9) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m2) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m5) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m4) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m15) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m0) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m8) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m10) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m6) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;

        // Round 5.
        v0 = (v0 + m9) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m5) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m2) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m10) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m4) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m15) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m7) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m0) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m14) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m11) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m6) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m3) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m8) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m13) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m12) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m1) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;

        // Round 6.
        v0 = (v0 + m2) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m6) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m0) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m8) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m11) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m3) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m10) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m12) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m4) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m7) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m15) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m1) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m14) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m9) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m5) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m13) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;

        // Round 7.
        v0 = (v0 + m12) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m1) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m14) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m4) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m13) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m10) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m15) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m5) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m0) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m6) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m9) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m8) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m2) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m11) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m3) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m7) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;

        // Round 8.
        v0 = (v0 + m13) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m7) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m12) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m3) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m1) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m9) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m14) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m11) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m5) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m15) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m8) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m2) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m6) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m10) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m4) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m0) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;

        // Round 9.
        v0 = (v0 + m6) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m14) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m11) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m0) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m3) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m8) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m9) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m15) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m12) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m13) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m1) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m10) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m4) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m5) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m7) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m2) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;

        // Round 10.
        v0 = (v0 + m10) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m8) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m7) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m1) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m6) & 0xFFFFFFFFL;
        v2 = (v2 + v6) & 0xFFFFFFFFL;
        v14 = (v14 ^ v2) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v10 = (v10 + v14) & 0xFFFFFFFFL;
        v6 = (v6 ^ v10) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) & 0xFFFFFFFFL | (v6 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m5) & 0xFFFFFFFFL;
        v3 = (v3 + v7) & 0xFFFFFFFFL;
        v15 = (v15 ^ v3) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) & 0xFFFFFFFFL | (v15 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v15) & 0xFFFFFFFFL;
        v7 = (v7 ^ v11) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m4) & 0xFFFFFFFFL;
        v1 = (v1 + v5) & 0xFFFFFFFFL;
        v13 = (v13 ^ v1) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v13) & 0xFFFFFFFFL;
        v5 = (v5 ^ v9) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) & 0xFFFFFFFFL | (v5 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m2) & 0xFFFFFFFFL;
        v0 = (v0 + v4) & 0xFFFFFFFFL;
        v12 = (v12 ^ v0) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v12) & 0xFFFFFFFFL;
        v4 = (v4 ^ v8) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v0 = (v0 + m15) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 16)) & 0xFFFFFFFFL | (v15 >>> 16) & 0xFFFFFFFFL;
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 12)) & 0xFFFFFFFFL | (v5 >>> 12) & 0xFFFFFFFFL;
        v1 = (v1 + m9) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 16)) & 0xFFFFFFFFL | (v12 >>> 16) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 12)) & 0xFFFFFFFFL | (v6 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m3) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 16)) & 0xFFFFFFFFL | (v13 >>> 16) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 12)) & 0xFFFFFFFFL | (v7 >>> 12) & 0xFFFFFFFFL;
        v3 = (v3 + m13) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 16)) & 0xFFFFFFFFL | (v14 >>> 16) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 12)) & 0xFFFFFFFFL | (v4 >>> 12) & 0xFFFFFFFFL;
        v2 = (v2 + m12) & 0xFFFFFFFFL;
        v2 = (v2 + v7) & 0xFFFFFFFFL;
        v13 = (v13 ^ v2) & 0xFFFFFFFFL;
        v13 = (v13 << (32 - 8)) & 0xFFFFFFFFL | (v13 >>> 8) & 0xFFFFFFFFL;
        v8 = (v8 + v13) & 0xFFFFFFFFL;
        v7 = (v7 ^ v8) & 0xFFFFFFFFL;
        v7 = (v7 << (32 - 7)) & 0xFFFFFFFFL | (v7 >>> 7) & 0xFFFFFFFFL;
        v3 = (v3 + m0) & 0xFFFFFFFFL;
        v3 = (v3 + v4) & 0xFFFFFFFFL;
        v14 = (v14 ^ v3) & 0xFFFFFFFFL;
        v14 = (v14 << (32 - 8)) & 0xFFFFFFFFL | (v14 >>> 8) & 0xFFFFFFFFL;
        v9 = (v9 + v14) & 0xFFFFFFFFL;
        v4 = (v4 ^ v9) & 0xFFFFFFFFL;
        v4 = (v4 << (32 - 7)) & 0xFFFFFFFFL | (v4 >>> 7) & 0xFFFFFFFFL;
        v1 = (v1 + m14) & 0xFFFFFFFFL;
        v1 = (v1 + v6) & 0xFFFFFFFFL;
        v12 = (v12 ^ v1) & 0xFFFFFFFFL;
        v12 = (v12 << (32 - 8)) & 0xFFFFFFFFL | (v12 >>> 8) & 0xFFFFFFFFL;
        v11 = (v11 + v12) & 0xFFFFFFFFL;
        v6 = (v6 ^ v11) & 0xFFFFFFFFL;
        v6 = (v6 << (32 - 7)) | (v6 >>> 7);
        v0 = (v0 + m11) & 0xFFFFFFFFL;
        v0 = (v0 + v5) & 0xFFFFFFFFL;
        v15 = (v15 ^ v0) & 0xFFFFFFFFL;
        v15 = (v15 << (32 - 8)) | (v15 >>> 8);
        v10 = (v10 + v15) & 0xFFFFFFFFL;
        v5 = (v5 ^ v10) & 0xFFFFFFFFL;
        v5 = (v5 << (32 - 7)) | (v5 >>> 7);

        this.h[0] = (this.h[0] ^ v0 ^ v8) & 0xFFFFFFFFL;
        this.h[1] = (this.h[1] ^ v1 ^ v9) & 0xFFFFFFFFL;
        this.h[2] = (this.h[2] ^ v2 ^ v10) & 0xFFFFFFFFL;
        this.h[3] = (this.h[3] ^ v3 ^ v11) & 0xFFFFFFFFL;
        this.h[4] = (this.h[4] ^ v4 ^ v12) & 0xFFFFFFFFL;
        this.h[5] = (this.h[5] ^ v5 ^ v13) & 0xFFFFFFFFL;
        this.h[6] = (this.h[6] ^ v6 ^ v14) & 0xFFFFFFFFL;
        this.h[7] = (this.h[7] ^ v7 ^ v15) & 0xFFFFFFFFL;
    }

    public void update(byte[] p, Long offsetparam, Long lengthparam) {


        if (this.isFinished)
            throw new RuntimeException("update() after calling digest()");
        long offset = offsetparam == null ? 0 : offsetparam;
        long length = lengthparam == null ? p.length - offset : lengthparam;

        if (length == 0) {
            return;
        }

        long i, left = 64 - nx;


        // Finish buffer.
        if (length > left) {
            for (i = 0; i < left; i++) {
                this.x[(int) (this.nx + i)] = p[(int) (offset + i)];
            }
            this.processBlock(64);
            offset += left;
            length -= left;
            this.nx = 0;
        }

        // Process message blocks.
        while (length > 64) {
            for (i = 0; i < 64; i++) {
                this.x[(int) i] = p[(int) (offset + i)];
            }
            this.processBlock(64);
            offset += 64;
            length -= 64;
            this.nx = 0;
        }

        // Copy leftovers to buffer.
        for (i = 0; i < length; i++) {
            this.x[(int) (this.nx + i)] = p[(int) (offset + i)];
        }
        this.nx += length;
    }

    public byte[] digest() {


        int i;

        if (this.isFinished) return this.result;

        for (i = (int) this.nx; i < 64; i++) this.x[(int) i] = 0;

        // Set last block flag.
        this.f0 = 0xffffffff;

        //TODO in tree mode, set f1 to 0xffffffff.
        this.processBlock(this.nx);

        byte[] d = new byte[32];
        for (i = 0; i < 8; i++) {
            long h = this.h[(int) i];
            d[i * 4 + 0] = (byte) ((h >>> 0) & 0xff);
            d[i * 4 + 1] = (byte) ((h >>> 8) & 0xff);
            d[i * 4 + 2] = (byte) ((h >>> 16) & 0xff);
            d[i * 4 + 3] = (byte) ((h >>> 24) & 0xff);
        }

        this.result = Arrays.copyOfRange(d, 0, this.digestLength);
        this.isFinished = true;
        return this.result;
    }

    public void update(byte[] subarray) {
        this.update(subarray, null, null);
    }

//    BLAKE2s.prototype.hexDigest = function() {
//    var hex = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'];
//    var out = [];
//    var d = this.digest();
//    for (var i = 0; i < d.length; i++) {
//    out.push(hex[(d[i] >> 4) & 0xf]);
//    out.push(hex[d[i] & 0xf]);
//    }
//    return out.join('');
//    };


}

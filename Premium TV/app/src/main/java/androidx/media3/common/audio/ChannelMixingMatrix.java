package androidx.media3.common.audio;

import androidx.media3.common.util.Assertions;

/* JADX INFO: loaded from: classes.dex */
public final class ChannelMixingMatrix {
    private final float[] coefficients;
    private final int inputChannelCount;
    private final boolean isDiagonal;
    private final boolean isIdentity;
    private final boolean isZero;
    private final int outputChannelCount;

    public static ChannelMixingMatrix create(int inputChannelCount, int outputChannelCount) {
        return new ChannelMixingMatrix(inputChannelCount, outputChannelCount, createMixingCoefficients(inputChannelCount, outputChannelCount));
    }

    public ChannelMixingMatrix(int inputChannelCount, int outputChannelCount, float[] coefficients) {
        boolean z = false;
        Assertions.checkArgument(inputChannelCount > 0, "Input channel count must be positive.");
        Assertions.checkArgument(outputChannelCount > 0, "Output channel count must be positive.");
        Assertions.checkArgument(coefficients.length == inputChannelCount * outputChannelCount, "Coefficient array length is invalid.");
        this.inputChannelCount = inputChannelCount;
        this.outputChannelCount = outputChannelCount;
        this.coefficients = checkCoefficientsValid(coefficients);
        boolean allDiagonalCoefficientsAreOne = true;
        boolean allCoefficientsAreZero = true;
        boolean allNonDiagonalCoefficientsAreZero = true;
        int row = 0;
        while (row < inputChannelCount) {
            int col = 0;
            while (col < outputChannelCount) {
                float coefficient = getMixingCoefficient(row, col);
                boolean onDiagonal = row == col;
                if (coefficient != 1.0f && onDiagonal) {
                    allDiagonalCoefficientsAreOne = false;
                }
                if (coefficient != 0.0f) {
                    allCoefficientsAreZero = false;
                    if (!onDiagonal) {
                        allNonDiagonalCoefficientsAreZero = false;
                    }
                }
                col++;
            }
            row++;
        }
        this.isZero = allCoefficientsAreZero;
        this.isDiagonal = isSquare() && allNonDiagonalCoefficientsAreZero;
        if (this.isDiagonal && allDiagonalCoefficientsAreOne) {
            z = true;
        }
        this.isIdentity = z;
    }

    public int getInputChannelCount() {
        return this.inputChannelCount;
    }

    public int getOutputChannelCount() {
        return this.outputChannelCount;
    }

    public float getMixingCoefficient(int inputChannel, int outputChannel) {
        return this.coefficients[(this.outputChannelCount * inputChannel) + outputChannel];
    }

    public boolean isZero() {
        return this.isZero;
    }

    public boolean isSquare() {
        return this.inputChannelCount == this.outputChannelCount;
    }

    public boolean isDiagonal() {
        return this.isDiagonal;
    }

    public boolean isIdentity() {
        return this.isIdentity;
    }

    public ChannelMixingMatrix scaleBy(float scale) {
        float[] scaledCoefficients = new float[this.coefficients.length];
        for (int i = 0; i < this.coefficients.length; i++) {
            scaledCoefficients[i] = this.coefficients[i] * scale;
        }
        return new ChannelMixingMatrix(this.inputChannelCount, this.outputChannelCount, scaledCoefficients);
    }

    private static float[] createMixingCoefficients(int inputChannelCount, int outputChannelCount) {
        if (inputChannelCount == outputChannelCount) {
            return initializeIdentityMatrix(outputChannelCount);
        }
        if (inputChannelCount == 1 && outputChannelCount == 2) {
            return new float[]{1.0f, 1.0f};
        }
        if (inputChannelCount == 2 && outputChannelCount == 1) {
            return new float[]{0.5f, 0.5f};
        }
        throw new UnsupportedOperationException("Default channel mixing coefficients for " + inputChannelCount + "->" + outputChannelCount + " are not yet implemented.");
    }

    private static float[] initializeIdentityMatrix(int channelCount) {
        float[] coefficients = new float[channelCount * channelCount];
        for (int c = 0; c < channelCount; c++) {
            coefficients[(channelCount * c) + c] = 1.0f;
        }
        return coefficients;
    }

    private static float[] checkCoefficientsValid(float[] coefficients) {
        for (int i = 0; i < coefficients.length; i++) {
            if (coefficients[i] < 0.0f) {
                throw new IllegalArgumentException("Coefficient at index " + i + " is negative.");
            }
        }
        return coefficients;
    }
}

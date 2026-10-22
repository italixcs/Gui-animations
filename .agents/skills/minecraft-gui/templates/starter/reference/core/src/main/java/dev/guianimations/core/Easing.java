package dev.guianimations.core;

/** Gentle, bounded curves for UI transitions. Each curve maps zero to zero and one to one. */
public enum Easing {
    LINEAR {
        @Override
        double interpolate(double progress) {
            return progress;
        }
    },
    CUBIC_IN {
        @Override
        double interpolate(double progress) {
            return progress * progress * progress;
        }
    },
    CUBIC_OUT {
        @Override
        double interpolate(double progress) {
            double remaining = 1.0 - progress;
            return 1.0 - remaining * remaining * remaining;
        }
    },
    CUBIC_IN_OUT {
        @Override
        double interpolate(double progress) {
            if (progress < 0.5) {
                return 4.0 * progress * progress * progress;
            }
            double remaining = 1.0 - progress;
            return 1.0 - 4.0 * remaining * remaining * remaining;
        }
    };

    /**
     * Evaluates this curve. Finite input outside the unit interval is clamped.
     *
     * @throws IllegalArgumentException if progress is not finite
     */
    public double apply(double progress) {
        if (!Double.isFinite(progress)) {
            throw new IllegalArgumentException("Progress must be finite");
        }
        return interpolate(Math.max(0.0, Math.min(1.0, progress)));
    }

    abstract double interpolate(double progress);
}

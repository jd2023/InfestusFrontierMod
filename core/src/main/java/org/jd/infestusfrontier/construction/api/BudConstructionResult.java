package org.jd.infestusfrontier.construction.api;

/** Observable terminal result of one bounded bud application. */
public enum BudConstructionResult {
    SUCCESS,
    UNAVAILABLE,
    INCOMPLETE,
    TARGET_CHANGED,
    INCOMPATIBLE_SITE,
    ADMISSION_EXHAUSTED
}

package com.sdsweather.model;

/**
 * Incident - Data model representing a recorded hardware failure or issue on a unit.
 *
 * Captures what happened (summary), how critical it was (severity), and when it
 * occurred (reportedAt). Each incident can be linked to multiple components via the
 * IncidentComponent join table.
 *
 * Severity Levels:
 *   LOW    - Minor issue, unit still operational
 *   MEDIUM - Significant issue, partial functionality affected
 *   HIGH   - Critical failure, unit non-operational
 *
 * @author Zachary Sneed
 * @version 1.0
 * @since 2026-02-16
 */
public class Incident {

    /** Unique identifier for this incident */
    public String incidentId;

    /** ID of the unit this incident is associated with */
    public String unitId;

    /** Brief description of what occurred */
    public String summary;

    /** Severity level: LOW, MEDIUM, or HIGH */
    public String severity;

    /** ISO timestamp of when the incident was reported */
    public String reportedAt;
}

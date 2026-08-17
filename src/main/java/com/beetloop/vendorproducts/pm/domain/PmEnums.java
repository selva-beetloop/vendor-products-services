package com.beetloop.vendorproducts.pm.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumerations for the vendor Project Management module.
 *
 * <p>Values follow the BRD (Project_Management_Consolidated_BRD_v1.2), §5–§6 and
 * the ID/status conventions in §12.
 */
public final class PmEnums {

    private PmEnums() {
    }

    /** BRD §3.3 — a project delivers either a finished good or a raw material. */
    public enum ProductClass {
        FINISHED_GOODS("Finished Goods", "Open BOM"),
        RAW_MATERIAL("Raw Material", "Check Stock");

        private final String label;
        /** BRD PM-02b: the line-item action is driven by the RFQ product type. */
        private final String lineAction;

        ProductClass(String label, String lineAction) {
            this.label = label;
            this.lineAction = lineAction;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        public String getLineAction() {
            return lineAction;
        }

        @JsonCreator
        public static ProductClass from(String raw) {
            return PmEnumSupport.match(values(), raw, "product class");
        }
    }

    /** BRD §5.1 — the track a project follows once the RFQ converts. */
    public enum ProjectTrack {
        CONTRACT_MANUFACTURING("Contract Manufacturing"),
        FORMULATION_DEVELOPMENT("Formulation Development"),
        RAW_MATERIAL_FULFILMENT("Raw Material Fulfilment");

        private final String label;

        ProjectTrack(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static ProjectTrack from(String raw) {
            return PmEnumSupport.match(values(), raw, "project track");
        }
    }

    public enum ProjectStatus {
        ACTIVE("Active"),
        ON_HOLD("On Hold"),
        IN_CLOSURE("In Closure"),
        COMPLETED("Completed"),
        CLOSED("Closed"),
        CANCELLED("Cancelled");

        private final String label;

        ProjectStatus(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static ProjectStatus from(String raw) {
            return PmEnumSupport.match(values(), raw, "project status");
        }
    }

    /**
     * BRD §5.2 — the stage delivery and approval cycle:
     * execute → submit for QC → QC verdict → buyer approval → payment.
     */
    public enum StageStatus {
        NOT_STARTED("Not Started"),
        IN_PROGRESS("In Progress"),
        QC_PENDING("QC Pending"),
        QC_REJECTED("QC Rejected"),
        QC_APPROVED("QC Approved"),
        AWAITING_BUYER_APPROVAL("Awaiting Buyer Approval"),
        REWORK_REQUESTED("Rework Requested"),
        BUYER_APPROVED("Buyer Approved"),
        PAYMENT_REQUESTED("Payment Requested"),
        PAID("Paid"),
        COMPLETED("Completed");

        private final String label;

        StageStatus(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        /** BRD §5.2 step 4 — the Request for Payment button appears only after buyer approval. */
        public boolean allowsPaymentRequest() {
            return this == BUYER_APPROVED;
        }

        @JsonCreator
        public static StageStatus from(String raw) {
            return PmEnumSupport.match(values(), raw, "stage status");
        }
    }

    public enum OrderStatus {
        NOT_STARTED("Not Started"),
        IN_PROGRESS("In Progress"),
        BLOCKED("Blocked"),
        COMPLETED("Completed"),
        CLOSED("Closed");

        private final String label;

        OrderStatus(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static OrderStatus from(String raw) {
            return PmEnumSupport.match(values(), raw, "order status");
        }
    }

    public enum TaskStatus {
        TO_DO("To Do"),
        IN_PROGRESS("In Progress"),
        DONE("Done"),
        CANCELLED("Cancelled");

        private final String label;

        TaskStatus(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static TaskStatus from(String raw) {
            return PmEnumSupport.match(values(), raw, "task status");
        }
    }

    /**
     * Task priority — the PRIORITY column and filter on All Tasks.
     * Observed values on the live screen: HIGH, MEDIUM, CRITICAL.
     */
    public enum TaskPriority {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        CRITICAL("Critical");

        private final String label;

        TaskPriority(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static TaskPriority from(String raw) {
            return PmEnumSupport.match(values(), raw, "task priority");
        }
    }

    /**
     * Delivery-health indicator on the All Orders list (SLA column and filter).
     * Not named in the BRD — taken from the live screen.
     */
    public enum SlaState {
        ON_TRACK("On Track"),
        AT_RISK("At Risk"),
        DELAYED("Delayed");

        private final String label;

        SlaState(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static SlaState from(String raw) {
            return PmEnumSupport.match(values(), raw, "SLA state");
        }
    }

    /** BRD PM-02a / RM-01 — stock position held against a raw-material line. */
    public enum StockState {
        NOT_CHECKED("Not Checked"),
        IN_STOCK("In stock"),
        PARTIAL("Partial"),
        NOT_AVAILABLE("Not available");

        private final String label;

        StockState(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static StockState from(String raw) {
            return PmEnumSupport.match(values(), raw, "stock state");
        }
    }

    /** BRD §6.5 / BP-23 — change orders, including the auto-raised "Rework" type. */
    public enum ChangeOrderType {
        SCOPE("Scope"),
        TIMELINE("Timeline"),
        COST("Cost"),
        REWORK("Rework"),
        OTHER("Other");

        private final String label;

        ChangeOrderType(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static ChangeOrderType from(String raw) {
            return PmEnumSupport.match(values(), raw, "change order type");
        }
    }

    public enum ApprovalDecision {
        PENDING("Pending"),
        APPROVED("Approved"),
        REJECTED("Rejected");

        private final String label;

        ApprovalDecision(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static ApprovalDecision from(String raw) {
            return PmEnumSupport.match(values(), raw, "approval decision");
        }
    }

    /** BRD PM-07 / PM-10 — who raised or is accountable for an item. */
    public enum PmParty {
        VENDOR("Vendor"),
        BUYER("Buyer"),
        QC("QC"),
        PROJECT_MANAGER("Project Manager"),
        INVENTORY("Inventory"),
        SYSTEM("System");

        private final String label;

        PmParty(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static PmParty from(String raw) {
            return PmEnumSupport.match(values(), raw, "party");
        }
    }

    /** BRD §6.8 — ISS raised by a user, ESC auto-generated by the Project Manager (§11.4). */
    public enum IssueKind {
        ISSUE("Issue"),
        ESCALATION("Escalation");

        private final String label;

        IssueKind(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static IssueKind from(String raw) {
            return PmEnumSupport.match(values(), raw, "issue kind");
        }
    }

    public enum IssueSeverity {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        CRITICAL("Critical");

        private final String label;

        IssueSeverity(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static IssueSeverity from(String raw) {
            return PmEnumSupport.match(values(), raw, "issue severity");
        }
    }

    public enum IssueStatus {
        OPEN("Open"),
        IN_PROGRESS("In Progress"),
        RESOLVED("Resolved"),
        CLOSED("Closed");

        private final String label;

        IssueStatus(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static IssueStatus from(String raw) {
            return PmEnumSupport.match(values(), raw, "issue status");
        }
    }

    /** BRD BP-25 — shipment status flow. */
    public enum ShipmentStatus {
        CREATED("Shipment Created"),
        DISPATCHED("Dispatched"),
        IN_TRANSIT("In Transit"),
        DELIVERED("Delivered"),
        QC("QC");

        private final String label;

        ShipmentStatus(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static ShipmentStatus from(String raw) {
            return PmEnumSupport.match(values(), raw, "shipment status");
        }
    }

    /**
     * Risk badge shown on the Active Projects card (§6.1). Drives the card accent
     * colour as well as the badge text.
     */
    public enum RiskLevel {
        GREEN("GREEN", "green"),
        AMBER("AMBER", "amber"),
        RED("RED", "red");

        private final String label;
        private final String accent;

        RiskLevel(String label, String accent) {
            this.label = label;
            this.accent = accent;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        /** Card accent token the UI uses alongside the badge. */
        public String getAccent() {
            return accent;
        }

        @JsonCreator
        public static RiskLevel from(String raw) {
            return PmEnumSupport.match(values(), raw, "risk level");
        }
    }

    /** BRD §6.4 — dependency relationship between two stages/orders. */
    public enum DependencyType {
        FINISH_TO_START("Finish to Start"),
        START_TO_START("Start to Start"),
        FINISH_TO_FINISH("Finish to Finish"),
        START_TO_FINISH("Start to Finish");

        private final String label;

        DependencyType(String label) {
            this.label = label;
        }

        @JsonValue
        public String getName() {
            return name();
        }

        public String getLabel() {
            return label;
        }

        @JsonCreator
        public static DependencyType from(String raw) {
            return PmEnumSupport.match(values(), raw, "dependency type");
        }
    }
}

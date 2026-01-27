package uk.ac.ebi.eva.submission.entity;

import com.fasterxml.jackson.databind.JsonNode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "eva_submissions", name = "call_home_event")
public class CallHomeEventEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "deployment_id")
    private String deploymentId;

    @Column(name = "run_id")
    private String runId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "cli_version")
    private String cliVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "runtime_seconds")
    private Integer runtimeSeconds;

    @Column(name = "executor")
    private String executor;

    @Column(name = "tasks")
    private String tasks;

    @Column(name = "trim_down")
    private Boolean trimDown;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "raw_payload", columnDefinition = "jsonb", nullable = false)
    private JsonNode rawPayload;

    public CallHomeEventEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getCliVersion() {
        return cliVersion;
    }

    public void setCliVersion(String cliVersion) {
        this.cliVersion = cliVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getRuntimeSeconds() {
        return runtimeSeconds;
    }

    public void setRuntimeSeconds(Integer runtimeSeconds) {
        this.runtimeSeconds = runtimeSeconds;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public String getTasks() {
        return tasks;
    }

    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    public Boolean getTrimDown() {
        return trimDown;
    }

    public void setTrimDown(Boolean trimDown) {
        this.trimDown = trimDown;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public JsonNode getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(JsonNode rawPayload) {
        this.rawPayload = rawPayload;
    }
}


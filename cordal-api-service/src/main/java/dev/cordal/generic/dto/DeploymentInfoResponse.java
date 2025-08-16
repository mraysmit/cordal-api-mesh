package dev.cordal.generic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe response for deployment information
 * Replaces Map<String, Object> for getDeploymentInfo()
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentInfoResponse {
    
    @JsonProperty("jarPath")
    private final String jarPath;
    
    @JsonProperty("javaVersion")
    private final String javaVersion;
    
    @JsonProperty("osName")
    private final String osName;
    
    @JsonProperty("osVersion")
    private final String osVersion;
    
    @JsonProperty("osArch")
    private final String osArch;
    
    @JsonProperty("applicationName")
    private final String applicationName;
    
    @JsonProperty("applicationVersion")
    private final String applicationVersion;
    
    @JsonProperty("buildTime")
    private final String buildTime;
    
    @JsonProperty("gitCommit")
    private final String gitCommit;
    
    /**
     * Constructor
     */
    public DeploymentInfoResponse(String jarPath, String javaVersion, String osName, String osVersion,
                                 String osArch, String applicationName, String applicationVersion,
                                 String buildTime, String gitCommit) {
        this.jarPath = jarPath;
        this.javaVersion = javaVersion;
        this.osName = osName;
        this.osVersion = osVersion;
        this.osArch = osArch;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
        this.buildTime = buildTime;
        this.gitCommit = gitCommit;
    }
    
    /**
     * Static factory method from system properties
     */
    public static DeploymentInfoResponse fromSystem(String jarPath, String applicationName, 
                                                   String applicationVersion, String buildTime, String gitCommit) {
        return new DeploymentInfoResponse(
            jarPath,
            System.getProperty("java.version"),
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch"),
            applicationName,
            applicationVersion,
            buildTime,
            gitCommit
        );
    }
    
    // Getters
    public String getJarPath() {
        return jarPath;
    }
    
    public String getJavaVersion() {
        return javaVersion;
    }
    
    public String getOsName() {
        return osName;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public String getOsArch() {
        return osArch;
    }
    
    public String getApplicationName() {
        return applicationName;
    }
    
    public String getApplicationVersion() {
        return applicationVersion;
    }
    
    public String getBuildTime() {
        return buildTime;
    }
    
    public String getGitCommit() {
        return gitCommit;
    }
    
    /**
     * Check if running on Windows
     */
    public boolean isWindows() {
        return osName != null && osName.toLowerCase().contains("windows");
    }
    
    /**
     * Check if running on Linux
     */
    public boolean isLinux() {
        return osName != null && osName.toLowerCase().contains("linux");
    }
    
    /**
     * Check if running on macOS
     */
    public boolean isMacOS() {
        return osName != null && osName.toLowerCase().contains("mac");
    }
    
    /**
     * Get short git commit (first 8 characters)
     */
    public String getShortGitCommit() {
        return gitCommit != null && gitCommit.length() > 8 ? gitCommit.substring(0, 8) : gitCommit;
    }
    
    /**
     * Check if this is a development build (no git commit)
     */
    public boolean isDevelopmentBuild() {
        return gitCommit == null || gitCommit.isEmpty() || "unknown".equals(gitCommit);
    }
    
    /**
     * Convert to Map<String, Object> for backward compatibility
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("jarPath", jarPath);
        map.put("javaVersion", javaVersion);
        map.put("osName", osName);
        map.put("osVersion", osVersion);
        map.put("osArch", osArch);
        map.put("applicationName", applicationName);
        map.put("applicationVersion", applicationVersion);
        map.put("buildTime", buildTime);
        map.put("gitCommit", gitCommit);
        return map;
    }
    
    @Override
    public String toString() {
        return "DeploymentInfoResponse{" +
                "applicationName='" + applicationName + '\'' +
                ", applicationVersion='" + applicationVersion + '\'' +
                ", javaVersion='" + javaVersion + '\'' +
                ", osName='" + osName + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", osArch='" + osArch + '\'' +
                ", jarPath='" + jarPath + '\'' +
                ", buildTime='" + buildTime + '\'' +
                ", gitCommit='" + getShortGitCommit() + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DeploymentInfoResponse that = (DeploymentInfoResponse) o;
        
        if (jarPath != null ? !jarPath.equals(that.jarPath) : that.jarPath != null) return false;
        if (javaVersion != null ? !javaVersion.equals(that.javaVersion) : that.javaVersion != null) return false;
        if (osName != null ? !osName.equals(that.osName) : that.osName != null) return false;
        if (osVersion != null ? !osVersion.equals(that.osVersion) : that.osVersion != null) return false;
        if (osArch != null ? !osArch.equals(that.osArch) : that.osArch != null) return false;
        if (applicationName != null ? !applicationName.equals(that.applicationName) : that.applicationName != null)
            return false;
        if (applicationVersion != null ? !applicationVersion.equals(that.applicationVersion) : that.applicationVersion != null)
            return false;
        if (buildTime != null ? !buildTime.equals(that.buildTime) : that.buildTime != null) return false;
        return gitCommit != null ? gitCommit.equals(that.gitCommit) : that.gitCommit == null;
    }
    
    @Override
    public int hashCode() {
        int result = jarPath != null ? jarPath.hashCode() : 0;
        result = 31 * result + (javaVersion != null ? javaVersion.hashCode() : 0);
        result = 31 * result + (osName != null ? osName.hashCode() : 0);
        result = 31 * result + (osVersion != null ? osVersion.hashCode() : 0);
        result = 31 * result + (osArch != null ? osArch.hashCode() : 0);
        result = 31 * result + (applicationName != null ? applicationName.hashCode() : 0);
        result = 31 * result + (applicationVersion != null ? applicationVersion.hashCode() : 0);
        result = 31 * result + (buildTime != null ? buildTime.hashCode() : 0);
        result = 31 * result + (gitCommit != null ? gitCommit.hashCode() : 0);
        return result;
    }
}

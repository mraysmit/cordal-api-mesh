package dev.cordal.generic.dto;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentInfoResponseTest {

    @Test
    void shouldExposeDeploymentFieldsAndDetectPlatforms() {
        DeploymentInfoResponse windows = new DeploymentInfoResponse(
            "app.jar", "21", "Windows 11", "11", "amd64", "CORDAL", "1.0.0", "now", "123456789abc"
        );
        DeploymentInfoResponse linux = new DeploymentInfoResponse(
            "app.jar", "21", "Linux", "6.8", "x86_64", "CORDAL", "1.0.0", "now", "unknown"
        );
        DeploymentInfoResponse mac = new DeploymentInfoResponse(
            "app.jar", "21", "Mac OS X", "14", "aarch64", "CORDAL", "1.0.0", "now", ""
        );

        assertThat(windows.getJarPath()).isEqualTo("app.jar");
        assertThat(windows.getJavaVersion()).isEqualTo("21");
        assertThat(windows.getApplicationName()).isEqualTo("CORDAL");
        assertThat(windows.getApplicationVersion()).isEqualTo("1.0.0");
        assertThat(windows.isWindows()).isTrue();
        assertThat(windows.isLinux()).isFalse();
        assertThat(windows.isMacOS()).isFalse();
        assertThat(windows.getShortGitCommit()).isEqualTo("12345678");
        assertThat(windows.isDevelopmentBuild()).isFalse();

        assertThat(linux.isLinux()).isTrue();
        assertThat(linux.isDevelopmentBuild()).isTrue();
        assertThat(mac.isMacOS()).isTrue();
        assertThat(mac.isDevelopmentBuild()).isTrue();
    }

    @Test
    void shouldCreateFromSystemAndConvertToMap() {
        DeploymentInfoResponse response = DeploymentInfoResponse.fromSystem(
            "app.jar", "CORDAL", "1.0.0", "build-time", "commit1234"
        );

        Map<String, Object> map = response.toMap();

        assertThat(response.getJarPath()).isEqualTo("app.jar");
        assertThat(response.getApplicationName()).isEqualTo("CORDAL");
        assertThat(response.getApplicationVersion()).isEqualTo("1.0.0");
        assertThat(response.getBuildTime()).isEqualTo("build-time");
        assertThat(response.getGitCommit()).isEqualTo("commit1234");
        assertThat(response.getJavaVersion()).isEqualTo(System.getProperty("java.version"));
        assertThat(response.getOsName()).isEqualTo(System.getProperty("os.name"));
        assertThat(response.getOsVersion()).isEqualTo(System.getProperty("os.version"));
        assertThat(response.getOsArch()).isEqualTo(System.getProperty("os.arch"));
        assertThat(map)
            .containsEntry("jarPath", "app.jar")
            .containsEntry("applicationName", "CORDAL")
            .containsEntry("applicationVersion", "1.0.0")
            .containsEntry("buildTime", "build-time")
            .containsEntry("gitCommit", "commit1234");
    }

    @Test
    void shouldSupportEqualityHashCodeAndToString() {
        DeploymentInfoResponse first = new DeploymentInfoResponse(
            "app.jar", "21", "Linux", "6.8", "x86_64", "CORDAL", "1.0.0", "now", "abcdef123456"
        );
        DeploymentInfoResponse second = new DeploymentInfoResponse(
            "app.jar", "21", "Linux", "6.8", "x86_64", "CORDAL", "1.0.0", "now", "abcdef123456"
        );
        DeploymentInfoResponse different = new DeploymentInfoResponse(
            "other.jar", "21", "Linux", "6.8", "x86_64", "CORDAL", "1.0.0", "now", "abcdef123456"
        );

        assertThat(first).isEqualTo(second).isNotEqualTo(different);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first.toString())
            .contains("DeploymentInfoResponse")
            .contains("applicationName='CORDAL'")
            .contains("gitCommit='abcdef12'");
    }
}
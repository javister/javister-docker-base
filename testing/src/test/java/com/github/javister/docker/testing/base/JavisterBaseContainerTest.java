package com.github.javister.docker.testing.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SuppressWarnings({"java:S119", "unchecked"})
class JavisterBaseContainerTest {

    @Test
    void getImageCoordinate() {
        assertEquals(
                JavisterBaseContainer.getImageCoordinate(JavisterBaseContainer.class, null),
                "/META-INF/docker/com.github.javister.docker/javister-docker-base/");
    }

    @Test
    void getImageTag() {
        assertThat(
                JavisterBaseContainer.getImageTag(JavisterBaseContainer.class, null),
                matchesPattern("^\\d+\\.\\d+(\\.\\d+)?(-.*)?$"));
    }

    @Test
    void getImageRepository() {
        assertEquals(
                JavisterBaseContainer.getImageRepository(JavisterBaseContainer.class, null),
                "javister-docker-docker.bintray.io/javister/javister-docker-base");
    }

    @Test
    void getImageName() {
        assertThat(
                JavisterBaseContainer.getImageName(JavisterBaseContainer.class, null),
                matchesPattern("^javister-docker-docker\\.bintray\\.io/javister/javister-docker-base:\\d+\\.\\d+(\\.\\d+)?(-.*)?$"));
    }

    @Test
    void getImageId() {
        assertThat(
                JavisterBaseContainer.getImageId(JavisterBaseContainer.class, null),
                matchesPattern("^[0-9a-f]{12}$"));
    }
}
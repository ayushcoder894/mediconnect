package com.mediconnect.review;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewNameTest {

    @Test
    void publicReviewsShowFirstNameAndInitialOnly() {
        assertThat(ReviewService.firstNameAndInitial("Ananya Sharma")).isEqualTo("Ananya S.");
        assertThat(ReviewService.firstNameAndInitial("  Rahul Kumar Verma ")).isEqualTo("Rahul V.");
        assertThat(ReviewService.firstNameAndInitial("Madonna")).isEqualTo("Madonna");
    }
}

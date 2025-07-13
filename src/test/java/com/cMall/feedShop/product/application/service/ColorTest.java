package com.cMall.feedShop.product.application.service;

import com.cMall.feedShop.product.domain.enums.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Color Enum 테스트")
class ColorTest {

    @Test
    @DisplayName("Color enum이 정의되었을때_values 호출하면_모든 색상이 반환된다")
    void givenColorEnum_whenCallValues_thenReturnAllColors() {
        // when
        Color[] result = Color.values();

        // then
        assertThat(result).hasSize(12);
        assertThat(result).contains(
                Color.BLACK, Color.WHITE, Color.BROWN, Color.NAVY,
                Color.GRAY, Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.PURPLE, Color.PINK, Color.ORANGE
        );
    }

    @Test
    @DisplayName("기본 색상들이 정의되었을때_객체 생성하면_null이 아니다")
    void givenBasicColors_whenCreateObjects_thenNotNull() {
        // when & then
        assertThat(Color.BLACK).isNotNull();
        assertThat(Color.WHITE).isNotNull();
        assertThat(Color.RED).isNotNull();
        assertThat(Color.BLUE).isNotNull();
        assertThat(Color.GREEN).isNotNull();
    }

    @Test
    @DisplayName("Color가 주어졌을때_name 호출하면_정확한 이름이 반환된다")
    void givenColor_whenCallName_thenReturnCorrectName() {
        // when & then
        assertThat(Color.BLACK.name()).isEqualTo("BLACK");
        assertThat(Color.WHITE.name()).isEqualTo("WHITE");
        assertThat(Color.RED.name()).isEqualTo("RED");
        assertThat(Color.BLUE.name()).isEqualTo("BLUE");
    }

    @Test
    @DisplayName("모든 색상의 개수가 정확할때_values 호출하면_12개가 반환된다")
    void givenAllColors_whenCallValues_thenReturn12Colors() {
        // when
        Color[] colors = Color.values();

        // then
        assertThat(colors).hasSize(12);
    }

    @Test
    @DisplayName("동일한 Color가 주어졌을때_equals 비교하면_true가 반환된다")
    void givenSameColor_whenCompareEquals_thenReturnTrue() {
        // when & then
        assertThat(Color.BLACK).isEqualTo(Color.BLACK);
        assertThat(Color.RED).isEqualTo(Color.RED);
    }

    @Test
    @DisplayName("다른 Color가 주어졌을때_equals 비교하면_false가 반환된다")
    void givenDifferentColor_whenCompareEquals_thenReturnFalse() {
        // when & then
        assertThat(Color.BLACK).isNotEqualTo(Color.WHITE);
        assertThat(Color.RED).isNotEqualTo(Color.BLUE);
        assertThat(Color.GREEN).isNotEqualTo(Color.YELLOW);
    }

    @Test
    @DisplayName("Color ordinal이 주어졌을때_순서가 정확하다")
    void givenColorOrdinal_whenCheckOrder_thenCorrectOrder() {
        // when & then
        assertThat(Color.BLACK.ordinal()).isZero();
        assertThat(Color.WHITE.ordinal()).isEqualTo(1);
        assertThat(Color.BROWN.ordinal()).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 색상들이 포함되어있을때_contains 확인하면_true가 반환된다")
    void givenSpecificColors_whenCheckContains_thenReturnTrue() {
        // given
        Color[] colors = Color.values();

        // when & then
        assertThat(colors).contains(Color.NAVY);
        assertThat(colors).contains(Color.GRAY);
        assertThat(colors).contains(Color.PURPLE);
        assertThat(colors).contains(Color.PINK);
        assertThat(colors).contains(Color.ORANGE);
    }
}
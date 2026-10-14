package org.example.soalabs.worker.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coordinates {

    @Column(name = "coordinate_x", nullable = false)
    private Integer x;

    @Column(name = "coordinate_y", nullable = false)
    private Float y;

}

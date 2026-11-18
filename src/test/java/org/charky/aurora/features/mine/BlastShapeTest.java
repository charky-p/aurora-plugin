package org.charky.aurora.features.mine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Minecraft's axes: x runs east, y up, z south. So facing north is z -1, and east
// is on the right of a player facing north
class BlastShapeTest {
    private static Set<String> blocks(List<int[]> offsets) {
        return offsets.stream().map(o -> o[0] + "," + o[1] + "," + o[2]).collect(Collectors.toSet());
    }

    @Test
    void noBlastBreaksNothingElse() {
        assertTrue(BlastShape.offsets(0, 0, 1, 0, 0, -1).isEmpty());
    }

    // Standing south of a wall looking north, its south face is the one hit
    @Test
    void blastThreeOnAWallIsAPatchOfThatWall() {
        Set<String> area = blocks(BlastShape.offsets(3, 0, 0, 1, 0, -1));

        assertEquals(8, area.size());
        // Never in front of or behind the wall
        assertTrue(area.stream().allMatch(block -> block.endsWith(",0")), area.toString());
        assertTrue(area.contains("-1,1,0"));
        assertTrue(area.contains("1,-1,0"));
    }

    @Test
    void blastThreeOnAWallFacingEastRunsNorthToSouth() {
        Set<String> area = blocks(BlastShape.offsets(3, -1, 0, 0, 1, 0));

        assertEquals(8, area.size());
        assertTrue(area.stream().allMatch(block -> block.startsWith("0,")), area.toString());
        assertTrue(area.contains("0,1,-1"));
        assertTrue(area.contains("0,-1,1"));
    }

    // Mining down into the floor, or up into a ceiling
    @Test
    void blastThreeOnAFloorOrCeilingIsFlat() {
        for (int faceY : new int[]{1, -1}) {
            Set<String> area = blocks(BlastShape.offsets(3, 0, faceY, 0, 0, -1));

            assertEquals(8, area.size());
            assertTrue(area.stream().allMatch(block -> block.split(",")[1].equals("0")), area.toString());
        }
    }

    @Test
    void blastOneAddsTheBlockToTheRight() {
        // Looking north at a wall, right is east
        assertEquals(Set.of("1,0,0"), blocks(BlastShape.offsets(1, 0, 0, 1, 0, -1)));
        // Looking south at a wall, right is west
        assertEquals(Set.of("-1,0,0"), blocks(BlastShape.offsets(1, 0, 0, -1, 0, 1)));
        // Looking east at a wall, right is south
        assertEquals(Set.of("0,0,1"), blocks(BlastShape.offsets(1, -1, 0, 0, 1, 0)));
        // Mining the floor while facing north, right is still east
        assertEquals(Set.of("1,0,0"), blocks(BlastShape.offsets(1, 0, 1, 0, 0, -1)));
    }

    @Test
    void blastTwoOnAWallGrowsRightAndUp() {
        assertEquals(Set.of("1,0,0", "0,1,0", "1,1,0"), blocks(BlastShape.offsets(2, 0, 0, 1, 0, -1)));
    }

    @Test
    void blastTwoOnAFloorGrowsRightAndAway() {
        // Facing north: right is east, away is north
        assertEquals(Set.of("1,0,0", "0,0,-1", "1,0,-1"), blocks(BlastShape.offsets(2, 0, 1, 0, 0, -1)));
    }

    // Blast four and five dig into the wall too, the patch one then two blocks deeper
    @Test
    void blastFiveIsACubeGoingIntoTheWall() {
        Set<String> four = blocks(BlastShape.offsets(4, 0, 0, 1, 0, -1));
        Set<String> five = blocks(BlastShape.offsets(5, 0, 0, 1, 0, -1));

        assertEquals(17, four.size());
        assertEquals(26, five.size());
        // The south face was hit, so in is north: z 0, -1 and -2, never behind the wall
        assertTrue(five.stream().allMatch(block -> Set.of("0", "-1", "-2").contains(block.split(",")[2])), five.toString());
        assertTrue(five.contains("-1,-1,-2"));
        assertTrue(five.contains("1,1,-2"));
        assertTrue(four.stream().noneMatch(block -> block.endsWith(",-2")), four.toString());
    }

    @Test
    void blastFiveOnAFloorDigsDown() {
        Set<String> area = blocks(BlastShape.offsets(5, 0, 1, 0, 0, -1));

        assertEquals(26, area.size());
        assertTrue(area.stream().allMatch(block -> Set.of("0", "-1", "-2").contains(block.split(",")[1])), area.toString());
    }

    @Test
    void levelsPastTheTableKeepTheBiggestShape() {
        assertEquals(blocks(BlastShape.offsets(5, 0, 0, 1, 0, -1)), blocks(BlastShape.offsets(9, 0, 0, 1, 0, -1)));
    }
}

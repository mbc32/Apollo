package org.bbop.apollo.projection

import org.bbop.apollo.Organism

/**
 * Created by nathandunn on 9/24/15.
 */
class MultiSequenceProjection extends AbstractProjection {

    // if a projection includes multiple sequences, this will include greater than one
    TreeMap<ProjectionSequence, DiscontinuousProjection> sequenceDiscontinuousProjectionMap = new TreeMap<>()
    ProjectionDescription projectionDescription  // description of how this is generated

    List<String> chunks = new ArrayList<>()
    ProjectionChunkList projectionChunkList = new ProjectionChunkList()

    ProjectionSequence getReverseProjectionSequence(Integer input) {
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            if (input >= projectionSequence.offset && input <= projectionSequence.offset + sequenceDiscontinuousProjectionMap.get(projectionSequence).bufferedLength) {
                return projectionSequence
            }
        }
        return null
    }

    List<ProjectionSequence> getReverseProjectionSequences(Integer minInput, Integer maxInput) {
        List<ProjectionSequence> orderedSequences = []
        Integer startOrder = getReverseProjectionSequence(minInput)?.order
        Integer endOrder = getReverseProjectionSequence(maxInput)?.order
        if (endOrder == null) {
            endOrder = getLastSequence().order
        }

        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            if (projectionSequence.order >= startOrder && projectionSequence.order <= endOrder) {
                orderedSequences << projectionSequence
            }
        }

        return orderedSequences
    }

    /**
     * Find which sequence I am on by iterating over coordinates
     * This looks in unprojected space
     * @param input
     * @return
     */
    ProjectionSequence getProjectionSequence(Integer input) {

        Integer offset = 0
        // should deliver these in order
        for (projectionSequence in sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            DiscontinuousProjection projection = sequenceDiscontinuousProjectionMap.get(projectionSequence)
//            if (input >= offset && input <= projection.originalLength + offset) {
            if (input >= offset && input <= projectionSequence.unprojectedLength + offset) {
                return projectionSequence
            }
            offset += projectionSequence.unprojectedLength
        }
        return null
    }

    Integer projectValue(Integer input) {
        ProjectionSequence projectionSequence = getProjectionSequence(input)
        if (!projectionSequence) {
            return UNMAPPED_VALUE
        }
        DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence)
        Integer returnValue = discontinuousProjection.projectValue(input - projectionSequence.originalOffset)
        if (returnValue == UNMAPPED_VALUE) {
            return UNMAPPED_VALUE
        } else {
            return returnValue + projectionSequence.offset
        }
    }

//    ProjectionSequence findPreviousProjectionSequence(ProjectionSequence projectionSequence) {
//        Integer order = projectionSequence.order
//        if (order <= 0) return null
//
//        for (ProjectionSequence projectionSequence1 in sequenceDiscontinuousProjectionMap.keySet()) {
//            if (projectionSequence1.order == (order - 1)) {
//                return projectionSequence1
//            }
//        }
//        return null
//    }

    Integer projectReverseValue(Integer input) {
        ProjectionSequence projectionSequence = getReverseProjectionSequence(input)
        if (!projectionSequence) return -1
        return sequenceDiscontinuousProjectionMap.get(projectionSequence).projectReverseValue(input - projectionSequence.offset) + projectionSequence.originalOffset
    }

    Integer getLength() {
        Map.Entry<ProjectionSequence, DiscontinuousProjection> entry = sequenceDiscontinuousProjectionMap.lastEntry()
        return entry.key.offset + entry.value.length
    }

    String projectSequence(String inputSequence, Integer minCoordinate, Integer maxCoordinate, Integer offset) {
        Integer index = minCoordinate
        List<String> sequenceList = []
        Integer scaffoldBuffer = 0

        // we start at the very bottom and go up
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }) {
            DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence)
            Integer unprojectedLength = projectionSequence.unprojectedLength
            Integer max, min
            max = unprojectedLength > maxCoordinate ? maxCoordinate : maxCoordinate - unprojectedLength
            min = projectionSequence.order==0 ? minCoordinate : 0
            scaffoldBuffer = projectionSequence.order
            // we assumef a single space between
            min+= scaffoldBuffer
            max += scaffoldBuffer
//            if (index < projectionLength) {
//                sequenceList << discontinuousProjection.projectSequence(inputSequence, index - index, max, offset+index)
//            }
            sequenceList << discontinuousProjection.projectSequence(inputSequence, min , max, offset)
            index += unprojectedLength
        }

        // not really used .  .. .  but otherwise would carve up into different bits
        return sequenceList.join("")
    }

    List<Coordinate> listCoordinates() {
        List<Coordinate> coordinateList = new ArrayList<>()
        for (def projection in sequenceDiscontinuousProjectionMap.values()) {
            coordinateList.addAll(projection.minMap.values() as List<Coordinate>)
        }
        return coordinateList
    }

    def addInterval(int min, int max, ProjectionSequence sequence) {
        Location location = new Location(min: min, max: max, sequence: sequence)
        addLocation(location)
    }


    Integer size() {
        Integer count = 0
        for (def projection in sequenceDiscontinuousProjectionMap.values()) {
            count += projection.size()
        }

        return count
    }

    def addLocations(List<Location> locationList) {
        for (Location location in locationList) {
            addLocation(location)
        }
    }

    @Override
    Integer clear() {
        int size = sequenceDiscontinuousProjectionMap.size()
        sequenceDiscontinuousProjectionMap.clear()
        return size
    }

// here we are adding a location to project
    def addLocation(Location location) {
        // if a single projection . . the default .. then assert that it is the same sequence / projection
//        ProjectionSequence projectionSequence = getProjectionSequence(location)
        DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(location.sequence)
        if (discontinuousProjection) {
            discontinuousProjection.addInterval(location.min, location.max, projectionDescription.padding)
        } else {
//        if (!projectionSequence) {
            ProjectionSequence internalProjectionSequence = location.sequence

            Integer order = findSequenceOrder(internalProjectionSequence)
            internalProjectionSequence.order = order

            DiscontinuousProjection thisDiscontinuousProjection = new DiscontinuousProjection()
            thisDiscontinuousProjection.addInterval(location.min, location.max, projectionDescription.padding)
            sequenceDiscontinuousProjectionMap.put(internalProjectionSequence, thisDiscontinuousProjection)
        }
    }

    Integer findSequenceOrder(ProjectionSequence projectionSequence) {
        List<ProjectionSequence> projectionSequenceList = projectionDescription.sequenceList
        int index = 0
        for (ProjectionSequence projectionSequence1 in projectionSequenceList) {
            if (projectionSequence1.name == projectionSequence.name) {
                return index
            }
            ++index
        }
        return -1
    }

    ProjectionSequence getProjectionSequence(Location location) {
//        if (sequenceDiscontinuousProjectionMap.containsKey(location.sequence)) {
        // should be a pretty limited set
        if (!location) return null
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet()) {
            if (projectionSequence.equals(location.sequence)) {
                return projectionSequence
            }
        }
//        }
        return null
    }

    /**
     * This is done at the end to make offsets render properly
     */
    def calculateOffsets() {
        Integer currentOrder = 0
        Integer lastLength = 0
//        Integer originalLength = 0
        Integer unprojectedLength = 0
        sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }.each {
            DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(it)
            if (currentOrder > 0) {
                it.offset = lastLength + 1
                it.originalOffset = unprojectedLength
            }

            lastLength += discontinuousProjection.bufferedLength
//            originalLength += discontinuousProjection.originalLength
            unprojectedLength += it.unprojectedLength
            ++currentOrder
        }
    }

    ProjectionSequence getProjectionSequence(String sequenceName, Organism organism) {
        return getProjectionSequence(sequenceName, null, organism)
    }

    ProjectionSequence getProjectionSequence(String sequenceName, Long sequenceId, Organism organism) {
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet()) {
            if (projectionSequence.name == sequenceName) {
                if (projectionSequence.organism && organism) {
                    if (projectionSequence.organism != organism.commonName) {
                        return projectionSequence
                    }
                }
                if (projectionSequence.id && sequenceId) {
                    if (projectionSequence.id != sequenceId) {
                        return null
                    }
                }
                return projectionSequence
            }
        }
        return null
    }

    Boolean containsSequence(String sequenceName, Organism organism) {
        return containsSequence(sequenceName, null, organism)
    }

    Boolean containsSequence(String sequenceName, Long sequenceId, Organism organism) {
        return getProjectionSequence(sequenceName, sequenceId, organism) != null
    }


    public String toString() {
        return "MultiSequenceProjection{" +
                "sequenceDiscontinuousProjectionMap=" + sequenceDiscontinuousProjectionMap +
                ", projectionDescription=" + projectionDescription +
                '}';
    }

    /**
     * Looks up the length of the stored projection sequence
     * @param sequenceName
     * @return
     */
    Integer findProjectSequenceLength(String sequenceName) {
        for (ProjectionSequence projectionSequence1 in sequenceDiscontinuousProjectionMap.keySet()) {
            if (projectionSequence1.name == sequenceName) {
                DiscontinuousProjection discontinuousProjection = sequenceDiscontinuousProjectionMap.get(projectionSequence1)
//                println "projected sequence ${discontinuousProjection.projectSequence(sequenceName)}"
                Integer calculatedLength = discontinuousProjection.projectValue(discontinuousProjection.originalLength)
                println "calculated length ${calculatedLength} from ${discontinuousProjection.originalLength}"
                return calculatedLength
            }
        }
        null
    }

    TreeMap<Integer, Coordinate> getMinMap() {
        Map<Integer, Coordinate> minMap = new TreeMap<>()
        List<ProjectionSequence> projectionSequenceList = sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order } as List

        for (ProjectionSequence projectionSequence : projectionSequenceList) {
            Map<Integer, Coordinate> returnMap = new TreeMap<>()

            sequenceDiscontinuousProjectionMap.get(projectionSequence).minMap.each {
                Coordinate coordinate = new Coordinate(min: it.value.min, max: it.value.max)
                coordinate.addOffset(projectionSequence.originalOffset)
                returnMap.put(it.key + projectionSequence.originalOffset, coordinate)
            }
//
            minMap.putAll(returnMap)

//            minMap.putAll(sequenceDiscontinuousProjectionMap.get(projectionSequence).minMap)
        }

        return minMap
    }

    TreeMap<Integer, Coordinate> getMaxMap() {
        Map<Integer, Coordinate> maxMap = new TreeMap<>()
        List<ProjectionSequence> projectionSequenceList = sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order } as List

        for (ProjectionSequence projectionSequence : projectionSequenceList) {
            Map<Integer, Coordinate> returnMap = new TreeMap<>()
            // add a set with an offset
            sequenceDiscontinuousProjectionMap.get(projectionSequence).maxMap.each {
                Coordinate coordinate = new Coordinate(min: it.value.min, max: it.value.max)
                coordinate.addOffset(projectionSequence.originalOffset)
                returnMap.put(it.key + projectionSequence.originalOffset, coordinate)
            }
//
            maxMap.putAll(returnMap)

//            maxMap.putAll(sequenceDiscontinuousProjectionMap.get(projectionSequence).maxMap)

        }

        return maxMap
    }

    Coordinate getMaxCoordinate(ProjectionSequence projectionSequence = null) {
        if (projectionSequence == null) {
            return getMaxMap().lastEntry().value
        }
        return sequenceDiscontinuousProjectionMap.get(projectionSequence).maxMap.lastEntry().value
    }

    Coordinate getMinCoordinate() {
        return getMinMap().firstEntry().value
    }

    Integer getOffsetForSequence(String sequenceName) {
        if (projectionChunkList) {
            ProjectionChunk projectionChunk = projectionChunkList.findProjectChunkForName(sequenceName)
            if (projectionChunk) {
                return projectionChunk.sequenceOffset
            }
        }
        for (ProjectionSequence projectionSequence in sequenceDiscontinuousProjectionMap.keySet()) {
            if (sequenceName == projectionSequence.name) {
                return projectionSequence.originalOffset
            }
        }
        println "no offset for sequence ${sequenceName}"

        return 0
    }

    ProjectionSequence getLastSequence() {
        return projectedSequences.last()
    }

//    DiscontinuousProjection getProjectionForSequence(ProjectionSequence projectionSequence) {
//        return sequenceDiscontinuousProjectionMap.get(projectionSequence)
//    }

    List<ProjectionSequence> getProjectedSequences() {
        return sequenceDiscontinuousProjectionMap.keySet().sort() { a, b -> a.order <=> b.order }
    }

}